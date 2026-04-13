# Implementación: AgroPulse Web con Java Backend

> **Para agentes:** Usar subagent-driven-development o executing-plans para implementar tarea por tarea.

**Objetivo:** Crear API REST en Java Desktop existente, actualizando webapp para usar esa API.

**Arquitectura:**
- Java Desktop actual + REST Controllers (Spring Boot embebido o Jetty)
- Webapp actual refactorizada para llamar al API Java
- SQLite local se mantiene igual, PostgreSQL opcional

**Stack:**
- Java + Maven (Jetty embebido para REST)
- React + Vite (webapp)
- SQLite (local) + PostgreSQL (Railway)

---

## Tareas

### Fase 1: Java REST API

#### Tarea 1.1: Agregar dependencias REST a pom.xml
- Modificar: `pom.xml`
- Agregar: spring-boot-starter-web o Jersey + Jetty

#### Tarea 1.2: Crear REST Controllers
- Crear: `src/main/java/com/agropulse/api/AuthRestController.java`
- Crear: `src/main/java/com/agropulse/api/SensorRestController.java`
- Crear: `src/main/java/com/agropulse/api/CropRestController.java`
- Crear: `src/main/java/com/agropulse/api/GreenhouseRestController.java`
- Crear: `src/main/java/com/agropulse/api/UserRestController.java`

#### Tarea 1.3: Crear Application REST main
- Crear: `src/main/java/com/agropulse/RestServer.java`
- Puerto 8080

---

### Fase 2: Webapp - Actualizar API calls

#### Tarea 2.1: Crear cliente API REST
- Crear: `webapp/src/services/api-client.ts`
- Endpoints: login, sensors, readings, crops, etc.

#### Tarea 2.2: Actualizar App.jsx Login
- Modificar: `webapp/src/App.jsx`
- Cambiar Supabase por llamadas REST

#### Tarea 2.3: Actualizar Dashboard
- Modificar: `webapp/src/pages/Dashboard.jsx`
- Llamar GET /api/sensors

---

## Comandos de test

### Java Backend (desarrollo):
```bash
cd "C:\Users\Cisna\Documents\Ing Software\Proyecto\AgroPulse v9"
mvn clean compile
java -cp "target/classes" com.agropulse.RestServer
```

### Webapp (desarrollo):
```bash
cd webapp
npm run dev
```

### Deploy:
- Backend: Railway (Dockerfile)
- Frontend: Vercel (auto-detecta React)