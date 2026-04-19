@echo off
REM Script: Ejecutar AgroPulse Frontend
REM USO: run-frontend.cmd

setlocal enabledelayedexpansion

REM Cambiar a directorio del frontend
cd /d "%~dp0\webapp"

echo.
echo 🎨 Iniciando AgroPulse Frontend Dev Server...
echo.

REM Ejecutar Vite
npm run dev

pause
