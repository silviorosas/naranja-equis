# 🏗️ Arquitectura de Billetera Virtual - Naranja X (v3.2)

## 📊 Visión General

Evolución del sistema de billetera virtual hacia un ecosistema de **Spring Cloud** completo con una interfaz de usuario moderna en **Angular 20**. La arquitectura soporta service discovery, configuración centralizada, gateway inteligente, resiliencia avanzada y una experiencia de usuario responsive (Mobile-First).

---

## 🎯 Stack Tecnológico

### Frontend
- **Angular 20** - Framework moderno para SPAs.
- **ngx-toastr** - Sistema de notificaciones profesionales.
- **CSS Grid/Flexbox** - Layout responsivo Mobile-First.
- **FontAwesome** - Iconografía enriquecida.

### Backend Core
- **Java 21** - Última versión LTS.
- **Spring Boot 3.2.2** - Framework principal.
- **Spring Cloud 2023.0.0** - Ecosistema de microservicios.
- **Spring Security + JWT** - Autenticación con Claims personalizados (`userId`).
- **SpringDoc OpenAPI** - Documentación interactiva (Swagger UI).

### Infraestructura Spring Cloud
- **Netflix Eureka** - Service Discovery.
- **Spring Cloud Gateway** - API Gateway y Filtros.
- **Spring Cloud Config** - Configuración centralizada.
- **Resilience4j** - Circuit Breaker e Isolation.

### Calidad de Código & DevOps
- **SonarQube Community** - Análisis estático y Quality Gates.
- **JaCoCo (Java)** - Reportes de cobertura agregados.
- **SonarJS / LCOV (Angular)** - Análisis especializado para frontend.
- **GitHub Actions** - Automatización de calidad (CI).

### Persistencia y Mensajería
- **MySQL 8.0/8.2** - DB relacional para transacciones y saldos.
- **MongoDB 6.x** - Auditoría y logs de notificaciones.
- **Redis 7.2** - Cache de identidades y saldos (Patrón Cache-Aside).
- **Apache Kafka** - Bus de eventos asíncronos para reconciliación de saldos y notificaciones.

---

## 🏛️ Ecosistema de Microservicios

### 🛡️ Servicios de Negocio

#### 1️⃣ Auth Service (8081)
- **Responsabilidad**: Seguridad, Usuarios y Sesiones.
- **Novedad**: Ahora emite JWTs que incluyen el `userId` en los claims, eliminando la necesidad de pasar el ID manualmente en los headers del frontend.

#### 2️⃣ Wallet Service (8082)
- **Responsabilidad**: Gestión de saldos, CVU y Alias.
- **Integración**: Sincroniza el saldo mediante eventos de Kafka emitidos por el Transaction Service.

#### 3️⃣ Transaction Service (8083)
- **Responsabilidad**: P2P, Depósitos e Historial.
- **Resiliencia (NUEVO)**: Implementa **Cache-Aside con Redis** para identidades de usuario. Si el Auth-Service no responde, el sistema recupera nombres y emails de la caché (TTL 24h), evitando placeholders como "Desconocido".
- **Seguridad**: Implementa validación de **Ownership**. Un usuario solo puede visualizar su propio historial de transacciones.
- **Historial**: Ordenamiento cronológico descendente.

#### 4️⃣ Notification Service (8084)
- **Responsabilidad**: Envío de notificaciones por email (transaccionales).
- **Hardening (v3.2)**: 
    - **¡Recibiste dinero!**: Nuevo flujo para receptores que prioriza el **Alias** sobre el CBU para una experiencia más humana.
    - **Anti-Blocking**: Delay estratégico de **10 segundos** entre envíos para cumplir con los límites de Mailtrap sin perder notificaciones.
- **Consumidor**: Escucha de `transaction.events` para disparar comprobantes y alertas de recepción de transferencias.
- **Auditoría**: Persistencia de cada notificación en MongoDB para trazabilidad.

---

## 🔄 Flujo de Datos Completo

```
  Usuario (Celular/Web)
          │
          ▼
    [ FrontEquis ] (4200)
          │ (JWT con userId)
          ▼
  [ API Gateway ] (8080) ───► [ Eureka ]
          │
          ├─► /api/auth/**        ──► Auth Service
          ├─► /api/wallets/**     ──► Wallet Service
          └─► /api/transactions/** ──► Transaction Service
                                          │
                                          ▼ (Kafka)
                                   [ Wallet Service ] (Actualiza Saldo)
```

---

## 🛡️ Seguridad y Resiliencia

- **UserPrincipal**: Clase compartida para manejar la identidad del usuario autenticado en todo el ecosistema.
- **Ownership Validation**: Los endpoints de transacciones verifican que el `principal.id` coincida con los recursos solicitados.
- **Circuit Breakers**: Protegen el Gateway de fallos en cascada si un microservicio de negocio se vuelve inestable.

---

## 🐳 Organización del Proyecto

```
naranjaX/
├── infrastructure/           # Eureka, Config Server, Gateway
├── services/                 # Auth, Wallet, Transaction, Notification
├── frontEquis/               # Aplicación Angular 20
├── common-library/           # Objetos compartidos y Seguridad
└── docker-compose.yml        # Orquestación completa
```

---

## 🚀 Roadmap Evolucionado

### ✅ Completado
- Infraestructura Spring Cloud (Eureka, Gateway, Config).
- Lógica de Transacciones P2P y Actualización de Saldo.
- Frontend Responsive con experiencia de usuario premium.
- Seguridad basada en JWT con ID incrustado.

### ✅ Fase 5 - Calidad de Código & Seguridad Robusta
- Integración de SonarQube local y remota.
- Refactor de Clean Code y accesibilidad en el Dashboard.
- Hardening de seguridad con `SecureRandom` y CSRF protection.
- **Calidad Gate Compliance**: Exclusión de DTOs/Entities de cobertura y generación de Unit Tests (>80%).

### ✅ Fase 6 - Observabilidad Avanzada
- ✅ **Step-by-Step Visual Flow**: Implementación de traza numerada y jerárquica para demostraciones.
- ✅ **Testing Estratégico**: JUnit 5 + Mockito cubriendo flujos críticos de logs y lógica.
- ✅ **Quality Gate Hardened**: Exclusión agresiva de Consumers/Producers y cobertura del 100% en servicios core.
- 🔲 Integración con Prometheus/Grafana.
- ✅ Auditoría transaccional en MongoDB.
- ✅ **Optimización de Notificaciones**: Decoupling total mediante eventos enriquecidos (Customer Data in Kafka).
- ✅ **Resiliencia Fintech**: Implementación de **Resilience4j Retry** (2s wait) para mitigar Rate Limits de Mailtrap (v3.0).
- ✅ **Auditoría Extendida**: Registro completo de contexto (Destinatario, Monto, ID Transacción) en MongoDB 6.0 para cumplimiento regulatorio.
- ✅ **Fase 7 - Identity & Notification Hardening (v3.2)**:
    - ✅ **Cache-Aside for Identities**: Persistencia en Redis para nombres y emails (Zero Auth-Downtime Impact).
    - ✅ **JSON Serialization**: RedisTemplate configurado con Jackson para objetos UserDto.
    - ✅ **UX Polishing**: Emails para receptores con bloques visuales de Origen/Destino y Aliases obligatorios.
    - ✅ **Stability**: Aumento del delay anti-bloqueo a 10s.

---

## 📈 Roadmap & Versiones
- **v1.x**: Fundamentos (Auth, Wallet).
- **v2.0**: Transacciones P2P y Auditoría.
- **v2.5**: Config Server y Service Discovery.
- **v2.9**: Event-Driven Optimization.
- **v3.0**: Resilience & UX Hardened.
- **v3.2**: Identity & Notification Hardening (Redis Cache + Hardened Emails).

**Versión**: 3.2 (Identity & Notification Hardening)  
**Fecha**: 2026-02-17  
**Estado**: Ecosistema Robusto y Resiliente 🚀

**Nota**: Este proyecto es una implementación de referencia y no debe usarse en producción sin auditorías de seguridad adicionales y pruebas exhaustivas.
