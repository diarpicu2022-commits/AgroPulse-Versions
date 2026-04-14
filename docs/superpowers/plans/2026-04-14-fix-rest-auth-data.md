# Fix REST API: Auth + Datos

> **Para agentes:** Usar subagent-driven-development o executing-plans.

**Objetivo:** Arreglar autenticación PBKDF2 y incluir datos SQLite en Railway.

**Arquitectura:**
- AuthRestController usa CryptoUtils.verifyPassword() para verificar hashes PBKDF2
- Dockerfile copia agropulse.db al contenedor

**Stack:** Java, PBKDF2, SQLite, Railway

---

## Tarea 1: Fix AuthRestController con PBKDF2

**Modificar:** `src/main/java/com/agropulse/api/AuthRestController.java:52-77`

- [ ] **Step 1: Agregar import CryptoUtils**

```java
import com.agropulse.util.CryptoUtils;
```

- [ ] **Step 2: Cambiar login() para usar verifyPassword**

```java
private void login(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
    JsonObject body = gson.fromJson(req.getReader(), JsonObject.class);
    String username = body.has("username") ? body.get("username").getAsString() : "";
    String password = body.has("password") ? body.get("password").getAsString() : "";

    List<User> users = userDao.findAll();
    User user = users.stream()
        .filter(u -> u.getUsername().equals(username))
        .findFirst()
        .orElse(null);

    if (user == null || !CryptoUtils.verifyPassword(password, user.getPassword())) {
        sendError(resp, 401, "Credenciales inválidas");
        return;
    }

    logDao.save(new SystemLog("LOGIN", "Login REST desde webapp", user.getUsername()));

    JsonObject response = new JsonObject();
    response.addProperty("id", user.getId());
    response.addProperty("username", user.getUsername());
    response.addProperty("full_name", user.getFullName());
    response.addProperty("role", user.getRole().name());

    sendJson(resp, response);
}
```

- [ ] **Step 3: Compilar localmente**

```bash
cd "C:\Users\Cisna\Documents\Ing Software\Proyecto\AgroPulse v9"
mvn compile -q
```

- [ ] **Step 4: Test login con REST**

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

Expected: `{"id":1,"username":"admin","full_name":"Administrador","role":"ADMIN"}`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/agropulse/api/AuthRestController.java
git commit -m "fix: use PBKDF2 password verification in REST API"
```

---

## Tarea 2: Incluir SQLite en Dockerfile

**Modificar:** `Dockerfile`

- [ ] **Step 1: Copiar agropulse.db al contenedor**

Añadir después de `COPY src ./src`:

```dockerfile
# Copiar base de datos SQLite
COPY agropulse.db /app/agropulse.db
```

- [ ] **Step 2: Commit**

```bash
git add Dockerfile
git commit -m "fix: include SQLite database in Docker image"
```

---

## Tarea 3: Redeploy a Railway

- [ ] **Step 1: Push a GitHub**

```bash
git push origin main
```

- [ ] **Step 2: Trigger Railway deployment**

Railway detecta automáticamente el push y redeploya.

- [ ] **Step 3: Verificar datos**

```bash
curl https://agropulse-versions-production.up.railway.app/api/sensors
```

Expected: `{"sensors":[{"id":1,"name":"Temperatura","type":"DHT22"...}]}`

- [ ] **Step 4: Verificar login**

```bash
curl -X POST https://agropulse-versions-production.up.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

---

## Test commands

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
- Backend: Railway (auto-detecta GitHub push)
- Frontend: Vercel (auto-detecta GitHub push)