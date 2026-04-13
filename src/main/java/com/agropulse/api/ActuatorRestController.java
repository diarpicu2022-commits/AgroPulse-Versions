package com.agropulse.api;

import com.agropulse.dao.ActuatorDao;
import com.agropulse.model.Actuator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ActuatorRestController extends JsonRestController {

    private static final ActuatorDao actuatorDao = new ActuatorDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/actuators")) {
                if ("GET".equals(method)) listActuators(resp);
                else if ("POST".equals(method)) createActuator(req, resp);
                else sendError(resp, 405, "Method not allowed");
            } else if (path.startsWith("/api/actuators/")) {
                int id = Integer.parseInt(path.substring("/api/actuators/".length()));
                if ("GET".equals(method)) getActuator(id, resp);
                else if ("PUT".equals(method)) updateActuator(id, req, resp);
                else if ("DELETE".equals(method)) deleteActuator(id, resp);
                else sendError(resp, 405, "Method not allowed");
            } else sendError(resp, 404, "Endpoint not found");
        } catch (Exception e) { sendError(resp, 500, e.getMessage()); }
    }

    private void listActuators(HttpServletResponse resp) throws IOException {
        List<Actuator> list = actuatorDao.findAll();
        JsonArray arr = new JsonArray();
        for (Actuator a : list) arr.add(actuatorToJson(a));
        JsonObject result = new JsonObject(); result.add("actuators", arr);
        sendJson(resp, result);
    }

    private void getActuator(int id, HttpServletResponse resp) throws IOException {
        var opt = actuatorDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Actuador no encontrado"); return; }
        sendJson(resp, actuatorToJson(opt.get()));
    }

    private void createActuator(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        Actuator a = new Actuator(
            body.get("name").getAsString(),
            com.agropulse.model.enums.ActuatorType.valueOf(body.get("type").getAsString())
        );
        actuatorDao.save(a);
        sendJson(resp, actuatorToJson(a));
    }

    private void updateActuator(int id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var opt = actuatorDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Actuador no encontrado"); return; }
        Actuator a = opt.get();
        JsonObject body = parseBody(req);
        if (body.has("enabled")) a.setEnabled(body.get("enabled").getAsBoolean());
        if (body.has("autoMode")) a.setAutoMode(body.get("autoMode").getAsBoolean());
        actuatorDao.update(a);
        sendJson(resp, actuatorToJson(a));
    }

    private void deleteActuator(int id, HttpServletResponse resp) throws IOException {
        actuatorDao.delete(id);
        JsonObject result = new JsonObject(); result.addProperty("deleted", true);
        sendJson(resp, result);
    }

    private JsonObject actuatorToJson(Actuator a) {
        JsonObject json = new JsonObject();
        json.addProperty("id", a.getId());
        json.addProperty("name", a.getName());
        json.addProperty("type", a.getType().name());
        json.addProperty("enabled", a.isEnabled());
        json.addProperty("autoMode", a.isAutoMode());
        return json;
    }
}