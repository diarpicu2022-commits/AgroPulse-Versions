#!/bin/bash
# ╔══════════════════════════════════════════════════════════════╗
# ║   AgroPulse — Guía de Despliegue en la Nube               ║
# ║   Para que el profesor pueda verificar el sistema online    ║
# ╚══════════════════════════════════════════════════════════════╝
#
# OPCIÓN 1 (RECOMENDADA): Railway — gratis, fácil, 1 clic
# OPCIÓN 2: Render — también gratis
# OPCIÓN 3: Docker + VPS (más control)
#
# AgroPulse es una app de escritorio Java Swing.
# Para "estar en la nube" hay dos enfoques:
#
#   A) La BASE DE DATOS sí está en la nube (Supabase — ya configurado ✅)
#   B) Exponer una API REST + Dashboard web (descrito abajo)
#
# ══════════════════════════════════════════════════════════════
#  OPCIÓN A (Ya implementada): Base de datos en Supabase
# ══════════════════════════════════════════════════════════════
#
# ✅ Ya está funcionando con tu Supabase (URL en tu .env → ONLINE_DB_URL)
#
# Para que el PROFESOR lo vea en línea:
#   1. Abre https://supabase.com → tu proyecto
#   2. Ve a Table Editor → verá todos los datos en tiempo real
#   3. Comparte el link del dashboard de tu proyecto
#
# ══════════════════════════════════════════════════════════════
#  OPCIÓN B: API REST + Web Dashboard con Railway
# ══════════════════════════════════════════════════════════════

echo "=== AgroPulse Cloud Deployment ==="
echo ""
echo "PASO 1: Instalar Railway CLI"
echo "  curl -fsSL https://railway.app/install.sh | sh"
echo ""
echo "PASO 2: Login"
echo "  railway login"
echo ""
echo "PASO 3: Crear proyecto"
echo "  railway new agropulse"
echo ""
echo "PASO 4: Configurar variables de entorno"
railway_env() {
cat << 'ENV'
# En Railway dashboard → Variables:
DATABASE_URL=<TU_ONLINE_DB_URL>
GEMINI_API_KEY=<TU_GEMINI_KEY>
GROQ_API_KEY=<TU_GROQ_KEY>
PORT=8080
ENV
}
railway_env

echo ""
echo "PASO 5: Deploy"
echo "  railway up"
echo ""
echo "Tu app estará en: https://agropulse.up.railway.app"
