package com.agropulse.api;

import com.agropulse.dao.AutomationRuleDao;
import com.agropulse.model.AutomationRule;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * REST Controller para gestionar reglas de automatización IF/THEN
 * Endpoints:
 *   GET    /api/rules              - Obtener todas las reglas del usuario
 *   POST   /api/rules              - Crear nueva regla
 *   PUT    /api/rules/{id}         - Actualizar regla
 *   DELETE /api/rules/{id}         - Eliminar regla
 */
public class RulesRestController extends JsonRestController {
    private static final AutomationRuleDao ruleDao = new AutomationRuleDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String username = req.getHeader("X-User");
            if (username == null || username.isEmpty()) {
                sendError(resp, 401, "No autorizado");
                return;
            }

            switch (method) {
                case "GET" -> handleGet(path, username, resp);
                case "POST" -> handlePost(path, username, req, resp);
                case "PUT" -> handlePut(path, username, req, resp);
                case "DELETE" -> handleDelete(path, username, resp);
                default -> sendError(resp, 405, "Método no permitido");
            }
        } catch (SQLException e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    private void handleGet(String path, String username, HttpServletResponse resp) throws SQLException, IOException {
        if (path.equals("/api/rules")) {
            // GET /api/rules - Obtener todas las reglas del usuario
            List<AutomationRule> rules = ruleDao.findByUsername(username);
            JsonArray arr = new JsonArray();
            for (AutomationRule rule : rules) {
                JsonObject json = new JsonObject();
                json.addProperty("id", rule.getId());
                json.addProperty("username", rule.getUsername());
                json.addProperty("condition_type", rule.getConditionType());
                json.addProperty("condition_value", rule.getConditionValue());
                json.addProperty("action_type", rule.getActionType());
                json.addProperty("enabled", rule.isEnabled());
                json.addProperty("createdAt", rule.getCreatedAt().toString());
                json.addProperty("lastExecuted", rule.getLastExecuted() != null ? rule.getLastExecuted().toString() : null);
                arr.add(json);
            }
            JsonObject response = new JsonObject();
            response.add("rules", arr);
            sendJson(resp, response);
        } else {
            sendError(resp, 404, "Endpoint no encontrado");
        }
    }

    private void handlePost(String path, String username, HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        if (path.equals("/api/rules")) {
            // POST /api/rules - Crear nueva regla
            JsonObject body = gson.fromJson(req.getReader(), JsonObject.class);

            AutomationRule rule = new AutomationRule();
            rule.setUsername(username);
            rule.setConditionType(body.get("condition_type").getAsString());
            rule.setConditionValue(body.get("condition_value").getAsDouble());
            rule.setActionType(body.get("action_type").getAsString());
            rule.setEnabled(body.has("enabled") ? body.get("enabled").getAsBoolean() : true);

            ruleDao.save(rule);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Regla creada");
            response.addProperty("id", rule.getId());
            sendJson(resp, response);
        } else {
            sendError(resp, 404, "Endpoint no encontrado");
        }
    }

    private void handlePut(String path, String username, HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        if (path.startsWith("/api/rules/")) {
            // PUT /api/rules/{id} - Actualizar regla
            long id = Long.parseLong(path.substring("/api/rules/".length()));
            
            AutomationRule rule = ruleDao.findById(id);
            if (rule == null || !rule.getUsername().equals(username)) {
                sendError(resp, 403, "No tienes permiso para actualizar esta regla");
                return;
            }

            JsonObject body = gson.fromJson(req.getReader(), JsonObject.class);
            rule.setConditionType(body.get("condition_type").getAsString());
            rule.setConditionValue(body.get("condition_value").getAsDouble());
            rule.setActionType(body.get("action_type").getAsString());
            rule.setEnabled(body.has("enabled") ? body.get("enabled").getAsBoolean() : true);

            ruleDao.update(rule);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Regla actualizada");
            sendJson(resp, response);
        } else {
            sendError(resp, 404, "Endpoint no encontrado");
        }
    }

    private void handleDelete(String path, String username, HttpServletResponse resp) throws SQLException, IOException {
        if (path.startsWith("/api/rules/")) {
            // DELETE /api/rules/{id} - Eliminar regla
            long id = Long.parseLong(path.substring("/api/rules/".length()));
            
            AutomationRule rule = ruleDao.findById(id);
            if (rule == null || !rule.getUsername().equals(username)) {
                sendError(resp, 403, "No tienes permiso para eliminar esta regla");
                return;
            }

            ruleDao.delete(id);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Regla eliminada");
            sendJson(resp, response);
        } else {
            sendError(resp, 404, "Endpoint no encontrado");
        }
    }
}
