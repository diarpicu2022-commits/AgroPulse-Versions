package com.agropulse.api;

import com.agropulse.dao.GreenhouseDao;
import com.agropulse.dao.UserDao;
import com.agropulse.model.Greenhouse;
import com.agropulse.model.User;
import com.agropulse.pattern.singleton.DatabaseConnection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class GreenhouseRestController extends JsonRestController {

    private static final GreenhouseDao       greenhouseDao = new GreenhouseDao();
    private static final UserDao             userDao       = new UserDao();
    private static final DatabaseConnection  db            = DatabaseConnection.getInstance();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/greenhouses")) {
                if ("GET".equals(method))  listGreenhouses(resp);
                else if ("POST".equals(method)) createGreenhouse(req, resp);
                else sendError(resp, 405, "Method not allowed");

            } else if (path.startsWith("/api/greenhouses/")) {
                String rest  = path.substring("/api/greenhouses/".length()); // e.g. "1" or "1/users" or "1/users/5"
                String[] seg = rest.split("/");
                int id = Integer.parseInt(seg[0]);

                if (seg.length == 1) {
                    // /api/greenhouses/:id
                    if ("GET".equals(method))    getGreenhouse(id, resp);
                    else if ("PUT".equals(method))    updateGreenhouse(id, req, resp);
                    else if ("DELETE".equals(method)) deleteGreenhouse(id, resp);
                    else sendError(resp, 405, "Method not allowed");

                } else if (seg.length == 2 && "users".equals(seg[1])) {
                    // /api/greenhouses/:id/users
                    if ("GET".equals(method))  listGreenhouseUsers(id, resp);
                    else if ("POST".equals(method)) assignUser(id, req, resp);
                    else sendError(resp, 405, "Method not allowed");

                } else if (seg.length == 3 && "users".equals(seg[1])) {
                    // /api/greenhouses/:id/users/:userId
                    int userId = Integer.parseInt(seg[2]);
                    if ("DELETE".equals(method)) removeUser(id, userId, resp);
                    else sendError(resp, 405, "Method not allowed");

                } else {
                    sendError(resp, 404, "Endpoint not found");
                }
            } else {
                sendError(resp, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    // ─── CRUD Greenhouses ─────────────────────────────────────────

    private void listGreenhouses(HttpServletResponse resp) throws IOException {
        List<Greenhouse> list = greenhouseDao.findAll();
        JsonArray arr = new JsonArray();
        for (Greenhouse g : list) arr.add(toJson(g));
        JsonObject result = new JsonObject(); result.add("greenhouses", arr);
        sendJson(resp, result);
    }

    private void getGreenhouse(int id, HttpServletResponse resp) throws IOException {
        var opt = greenhouseDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Invernadero no encontrado"); return; }
        sendJson(resp, toJson(opt.get()));
    }

    private void createGreenhouse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        Greenhouse g = new Greenhouse(
            body.get("name").getAsString(),
            body.has("location")    ? body.get("location").getAsString()    : null,
            body.has("description") ? body.get("description").getAsString() : null,
            body.has("ownerId")     ? body.get("ownerId").getAsInt()        : 1
        );
        greenhouseDao.save(g);
        sendJson(resp, toJson(g));
    }

    private void updateGreenhouse(int id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var opt = greenhouseDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Invernadero no encontrado"); return; }
        Greenhouse g = opt.get();
        JsonObject body = parseBody(req);
        if (body.has("name"))        g.setName(body.get("name").getAsString());
        if (body.has("location"))    g.setLocation(body.get("location").getAsString());
        if (body.has("description")) g.setDescription(body.get("description").getAsString());
        greenhouseDao.update(g);
        sendJson(resp, toJson(g));
    }

    private void deleteGreenhouse(int id, HttpServletResponse resp) throws IOException {
        greenhouseDao.delete(id);
        JsonObject result = new JsonObject(); result.addProperty("deleted", true);
        sendJson(resp, result);
    }

    // ─── User assignment ──────────────────────────────────────────

    private void listGreenhouseUsers(int greenhouseId, HttpServletResponse resp) throws IOException {
        JsonArray arr  = new JsonArray();
        Connection con = db.getConnection();
        String sql = "SELECT u.id, u.username, u.full_name, u.role FROM user_greenhouse ug " +
                     "JOIN users u ON ug.user_id = u.id WHERE ug.greenhouse_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, greenhouseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id",       rs.getInt("id"));
                obj.addProperty("username", rs.getString("username"));
                obj.addProperty("fullName", com.agropulse.util.CryptoUtils.decrypt(rs.getString("full_name")));
                obj.addProperty("role",     rs.getString("role"));
                arr.add(obj);
            }
        } catch (SQLException e) {
            System.err.println("  [Greenhouse] Error listando usuarios: " + e.getMessage());
        }
        JsonObject result = new JsonObject(); result.add("users", arr);
        sendJson(resp, result);
    }

    private void assignUser(int greenhouseId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body   = parseBody(req);
        int        userId = body.get("userId").getAsInt();
        Connection con    = db.getConnection();
        String sql = "INSERT OR IGNORE INTO user_greenhouse (user_id, greenhouse_id, assigned_at) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, greenhouseId);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            // PostgreSQL fallback
            String sqlPg = "INSERT INTO user_greenhouse (user_id, greenhouse_id, assigned_at) VALUES (?,?,?) ON CONFLICT DO NOTHING";
            try (PreparedStatement ps = con.prepareStatement(sqlPg)) {
                ps.setInt(1, userId);
                ps.setInt(2, greenhouseId);
                ps.setString(3, LocalDateTime.now().toString());
                ps.executeUpdate();
            } catch (SQLException e2) {
                System.err.println("  [Greenhouse] Error asignando usuario: " + e2.getMessage());
            }
        }
        JsonObject result = new JsonObject(); result.addProperty("assigned", true);
        sendJson(resp, result);
    }

    private void removeUser(int greenhouseId, int userId, HttpServletResponse resp) throws IOException {
        Connection con = db.getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM user_greenhouse WHERE greenhouse_id=? AND user_id=?")) {
            ps.setInt(1, greenhouseId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [Greenhouse] Error removiendo usuario: " + e.getMessage());
        }
        JsonObject result = new JsonObject(); result.addProperty("removed", true);
        sendJson(resp, result);
    }

    // ─── JSON helper ──────────────────────────────────────────────

    private JsonObject toJson(Greenhouse g) {
        JsonObject json = new JsonObject();
        json.addProperty("id",          g.getId());
        json.addProperty("name",        g.getName());
        json.addProperty("location",    g.getLocation());
        json.addProperty("description", g.getDescription());
        json.addProperty("ownerId",     g.getOwnerId());
        json.addProperty("active",      g.isActive());
        return json;
    }
}
