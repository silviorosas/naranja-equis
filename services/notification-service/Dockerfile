# FASE 1: Builder - Optimización de capas y aislamiento de módulos
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# 1. Copiar POM raíz y "limpiarlo" de módulos innecesarios para esta build
# Esto evita el error "Child module ... does not exist" sin copiar todo el repo
COPY pom.xml .
RUN sed -i '/<modules>/,/<\/modules>/c\<modules><module>common-library</module><module>services/notification-service</module></modules>' pom.xml

# 2. Copiar los POMs de los módulos requeridos (Common Library + Notification)
COPY common-library/pom.xml common-library/
COPY services/notification-service/pom.xml services/notification-service/

# 3. Descargar dependencias - Enfoque selectivo (Layer Caching)
# -pl: PROJECT LIST, -am: ALSO MAKE (incluye dependencias como common-library)
RUN mvn dependency:go-offline -B -pl services/notification-service -am

# 4. Copiar código fuente y compilar el JAR final
COPY common-library/src common-library/src
COPY services/notification-service/src services/notification-service/src
RUN mvn clean package -DskipTests -pl services/notification-service -am

# FASE 2: Runtime - Seguridad y performance
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Blindaje: Usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el artefacto compilado
COPY --from=builder /app/services/notification-service/target/*.jar app.jar

EXPOSE 8084
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Healthcheck interno del ecosistema
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
