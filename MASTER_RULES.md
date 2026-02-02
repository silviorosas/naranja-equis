# 📜 MASTER_RULES: Configuración del Sistema Multi-Agente NaranjaX

## 📂 Contexto Global del Proyecto
Este es un ecosistema de microservicios financieros (Billetera Virtual) basado en Java 21 y Spring Cloud. El objetivo es emular la robustez de NaranjaX utilizando una arquitectura orientada a eventos con Kafka y resiliencia con Resilience4j.

## 🤖 Reglas de Comportamiento de la IA (Obligatorias)

1. **Sincronización de Documentación**: 
   - Cada vez que generes o modifiques código que afecte la estructura, la base de datos o el flujo, DEBES actualizar automáticamente los archivos: `architecture.md`, `PROJECT_SUMMARY.md` y `README.md`.
   - No esperes a que el usuario lo pida; mantenlos como la "Única Fuente de Verdad".

2. **Protocolo de Endpoints y Postman**:
   - Por cada nuevo Controller o Endpoint creado, añade una sección al `README.md` llamada "🚀 Guía de Pruebas (Postman)".
   - Incluye el CURL exacto y el cuerpo del JSON para probarlo.

3. **Arquitectura y Calidad**:
   - Stack: Spring Boot 3.2.x, Eureka, API Gateway, Config Server.
   - Persistencia: MySQL (Transaccional) y MongoDB (Auditoría).
   - Mensajería: Kafka para comunicación asíncrona (Flujo de registro/billetera).

4. **Seguridad Innegociable**:
   - Todo endpoint debe ser auditado por el rol `@security-auditor`.
   - Uso de JWT y validación de roles en cada servicio.