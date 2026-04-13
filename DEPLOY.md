# Deploy AgroPulse a Railway

## Requisitos Previos

- Cuenta en [Railway.app](https://railway.app)
- GitHub repository configurado

## Paso 1: Crear Proyecto en Railway

```bash
# Instalar CLI
npm i -g @railway/cli

# Login
railway login

# Iniciar proyecto (desde la raíz del repo)
railway init
```

## Paso 2: Agregar PostgreSQL

```bash
# En el dashboard de Railway:
# 1. Click "+ New" → "Database" → "PostgreSQL"
# 2. Seleccionar el proyecto creado
```

## Paso 3: Variables de Entorno

En Railway Dashboard → Variables:

```
DB_URL=postgresql://user:password@host:5432/agropulse
REST_PORT=8080
ENV=production
```

## Paso 4: Deploy

```bash
# Deploy automático desde GitHub
railway up
```

## URLs y Endpoints

Una vez deployado, tu API estará en:
- `https://tu-proyecto.up.railway.app/api/sensors`
- `https://tu-proyecto.up.railway.app/api/greenhouses`
- `https://tu-proyecto.up.railway.app/api/crops`
- etc.

## Verificar Funcionamiento

```bash
# Test endpoint
curl https://tu-proyecto.up.railway.app/api/greenhouses
```

## Notas

- Railway crea PostgreSQL, no SQLite
- La app se conecta a la DB via DB_URL
- El puerto es 8080 (configurado en Dockerfile)