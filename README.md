# NaranjaX Virtual Wallet (Spring Cloud Edition) 🚀

Sistema de Billetera Virtual construido con una arquitectura de microservicios robusta y resiliente utilizando **Spring Cloud**.

## 🏗️ Arquitectura Spring Cloud

Ahora el sistema utiliza un ecosistema completo para escalabilidad y resiliencia:

### 📡 Infraestructura
- **API Gateway** (Puerto 8080): Punto único de entrada ruteo dinámico con **Spring Cloud Gateway**.
- **Discovery Server** (Puerto 8761): Service discovery con **Netflix Eureka**.
- **Config Server** (Puerto 8888): Configuración centralizada gestionada en una carpeta local (`config-repo/`).
- **Resilience4j**: Circuit Breaker implementado en el Gateway para proteger los servicios.

### 🛡️ Microservicios de Negocio
- **Auth Service** (Puerto 8081): Autenticación y autorización con JWT.
- **Wallet Service** (Puerto 8082): Gestión de billeteras virtuales y saldos.
- **Transaction Service** (Puerto 8083): Procesamiento de transacciones (P2P, depósitos).
- **Notification Service** (Puerto 8084): Notificaciones asíncronas multi-canal.

## 🛠️ Stack Tecnológico

- **Backend**: Java 21, Spring Boot 3.2.x, Spring Cloud 2023.0.x
- **Persistencia**: MySQL 8.2 (Relacional), MongoDB 6.0 (Logs)
- **Mensajería**: Apache Kafka 7.5.0 (Confluent)
- **DevOps**: Docker & Docker Compose (Multi-stage builds)

## 📁 Estructura del Proyecto

```
naranjaX/
├── infrastructure/           # Eureka, Config Server, Gateway
├── services/                 # Auth, Wallet, Transaction, Notification
├── common-library/           # Librería compartida (DTOs, Events)
├── config-repo/              # Archivos de configuración (.yml)
├── architecture.md           # Documentación detallada
└── docker-compose.yml        # Orquestación completa
```

## 🚀 Inicio Rápido (Docker First)

**IMPORTANTE**: No necesitas tener Java o Maven instalado localmente. Las imágenes de Docker realizan la compilación internamente para asegurar consistencia.

### 1. Clonar y Preparar

```powershell
git clone <repository-url>
cd naranjaX
```

### 2. Levantar Todo el Ecosistema

Usa Docker Compose para compilar y levantar los 10+ servicios simultáneamente:

```powershell
docker-compose up -d --build
```

### 3. Verificar en el Discovery Server (Eureka)

Accede al dashboard para ver todos los servicios registrados:
👉 [http://localhost:8761](http://localhost:8761)

### 4. Acceso a través del Gateway (Punto de Entrada)

A partir de ahora, todas las llamadas al API deben hacerse a través del puerto **8080**:

- **Health Check Global**: `http://localhost:8080/actuator/health`
- **Registro de Usuario**: `POST http://localhost:8080/api/auth/register`
- **Login**: `POST http://localhost:8080/api/auth/login`
- **Depósitos**: `POST http://localhost:8080/api/transactions/deposit?amount=5000`
- **Transferencias**: `POST http://localhost:8080/api/transactions/transfer`

---

## 🚀 Guía de Pruebas (Postman)

### 1. Registro de Usuario (Auth Service)
**CURL:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{"email": "juan@test.com", "password": "password123", "fullName": "Juan Perez"}'
```

### 2. Depósito de Saldo (Transaction Service)
*Requiere el ID del usuario creado.*
**CURL:**
```bash
curl -X POST "http://localhost:8080/api/transactions/deposit?amount=10000.50" \
     -H "X-User-Id: 1"
```

### 3. Transferencia entre Usuarios (P2P)
**CURL:**
```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
     -H "Content-Type: application/json" \
     -H "X-User-Id: 1" \
     -d '{"receiverId": 2, "amount": 1500.00, "description": "Pago de deuda 💸"}'
```

## 🔧 Gestión con el Script `manage.ps1`

Hemos incluido un script de PowerShell para facilitar las tareas comunes:

```powershell
.\scripts\manage.ps1 help     # Ver todos los comandos
.\scripts\manage.ps1 up       # Levantar infraestructura
.\scripts\manage.ps1 status   # Ver estado de servicios
.\scripts\manage.ps1 logs     # Ver logs en tiempo real
.\scripts\manage.ps1 rebuild  # Reconstruir imágenes desde cero
```

## 🔐 Seguridad

- **JWT Stateless**: Validado centralmente (opcional) o en cada servicio.
- **Circuit Breaker**: Si el Auth Service cae, el Gateway responde con un fallback controlado.
- **Configuración Segura**: Los secrets se gestionan centralmente en el Config Server.

---

**Versión**: 2.1.0 (Spring Cloud Integrated)  
**Estado**: Infraestructura OK - Funcionalidades de Negocio activas 🚀
