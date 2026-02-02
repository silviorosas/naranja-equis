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

# Descargar dependencias
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY infrastructure/api-gateway/src infrastructure/api-gateway/src

# Compilar
RUN mvn clean package -DskipTests -pl infrastructure/api-gateway -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/infrastructure/api-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
