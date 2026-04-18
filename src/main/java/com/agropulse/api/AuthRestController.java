package com.agropulse.api;

import com.agropulse.dao.UserDao;
import com.agropulse.model.User;
import com.agropulse.model.enums.UserRole;
import com.agropulse.model.SystemLog;
import com.agropulse.dao.SystemLogDao;
import com.agropulse.util.CryptoUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class AuthRestController extends JsonRestController {

    private static final UserDao userDao = new UserDao();
    private static final SystemLogDao logDao = new SystemLogDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            switch (method) {
                case "POST" -> handlePost(path, req, resp);
                case "GET" -> handleGet(path, req, resp);
                case "PUT" -> handlePut(path, req, resp);
                default -> sendError(resp, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    private void handlePost(String path, HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        if (path.equals("/api/auth/login")) {
            login(req, resp);
        } else {
            sendError(resp, 404, "Endpoint not found");
        }
    }

    private void handleGet(String path, HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        if (path.equals("/api/auth/me")) {
            getCurrentUser(req, resp);
        } else if (path.equals("/api/auth/users")) {
            listUsers(req, resp);
        } else {
            sendError(resp, 404, "Endpoint not found");
        }
    }

    private void handlePut(String path, HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        // PUT /api/auth/users/{id}/role - Cambiar rol de usuario (solo ADMIN)
        if (path.matches("/api/auth/users/\\d+/role")) {
            changeUserRole(path, req, resp);
        } else {
            sendError(resp, 404, "Endpoint not found");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        JsonObject body = gson.fromJson(req.getReader(), JsonObject.class);
        
        // Flujo 1: Login con Google (email + name)
        if (body.has("email") && body.has("name")) {
            String email = body.get("email").getAsString();
            String name = body.get("name").getAsString();
            String googleId = body.has("googleId") ? body.get("googleId").getAsString() : email;
            
            // ✅ Obtener email admin desde variable de entorno (no hardcodear)
            String adminEmail = System.getenv("AGROPULSE_ADMIN_EMAIL");
            if (adminEmail == null || adminEmail.isEmpty()) {
                adminEmail = "admin@agropulse.local"; // Fallback seguro
            }
            String role = adminEmail.equals(email) ? "ADMIN" : "USER";
            
            logDao.save(new SystemLog("LOGIN", "Login Google desde webapp", email));
            
            JsonObject response = new JsonObject();
            response.addProperty("id", 0); // Google users don't have DB id
            response.addProperty("username", email);
            response.addProperty("full_name", name);
            response.addProperty("role", role);
            response.addProperty("provider", "GOOGLE");
            
            sendJson(resp, response);
            return;
        }
        
        // Flujo 2: Login con username/password (credenciales locales)
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
        response.addProperty("provider", "LOCAL");

        sendJson(resp, response);
    }

    private void getCurrentUser(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        String username = req.getHeader("X-User");
        if (username == null) {
            sendError(resp, 401, "No autorizado");
            return;
        }

        List<User> users = userDao.findAll();
        User user = users.stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst()
            .orElse(null);

        if (user == null) {
            sendError(resp, 404, "Usuario no encontrado");
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("id", user.getId());
        response.addProperty("username", user.getUsername());
        response.addProperty("full_name", user.getFullName());
        response.addProperty("role", user.getRole().name());

        sendJson(resp, response);
    }

    private void listUsers(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        // Verificar que sea ADMIN
        String adminEmail = req.getHeader("X-Admin-Email");
        if (adminEmail == null || !adminEmail.equals("diarpicu2022@gmail.com")) {
            sendError(resp, 403, "Solo ADMIN puede listar usuarios");
            return;
        }

        List<User> users = userDao.findAll();
        var response = new JsonObject();
        var usersArray = new com.google.gson.JsonArray();
        
        for (User u : users) {
            var userObj = new JsonObject();
            userObj.addProperty("id", u.getId());
            userObj.addProperty("username", u.getUsername());
            userObj.addProperty("full_name", u.getFullName());
            userObj.addProperty("role", u.getRole().name());
            usersArray.add(userObj);
        }
        
        response.add("users", usersArray);
        sendJson(resp, response);
    }

    private void changeUserRole(String path, HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        // Verificar que sea ADMIN
        String adminEmail = req.getHeader("X-Admin-Email");
        if (adminEmail == null || !adminEmail.equals("diarpicu2022@gmail.com")) {
            sendError(resp, 403, "Solo ADMIN puede cambiar roles");
            return;
        }

        // Extraer ID del usuario de la URL: /api/auth/users/{id}/role
        String[] parts = path.split("/");
        long userId = Long.parseLong(parts[4]);
        
        // Obtener nuevo rol del body
        JsonObject body = parseBody(req);
        String newRole = body.has("role") ? body.get("role").getAsString() : "";
        
        // Validar rol
        if (!newRole.equals("ADMIN") && !newRole.equals("USER")) {
            sendError(resp, 400, "Rol inválido. Debe ser ADMIN o USER");
            return;
        }
        
        // Actualizar rol del usuario
        List<User> users = userDao.findAll();
        User targetUser = users.stream()
            .filter(u -> u.getId() == userId)
            .findFirst()
            .orElse(null);
        
        if (targetUser == null) {
            sendError(resp, 404, "Usuario no encontrado");
            return;
        }
        
        // Cambiar rol (usando reflections o actualizar directamente en BD)
        try {
            targetUser.setRole(UserRole.valueOf(newRole));
            userDao.update(targetUser);
            
            logDao.save(new SystemLog("ROLE_CHANGE", "Rol cambiado a " + newRole, targetUser.getUsername()));
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Rol actualizado a " + newRole);
            response.addProperty("user_id", userId);
            sendJson(resp, response);
        } catch (Exception e) {
            sendError(resp, 500, "Error actualizando rol: " + e.getMessage());
        }
    }
}