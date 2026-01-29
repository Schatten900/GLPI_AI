# ===========================================
# Multi-stage Dockerfile para AiClassificator
# ===========================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copia apenas arquivos de dependencia primeiro (cache layer)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Baixa dependencias (cached se pom.xml nao mudar)
RUN mvn dependency:go-offline -B

# Copia codigo fonte
COPY src ./src

# Compila o projeto
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Cria usuario nao-root para seguranca
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copia o JAR do stage de build
COPY --from=builder /app/target/*.jar app.jar

# Define ownership
RUN chown -R appuser:appgroup /app

# Usa usuario nao-root
USER appuser

# Expoe porta
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

# Variaveis de ambiente com defaults
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SERVER_PORT=8080

# Entrypoint
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
