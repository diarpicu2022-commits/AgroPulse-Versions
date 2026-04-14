# AgroPulse - Dockerfile Multi-stage
# Build + Run en un solo archivo

# ============================================
# STAGE 1: Build (compilar)
# ============================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copiar solo archivos necesarios para build
COPY pom.xml .
COPY src ./src

# Compilar (esto crea el JAR en target/)
RUN mvn package -DskipTests -q

# Copiar base de datos SQLite
COPY agropulse.db /app/agropulse.db

# ============================================
# STAGE 2: Runtime (producción)
# ============================================
FROM eclipse-temurin:17-jre-alpine

# Etiquetas
LABEL maintainer="Diego Armando Pinta Cuasquen"
LABEL version="9.0.0"
LABEL description="AgroPulse - Sistema de Monitoreo de Invernadero"

WORKDIR /app

# Copiar JAR desde stage de build
COPY --from=build /app/target/AgroPulse-2.0.0.jar app.jar

# Exponer puerto REST API
EXPOSE 8080

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV REST_PORT=8080

# Puerto por defecto
EXPOSE 8080

# Iniciar solo REST API (sin GUI)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp app.jar com.agropulse.RestServer"]

# ============================================
# NOTAS PARA DEPLOY:
# ============================================
# Build local:
#   docker build -t agropulse .
#
# Run local:
#   docker run -p 8080:8080 agropulse
#
# Railway:
#   railway login
#   railway new
#   railway up
#
# Docker Hub:
#   docker build -t tu-usuario/agropulse .
#   docker push tu-usuario/agropulse
#
# Render:
#   Render会自动 从 Dockerfile 构建
#   Solo necesitas especificar build command: docker build .
#   Run command: docker run -p 8080:8080 agropulse