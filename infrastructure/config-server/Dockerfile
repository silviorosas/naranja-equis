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
COPY infrastructure/config-server/src infrastructure/config-server/src

# Compilar
RUN mvn clean package -DskipTests -pl infrastructure/config-server -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/infrastructure/config-server/target/*.jar app.jar
RUN mkdir /app/config-repo

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
