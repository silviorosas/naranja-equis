# 🏗️ Arquitectura de Billetera Virtual - Naranja X (Spring Cloud Edition)

## 📊 Visión General

Evolución del sistema de billetera virtual hacia un ecosistema de **Spring Cloud** completo. La arquitectura se ha rediseñado para soportar service discovery, configuración centralizada, gateway inteligente y resiliencia avanzada.

---

## 🎯 Stack Tecnológico

### Backend Core
- **Java 21** - Última versión LTS
- **Spring Boot 3.2.x** - Framework principal
- **Spring Cloud 2023.0.x** - Ecosistema de microservicios
- **Spring Security + JWT** - Autenticación y autorización centralizada

### Infraestructura Spring Cloud
- **Netflix Eureka** - Service Discovery (Registro y localización de servicios)
- **Spring Cloud Gateway** - API Gateway (Punto único de entrada, ruteo y filtros)
- **Spring Cloud Config** - Configuración centralizada (Gestionada por carpetas locales o Git)
- **Resilience4j** - Implementación de Circuit Breaker, Rate Limiter y Retry

### Persistencia y Mensajería
- **MySQL 8.x** - DB relacional transaccional
- **MongoDB 6.x** - DB NoSQL para eventos y notificaciones
- **Apache Kafka** - Bus de eventos asíncronos

---

## 🏛️ Ecosistema de Microservicios

### 📡 Servicios de Infraestructura

#### 1. Discovery Server (Netflix Eureka)
- **Puerto**: 8761
- **Función**: Permite que los microservicios se encuentren entre sí sin conocer sus IPs fijas.
- **Resiliencia**: Si una instancia cae, Eureka la remueve del registro automáticamente.

#### 2. Config Server
- **Puerto**: 8888
- **Función**: Repositorio central de archivos `.yml` y `.properties`. Los servicios cargan su configuración al iniciar.
- **Seguridad**: Permite encriptar valores sensibles (passwords, secrets).

#### 3. API Gateway
- **Puerto**: 8080 (Nuevo punto de entrada principal)
- **Función**: 
  - Ruteo dinámico hacia los microservicios usando Eureka.
  - Terminación de JWT (validación centralizada opcional).
  - Implementación de **Circuit Breakers** con Resilience4j.
  - Rate Limiting centralizado.

---

### 🛡️ Servicios de Negocio

#### 1️⃣ Auth Service (Puerto: 8081)
- **Responsabilidad**: Seguridad, Usuarios y Roles.
- **Novedad**: Registrado en Eureka. Usa Config Server para sus credenciales de DB y JWT.

#### 2️⃣ Wallet Service (Puerto: 8082)
- **Responsabilidad**: Saldos, CVU y Alias.
- **Resiliencia**: Implementa Circuit Breaker al consultar otros servicios o Kafka.

#### 3️⃣ Transaction Service (Puerto: 8083)
- **Responsabilidad**: P2P, Depósitos y Retiros.
- **Flujo**: Genera eventos `transaction.events` para que Wallet Service actualice saldos.
- **Resiliencia**: Circuit Breaker crítico para evitar caídas en cascada durante picos de transacciones.

#### 4️⃣ Notification Service (Puerto: 8084)
- **Responsabilidad**: Email, Push y SMS.
- **Tecnología**: MongoDB para historial persistente de notificaciones.

---

## 🔄 Flujo de Datos con Gateway

```
Frontend (Angular) 
      │
      ▼
API Gateway (8080) ───► Eureka (Discovery)
      │
      ├─► /api/auth/**        ──► Auth Service
      ├─► /api/wallets/**     ──► Wallet Service
      └─► /api/transactions/** ──► Transaction Service
```

---

## 🛡️ Resiliencia con Resilience4j

Se aplica el patrón **Circuit Breaker** en las comunicaciones entre servicios:
- **Closed**: Flujo normal.
- **Open**: Si el servicio destino falla repetidamente, el circuito se abre y se retorna un "fallback" inmediatamente.
- **Half-Open**: Prueba periódica para ver si el servicio destino se recuperó.

---

## 🐳 Estructura de Proyecto y Docker

### Organización de Carpetas
```
naranjaX/
├── infrastructure/
│   ├── discovery-server/     # Eureka
│   ├── config-server/        # Spring Cloud Config
│   └── api-gateway/          # Cloud Gateway
├── services/
│   ├── auth-service/
│   ├── wallet-service/
│   ├── transaction-service/
│   └── notification-service/
├── common-library/           # Objetos compartidos
└── docker-compose.yml        # Orquestación de todo el ecosistema
```

---

## 🚀 Roadmap Evolucionado

### ✅ Fase 1 y 2 - Core & Mensajería
- Implementación de servicios base y Kafka.

### ✅ Fase 3 - Infraestructura Spring Cloud (Actual)
- Implementación de Eureka (Discovery), Gateway y Config Server.
- Configuración de Resilience4j para todos los servicios críticos.
- Integración de Transaction Service con ruteo dinámico y filtros de gateway.

### 🔲 Fase 4 - Observabilidad
- Integración con Prometheus, Grafana y Zipkin para trazado distribuido.

---

**Versión**: 2.1 (Spring Cloud Integrated)  
**Fecha**: 2026-02-02  
**Estado**: Infraestructura Completada - Implementando Lógica de Transacciones
