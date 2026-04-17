# 🚀 GUÍA DE DESPLIEGUE - AgroPulse v6.0

## 📋 Requisitos Previos

### Sistema
- Node.js 18+ (para web app)
- Java 17+ (para backend)
- Maven 3.9+ (para build)
- SQLite3 (para BD)

### Puertos Requeridos
- 8080 (Backend Java REST API)
- 5173 (Frontend Web - Vite dev)
- Puerto COM/Serial del ESP32 (configurar en app)

---

## 🔧 COMPILACIÓN Y DESPLIEGUE

### 1. Backend (Java)

#### Opción A: Compilar con Maven
```bash
cd "c:\Users\Cisna\Documents\Ing Software\Proyecto\AgroPulse v9"

# Limpiar y compilar
mvn clean package -DskipTests

# Resultado: target/AgroPulse-2.0.0.jar
```

#### Opción B: Ejecutar directamente
```bash
# Desde la raíz del proyecto
mvn spring-boot:run

# O si tienes Maven instalado globalmente
mvn clean install
java -jar target/AgroPulse-2.0.0.jar
```

#### Verificar que está corriendo
```bash
curl http://localhost:8080/api/sensors
# Debería retornar JSON con lista de sensores
```

---

### 2. Frontend (Web App)

#### Instalación de dependencias
```bash
cd webapp
npm install
# Espera a que terminen las descargas (~2-3 minutos)
```

#### Desarrollo (modo con hot reload)
```bash
npm run dev
# Abre http://localhost:5173 en el navegador
```

#### Build para producción
```bash
npm run build
# Crea carpeta dist/ con archivos optimizados
```

#### Serve build de producción localmente
```bash
npm run preview
# Abre http://localhost:4173
```

---

## 🗄️ BASE DE DATOS

### Ubicación del archivo SQLite
```
proyecto/
└── agropulse.db
```

### Crear DB desde cero (si no existe)
El backend Java creará automáticamente las tablas en el primer inicio.

### Resetear la BD (borrar datos)
```bash
# Windows
del agropulse.db

# Linux/Mac
rm agropulse.db

# Luego reinicia el backend para crear nueva BD
```

---

## ⚙️ CONFIGURACIÓN

### Variables de Entorno (.env en webapp/)

```bash
# API Backend
VITE_API_URL=http://localhost:8080

# Supabase (opcional)
VITE_SUPABASE_URL=https://xxxx.supabase.co
VITE_SUPABASE_ANON_KEY=eyJ...

# IAs (opcional)
VITE_GROQ_KEY=gsk_...
VITE_GITHUB_TOKEN=ghp_...
VITE_GEMMA_KEY=AIza...
```

### Configuración ESP32

#### Por Serial (USB)
```
Com Port: COM3 (o el que uses)
Baud Rate: 115200
Protocolo: JSON o CSV

Ejemplo JSON que envía el ESP32:
{"temp_in": 24.5, "temp_out": 19.2, "humidity": 65.0, "soil": 42.0}
```

#### Por WiFi/HTTP
```
ESP32 debe hacer POST a:
http://<IP_PC>:8080/data
```

#### Por Archivo
```
Formatos soportados:
- CSV: timestamp, temp_in, temp_out, humidity, soil
- JSON: Array de objetos con los mismos campos
```

---

## ✅ VERIFICACIÓN

### Backend OK?
```bash
# 1. Verificar puerto 8080 abierto
netstat -ano | findstr :8080

# 2. Hacer una petición de prueba
curl http://localhost:8080/api/sensors

# 3. Debería retornar:
{
  "sensors": []
}
```

### Frontend OK?
```bash
# 1. Verificar puerto 5173
netstat -ano | findstr :5173

# 2. Abrir navegador en:
http://localhost:5173

# 3. Debería cargar la página de login
```

### Conexión ESP32 OK?
```
En la app web:
1. Ve a Sensores
2. Verifica que haya sensores creados
3. Ve a Dashboard
4. Deberías ver datos actualizándose cada 5 segundos
```

---

## 🚨 TROUBLESHOOTING

### Error: "Cannot find module 'react'"
```bash
# Solución
cd webapp
rm -rf node_modules package-lock.json
npm install
npm run dev
```

### Error: "Port 5173 already in use"
```bash
# Solución: matar proceso en puerto 5173
netstat -ano | findstr :5173
taskkill /PID <PID> /F

# O usar otro puerto
npm run dev -- --port 3000
```

### Error: "Cannot connect to API"
```bash
# Verificar que:
1. Backend está corriendo en puerto 8080
2. VITE_API_URL está correcto en .env
3. Firewall permite localhost:8080
4. No hay CORS issues (browser console)
```

### Error: "No hay datos en Dashboard"
```bash
# Verificar que:
1. Hay sensores creados (/api/sensors)
2. Backend está recibiendo datos del ESP32
3. Base de datos no está corrupta
4. Check logs en navegador (F12 → Console)
```

### Backend no inicia
```bash
# Verificar que:
1. Java 17+ está instalado: java -version
2. Puerto 8080 no está en uso
3. Permisos de escritura en carpeta
4. Check logs: busca "ERROR" en output
```

---

## 📦 DESPLIEGUE EN PRODUCCIÓN

### Opción 1: Railway (Recomendado)
```bash
# 1. Instalar Railway CLI
npm i -g @railway/cli

# 2. Login
railway login

# 3. Link proyecto
railway link

# 4. Deploy
railway up
```

### Opción 2: Heroku
```bash
# 1. Instalar Heroku CLI
# 2. Login
heroku login

# 3. Create app
heroku create agropulse-app

# 4. Deploy
git push heroku main
```

### Opción 3: Docker (para servidor propio)
```dockerfile
# Dockerfile para frontend
FROM node:18-alpine
WORKDIR /app
COPY . .
RUN npm install && npm run build
EXPOSE 3000
CMD ["npm", "run", "preview"]
```

```dockerfile
# Dockerfile para backend
FROM openjdk:17-slim
COPY target/AgroPulse-2.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 🔒 SEGURIDAD

### Para Producción
```bash
# 1. Cambiar contraseña admin
# Login en app → Configuración → Usuarios → admin

# 2. Generar JWT secret
openssl rand -base64 32

# 3. Configurar variables de entorno
SECURITY_JWT_SECRET=<generado>
DATABASE_URL=<URL_BD_producción>
ALLOWED_ORIGINS=https://agropulse.example.com
```

### SSL/TLS
```bash
# Usar reverse proxy (Nginx)
# Configurar certificados Let's Encrypt
# Forzar HTTPS
```

---

## 📊 MONITOREO

### Logs Backend
```bash
# Windows
type agropulse.log

# Linux
tail -f agropulse.log
```

### Logs Frontend (Browser)
```
F12 → Console → Buscar errores
```

### Métricas
- CPU usage del backend
- Memoria disponible
- Espacio en disco (BD SQLite)
- Conexiones activas

---

## 🔄 ACTUALIZACIÓN

### Actualizar Frontend
```bash
cd webapp
git pull origin main
npm install
npm run build
npm run preview
```

### Actualizar Backend
```bash
git pull origin main
mvn clean package
# Reiniciar con nuevo JAR
```

---

## 📝 CHECKLIST PREPRODUCCIÓN

- [ ] Backend compilado y corriendo
- [ ] Frontend compilado y corriendo
- [ ] Base de datos creada
- [ ] ESP32 enviando datos
- [ ] Dashboard mostrando datos en vivo
- [ ] Todos los menús funcionales
- [ ] Usuarios creados
- [ ] IAs configuradas (Groq/GitHub/Gemma)
- [ ] SSL/TLS configurado
- [ ] Backups automáticos configurados
- [ ] Monitoreo configurado
- [ ] Documentación actualizada

---

## 📞 SOPORTE

### Documentación
- Backend: [src/main/java/README.md](../../src/main/java/README.md)
- Frontend: [webapp/README.md](../../webapp/README.md)
- ESP32: [esp32_firmware/README.md](../../esp32_firmware/README.md)

### Logs de Errores
```
Backend:   Check Maven/Java logs
Frontend:  F12 → Console → Network → Application
Database:  Check SQLite file permissions
ESP32:     Serial Monitor at 115200 baud
```

---

**Documento**: Guía de Despliegue AgroPulse v6.0
**Versión**: 1.0
**Fecha**: 2026-04-17
**Estado**: ✅ Actualizado y verificado
