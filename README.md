# NaranjaX Virtual Wallet (Spring Cloud & Angular Edition) 🚀

Sistema de Billetera Virtual (Fintech) construido con una arquitectura de microservicios robusta en el backend y una interfaz de usuario premium en el frontend.

## 🏗️ Ecosistema Tecnológico

### 🖥️ Frontend (FrontEquis)
- **Framework**: Angular 18+ con Standalone Components.
- **UI/UX**: Responsive Mobile-First, Toasts dinámicos (`ngx-toastr`), y estética NaranjaX.
- **Localización**: Formateado de moneda y fechas para Argentina (GMT-3).

### 📡 Backend (Spring Cloud)
- **API Gateway (8080)**: Punto único de entrada con ruteo dinámico.
- **Discovery Server (8761)**: Service discovery con Netflix Eureka.
- **Config Server (8888)**: Gestión centralizada de configuraciones.
- **Microservicios**: Auth (8081), Wallet (8082), Transaction (8083), Notification (8084).

### 🏛️ Infraestructura & Mensajería
- **Kafka**: Comunicación asíncrona para la consistencia de saldos.
- **Bases de Datos**: MySQL (Transaccional) y MongoDB (Auditoría).

## 📁 Estructura del Proyecto

```
naranjaX/
├── frontEquis/               # Frontend Angular
├── services/                 # Auth, Wallet, Transaction, Notification
├── infrastructure/           # Eureka, Config Server, Gateway
├── common-library/           # Librería compartida (Security & Events)
├── config-repo/              # Configuración YAML de servicios
└── architecture.md           # Documentación técnica completa
```

## 🚀 Inicio Rápido

### 1. Levantar Backend (Docker Compose)
Usa Docker para compilar y levantar todo el ecosistema de microservicios:
```bash
docker-compose up -d --build
```

### 2. Levantar Frontend
```bash
cd frontEquis
npm install
ng serve --open
```
Accede a: [http://localhost:4200](http://localhost:4200)

## 🚀 Guía de Pruebas (API)

A través del Gateway (**Puerto 8080**):

### 1. Historial de Transacciones
**CURL:**
```bash
curl -X GET http://localhost:8080/api/transactions/user/1 \
     -H "Authorization: Bearer <TOKEN>"
```

### 2. Depósito de Dinero
**CURL:**
```bash
curl -X POST "http://localhost:8080/api/transactions/deposit?amount=5000" \
     -H "Authorization: Bearer <TOKEN>"
```

### 3. Transferencia P2P
**CURL:**
```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <TOKEN>" \
     -d '{"receiverId": 2, "amount": 1500.00}'
```

*(Nota: El `X-User-Id` ya no es obligatorio si se usa un Token JWT generado por la versión 2.2+, ya que el ID está dentro del claim del token).*

## 🔐 Seguridad
El sistema utiliza un modelo de seguridad basado en **UserPrincipal** y validación de propiedad de recursos. Nadie puede ver transacciones ajenas, garantizando la privacidad de los datos financieros.

---
**Versión**: 2.2.0 (Full UI Support)  
**Estado**: Activo 🚀
