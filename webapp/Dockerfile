# AgroPulse Webapp - Dockerfile
# Multi-stage: Build + Nginx para producción

# ============================================
# STAGE 1: Build (compilar React)
# ============================================
FROM node:18-alpine AS build

WORKDIR /app

# Copiar package files
COPY webapp/package.json webapp/package-lock.json ./

# Instalar dependencias
RUN npm install

# Copiar código fuente
COPY webapp/src ./src
COPY webapp/public ./public
COPY webapp/index.html ./
COPY webapp/vite.config.js ./
COPY webapp/.env* ./

# Variables de entorno para producción
ENV VITE_API_URL=https://agropulse-versions-production.up.railway.app
ENV NODE_ENV=production

# Build de producción
RUN npm run build

# ============================================
# STAGE 2: Servir con Nginx
# ============================================
FROM nginx:alpine

# Copiar build desde stage anterior
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar configuración nginx
COPY webapp/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Iniciar nginx
CMD ["nginx", "-g", "daemon off;"]

# ============================================
# NOTAS PARA DEPLOY:
# ============================================
# Build local:
#   docker build -t agropulse-web .
#
# Run local:
#   docker run -p 3000:80 agropulse-web
#
# Vercel (recomendado):
#   npm i -g vercel
#   cd webapp
#   vercel
#
# Docker Hub:
#   docker build -t tu-usuario/agropulse-web .
#   docker push tu-usuario/agropulse-web