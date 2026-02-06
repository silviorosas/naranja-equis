# 🏗️ Arquitectura de Billetera Virtual - Naranja X (v2.3)

## 📊 Visión General

Evolución del sistema de billetera virtual hacia un ecosistema de **Spring Cloud** completo con una interfaz de usuario moderna en **Angular 18**. La arquitectura soporta service discovery, configuración centralizada, gateway inteligente, resiliencia avanzada y una experiencia de usuario responsive (Mobile-First).

---

## 🎯 Stack Tecnológico

### Frontend
- **Angular 18** - Framework moderno para SPAs.
- **ngx-toastr** - Sistema de notificaciones profesionales.
- **CSS Grid/Flexbox** - Layout responsivo Mobile-First.
- **FontAwesome** - Iconografía enriquecida.

### Backend Core
- **Java 21** - Última versión LTS.
- **Spring Boot 3.2.x** - Framework principal.
- **Spring Cloud 2023.0.x** - Ecosistema de microservicios.
- **Spring Security + JWT** - Autenticación con Claims personalizados (`userId`).

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
- **MySQL 8.x** - DB relacional para transacciones y saldos.
- **MongoDB 6.x** - Auditoría y logs de notificaciones.
- **Apache Kafka** - Bus de eventos asíncronos para reconciliación de saldos.

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
- **Seguridad**: Implementa validación de **Ownership**. Un usuario solo puede visualizar su propio historial de transacciones (`/transactions/user/{id}`).
- **Historial**: Ordenamiento cronológico descendente (Primero lo más reciente).

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
├── frontEquis/               # Aplicación Angular 18
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

### 🔲 Fase 6 - Observabilidad Avanzada
- Integración con Prometheus/Grafana.
- Auditoría transaccional en MongoDB.

---

**Versión**: 2.3 (Quality & Security Hardened)  
**Fecha**: 2026-02-06  
**Estado**: Estándares de Industria Alcanzados 🚀

**Nota**: Este proyecto es una implementación de referencia y no debe usarse en producción sin auditorías de seguridad adicionales y pruebas exhaustivas.
