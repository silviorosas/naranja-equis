# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .
COPY common-library/pom.xml common-library/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/wallet-service/pom.xml services/wallet-service/
COPY services/transaction-service/pom.xml services/transaction-service/
COPY services/notification-service/pom.xml services/notification-service/
COPY infrastructure/discovery-server/pom.xml infrastructure/discovery-server/
COPY infrastructure/config-server/pom.xml infrastructure/config-server/
COPY infrastructure/api-gateway/pom.xml infrastructure/api-gateway/

# Descargar dependencias para cachear capas
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY common-library/src common-library/src
COPY services/wallet-service/src services/wallet-service/src

# Compilar
RUN mvn clean package -DskipTests -pl services/wallet-service -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar JAR desde build stage
COPY --from=builder /app/services/wallet-service/target/*.jar app.jar

# Puerto
EXPOSE 8082

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/api/actuator/health || exit 1

# Ejecutar aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
