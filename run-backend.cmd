@echo off
REM Script: Ejecutar AgroPulse Backend con Supabase
REM USO: run-backend.cmd
REM REQUISITOS: .env archivo con SUPABASE_JDBC_URL configurada

setlocal enabledelayedexpansion

REM Detectar .env
if not exist .env (
    echo.
    echo ❌ Archivo .env no encontrado
    echo.
    echo    Por favor copia .env.example a .env
    echo    y configura SUPABASE_JDBC_URL
    echo.
    exit /b 1
)

REM Cargar variables desde .env
echo.
echo 📝 Cargando variables de entorno desde .env...

for /f "usebackq eol=# delims==" %%a in (.env) do (
    set "%%a"
    echo   ✓ %%a
)

echo.
echo 🔌 Supabase Configuration:
if defined SUPABASE_JDBC_URL (
    echo   ✅ SUPABASE_JDBC_URL LOADED
    echo   URL: !SUPABASE_JDBC_URL!
) else (
    echo   ⚠️  SUPABASE_JDBC_URL NOT found ^(usando SQLite^)
)

echo.
echo 🚀 Iniciando AgroPulse Backend...
echo.

REM Ejecutar Maven con variable de entorno explícita
mvn exec:java -Dexec.mainClass="com.agropulse.RestServerStandalone" -Dexec.classpathScope="runtime"

pause
