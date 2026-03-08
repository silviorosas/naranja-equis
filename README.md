# NaranjaX Virtual Wallet (Spring Cloud & Angular Edition) 🚀

Sistema de Billetera Virtual (Fintech) construido con una arquitectura de microservicios robusta en el backend y una interfaz de usuario premium en el frontend.

## 🏗️ Ecosistema Tecnológico

### 🖥️ Frontend (FrontEquis)
- **Framework**: Angular 20 con Standalone Components.
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
### 3. Análisis de Calidad (SonarQube)
Levanta la infraestructura de Sonar e inicia el escaneo:
```bash
# Levantar SonarQube & Postgres
docker-compose -f docker-compose.sonar.yml up -d

# Ejecutar análisis Backend (Java)
mvn verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.login=admin -Dsonar.password=admin

# Ejecutar análisis Frontend (Angular)
cd frontEquis
npm test -- --watch=false --code-coverage
npx sonar-scanner
uso local tunel
```
Accede a: [http://localhost:9000](http://localhost:9000)

## 🚀 Guía de Pruebas (API)

### 📄 Documentación Swagger (OpenAPI 3)
Cada microservicio expone su propia documentación interactiva. Nota: Debes autenticarte en el botón "Authorize" con el Bearer Token (JWT) obtenido en el login.

| Servicio | URL Swagger UI | URL OpenAPI JSON |
|----------|----------------|------------------|
| **Auth** | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |
| **Wallet** | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) |
| **Transaction** | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) |
| **Notification** | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) | [http://localhost:8084/v3/api-docs](http://localhost:8084/v3/api-docs) |

---

## 🚀 Guía de Pruebas (Postman)

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
## 🚀 CI/CD & Calidad
El proyecto incluye un pipeline de **GitHub Actions** (`.github/workflows/sonar.yml`) que automatiza:
- Compilación con JDK 21.
- Ejecución de pruebas unitarias y generación de reportes JaCoCo.
- Análisis estático de código en SonarQube para detectar Bug, Vulnerabilities y Code Smells.

---
---
## 🔍 Observabilidad Visual (High-Impact Logs)
El sistema implementa un sistema de logs de alto impacto visual diseñado para monitoreo en tiempo real (ideal para demos y auditoría visual):

- **⚡ Redis (Identity Cache)**: Identificación inmediata de `CACHE HIT` y `CACHE MISS` para datos de usuarios. Garantiza que las notificaciones salgan con nombres reales incluso si el Auth-Service está caído.
- **✅ DB**: Confirmación visual de persistencia exitosa en MySQL y MongoDB.
- **--------- [KAFKA]**: Bloques visuales sólidos (`=======`) para trazar eventos asíncronos enriquecidos.
- **🔢 Step-by-Step Flow**: Flujo numerado `[PASO X/5]` para trazabilidad total de extremo a extremo.

---
**Versión**: 3.2.0 (Identity & Notification Hardening)  
**Estado**: Activo - Ecosistema Resiliente 🛡️
