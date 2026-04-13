package com.agropulse.api;

import com.agropulse.dao.UserDao;
import com.agropulse.model.User;
import com.agropulse.model.SystemLog;
import com.agropulse.dao.SystemLogDao;
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
        } else {
            sendError(resp, 404, "Endpoint not found");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        JsonObject body = gson.fromJson(req.getReader(), JsonObject.class);
        String username = body.has("username") ? body.get("username").getAsString() : "";
        String password = body.has("password") ? body.get("password").getAsString() : "";

        List<User> users = userDao.findAll();
        User user = users.stream()
            .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
            .findFirst()
            .orElse(null);

        if (user == null) {
            sendError(resp, 401, "Credenciales inválidas");
            return;
        }

        logDao.save(new SystemLog("LOGIN", "Login REST desde webapp", user.getUsername()));

        JsonObject response = new JsonObject();
        response.addProperty("id", user.getId());
        response.addProperty("username", user.getUsername());
        response.addProperty("full_name", user.getFullName());
        response.addProperty("role", user.getRole());

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
        response.addProperty("role", user.getRole());

        sendJson(resp, response);
    }
}