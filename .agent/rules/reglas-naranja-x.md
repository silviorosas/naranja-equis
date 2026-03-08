---
trigger: always_on
---

1. Sincronización de Documentación y "Single Source of Truth"
Automatización: Cada modificación debe reflejarse en: architecture.md, PROJECT_SUMMARY.md y README.md.

Actualización de Stack: Mantener versiones reales: Angular 20.2.x, Spring Boot 3.2.2 y Java 21. Prohibido referenciar Angular 18 o versiones obsoletas.

2. Protocolo de Desarrollo y Seguridad
Common Library First: La lógica de JWT (JwtUtils, JwtAuthenticationFilter) es intocable fuera de la librería común.

Validación de Identidad: Todo endpoint de negocio debe aplicar Ownership Validation (validar que userId del UserPrincipal coincida con el recurso).

Auditoría: Endpoints críticos bajo rol ROLE_SECURITY_AUDITOR con log asíncrono en MongoDB 6.0.

3. Comunicación y Resiliencia
Consultas Síncronas: REST + Resilience4j (Circuit Breakers) para validaciones críticas.

Eventos Asíncronos: Kafka para reconciliación de saldos y notificaciones.

Performance: Patrón Cache-Aside con Redis 7.2 para saldos e idempotencia.

4. Calidad de Código (Blindaje SonarQube v3.2)
Quality Gates: Mínimo 80% de cobertura en lógica de negocio (JaCoCo) incluyendo ramas de error y bloques catch.

Protocolo de Blindaje (4 Pilares):
- Foco en Lógica: Prohibido testear DTOs, Entities o Mappers (exclusiones anti-ruido). Centrarse en @Service y @Component.
- Escenarios de Error: Cobertura obligatoria de excepciones específicas y fallbacks de resiliencia.
- Reliability First: Nunca silenciar InterruptedException. Usar siempre Thread.currentThread().interrupt().
- Clean Code: Prohibido usar literales duplicados (usar constantes) y excepciones genéricas (usar BusinessException).

Exclusiones: Configurar Sonar para ignorar DTOs, Mappers y Boilerplate.

🛡️ Regla de Integridad y No Duplicación (Anti-Chaos)
1. Protocolo de "Common-Library" Obligatorio
Prohibición de Duplicación: Prohibido crear DTOs, Excepciones o Constantes localmente si pueden compartirse.

Refactorización: Si un servicio requiere un DTO similar a uno existente, se refactoriza el original en la common-library para hacerlo genérico.

2. Preservación del Contrato de Interfaz
Inmutabilidad: No modificar campos en eventos de Kafka o ApiResponse sin asegurar compatibilidad hacia atrás.

Balanceo: Toda comunicación nueva debe ser vía Eureka y protegida por Resilience4j.

3. Coherencia de Versiones y Stack
Detección de Colisiones: Escaneo obligatorio del Parent POM para heredar versiones y evitar "Jar Hell".

Angular 20: Uso estricto de Standalone Components y servicios inyectables; prohibido el uso de módulos (NgModules) antiguos.

4. Validación de Impacto (Checklist Pre-Cambio)
Antes de proponer un cambio, la IA debe validar internamente:

¿Este código ya existe en common-library?

¿Este cambio rompe la validación JWT en el Gateway?

¿Cómo afecta este cambio al Quality Gate de SonarQube?