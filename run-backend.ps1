# PowerShell Script: Ejecutar AgroPulse Backend con Supabase
# USO: ./run-backend.ps1
# REQUISITOS: .env archivo con SUPABASE_JDBC_URL configurada

# Detectar ubicación del .env
$envFile = ".\.env"
if (-Not (Test-Path $envFile)) {
    Write-Host "❌ Archivo .env no encontrado" -ForegroundColor Red
    Write-Host "   Por favor, copia .env.example a .env y configura SUPABASE_JDBC_URL" -ForegroundColor Yellow
    exit 1
}

# Cargar variables de entorno desde .env
Write-Host "📝 Cargando variables de entorno desde .env..." -ForegroundColor Cyan
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        if ($name -ne '' -and -not $name.StartsWith('#')) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "  ✓ $name" -ForegroundColor Green
        }
    }
}

Write-Host ""
Write-Host "🔌 Supabase Configuration:" -ForegroundColor Cyan
if ($env:SUPABASE_JDBC_URL) {
    Write-Host "  ✅ SUPABASE_JDBC_URL configured" -ForegroundColor Green
    # Mostrar solo el host sin la contraseña
    if ($env:SUPABASE_JDBC_URL -match 'db\.[a-z0-9]+\.supabase\.co') {
        Write-Host "    Host: $($matches[0])" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  SUPABASE_JDBC_URL NOT configured (usando SQLite)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "🚀 Iniciando AgroPulse Backend..." -ForegroundColor Cyan
Write-Host ""

# Cambiar a directorio del proyecto
cd (Split-Path -Parent $PSCommandPath)

# Ejecutar Maven
mvn exec:java -Dexec.mainClass="com.agropulse.RestServerStandalone" -Dexec.classpathScope="runtime"
