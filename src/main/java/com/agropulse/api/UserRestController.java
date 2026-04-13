package com.agropulse.api;

import com.agropulse.dao.UserDao;
import com.agropulse.model.User;
import com.agropulse.model.UserRole;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class UserRestController extends JsonRestController {

    private static final UserDao userDao = new UserDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/users")) {
                if ("GET".equals(method)) listUsers(resp);
                else if ("POST".equals(method)) createUser(req, resp);
                else sendError(resp, 405, "Method not allowed");
            } else if (path.startsWith("/api/users/")) {
                int id = Integer.parseInt(path.substring("/api/users/".length()));
                if ("GET".equals(method)) getUser(id, resp);
                else if ("PUT".equals(method)) updateUser(id, req, resp);
                else if ("DELETE".equals(method)) deleteUser(id, resp);
                else sendError(resp, 405, "Method not allowed");
            } else sendError(resp, 404, "Endpoint not found");
        } catch (Exception e) { sendError(resp, 500, e.getMessage()); }
    }

    private void listUsers(HttpServletResponse resp) throws IOException {
        List<User> list = userDao.findAll();
        JsonArray arr = new JsonArray();
        for (User u : list) arr.add(userToJson(u));
        JsonObject result = new JsonObject(); result.add("users", arr);
        sendJson(resp, result);
    }

    private void getUser(int id, HttpServletResponse resp) throws IOException {
        var opt = userDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Usuario no encontrado"); return; }
        sendJson(resp, userToJson(opt.get()));
    }

    private void createUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        User u = new User(
            body.get("username").getAsString(),
            body.get("password").getAsString(),
            body.get("fullName").getAsString(),
            body.has("phone") ? body.get("phone").getAsString() : null,
            body.has("role") ? UserRole.valueOf(body.get("role").getAsString()) : UserRole.USER
        );
        userDao.save(u);
        sendJson(resp, userToJson(u));
    }

    private void updateUser(int id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var opt = userDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Usuario no encontrado"); return; }
        User u = opt.get();
        JsonObject body = parseBody(req);
        if (body.has("fullName")) u.setFullName(body.get("fullName").getAsString());
        if (body.has("phone")) u.setPhone(body.get("phone").getAsString());
        if (body.has("role")) u.setRole(UserRole.valueOf(body.get("role").getAsString()));
        if (body.has("active")) u.setActive(body.get("active").getAsBoolean());
        userDao.update(u);
        sendJson(resp, userToJson(u));
    }

    private void deleteUser(int id, HttpServletResponse resp) throws IOException {
        userDao.delete(id);
        JsonObject result = new JsonObject(); result.addProperty("deleted", true);
        sendJson(resp, result);
    }

    private JsonObject userToJson(User u) {
        JsonObject json = new JsonObject();
        json.addProperty("id", u.getId());
        json.addProperty("username", u.getUsername());
        json.addProperty("fullName", u.getFullName());
        json.addProperty("phone", u.getPhone());
        json.addProperty("role", u.getRole().name());
        json.addProperty("active", u.isActive());
        return json;
    }
}