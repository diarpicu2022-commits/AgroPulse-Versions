package com.agropulse.api;

import com.agropulse.dao.SensorDao;
import com.agropulse.model.Sensor;
import com.agropulse.model.enums.SensorType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class SensorRestController extends JsonRestController {

    private static final SensorDao sensorDao = new SensorDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/sensors")) {
                if ("GET".equals(method)) {
                    listSensors(resp);
                } else if ("POST".equals(method)) {
                    createSensor(req, resp);
                } else {
                    sendError(resp, 405, "Method not allowed");
                }
            } else if (path.startsWith("/api/sensors/")) {
                int id = Integer.parseInt(path.substring("/api/sensors/".length()));
                if ("GET".equals(method)) {
                    getSensor(id, resp);
                } else if ("PUT".equals(method)) {
                    updateSensor(id, req, resp);
                } else if ("DELETE".equals(method)) {
                    deleteSensor(id, resp);
                } else {
                    sendError(resp, 405, "Method not allowed");
                }
            } else {
                sendError(resp, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    private void listSensors(HttpServletResponse resp) throws IOException {
        List<Sensor> sensors = sensorDao.findAll();
        JsonArray arr = new JsonArray();
        for (Sensor s : sensors) {
            arr.add(sensorToJson(s));
        }
        JsonObject result = new JsonObject();
        result.add("sensors", arr);
        sendJson(resp, result);
    }

    private void getSensor(int id, HttpServletResponse resp) throws IOException {
        var opt = sensorDao.findById(id);
        if (opt.isEmpty()) {
            sendError(resp, 404, "Sensor no encontrado");
            return;
        }
        sendJson(resp, sensorToJson(opt.get()));
    }

    private void createSensor(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        Sensor sensor = new Sensor(
            body.get("name").getAsString(),
            SensorType.valueOf(body.get("type").getAsString()),
            body.has("location") ? body.get("location").getAsString() : null
        );
        sensorDao.save(sensor);
        sendJson(resp, sensorToJson(sensor));
    }

    private void updateSensor(int id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var opt = sensorDao.findById(id);
        if (opt.isEmpty()) {
            sendError(resp, 404, "Sensor no encontrado");
            return;
        }
        Sensor sensor = opt.get();
        JsonObject body = parseBody(req);
        if (body.has("name")) sensor.setName(body.get("name").getAsString());
        if (body.has("minValue")) sensor.setMinValue(body.get("minValue").getAsDouble());
        if (body.has("maxValue")) sensor.setMaxValue(body.get("maxValue").getAsDouble());
        sensorDao.update(sensor);
        sendJson(resp, sensorToJson(sensor));
    }

    private void deleteSensor(int id, HttpServletResponse resp) throws IOException {
        var opt = sensorDao.findById(id);
        if (opt.isEmpty()) {
            sendError(resp, 404, "Sensor no encontrado");
            return;
        }
        sensorDao.delete(id);
        JsonObject result = new JsonObject();
        result.addProperty("deleted", true);
        sendJson(resp, result);
    }

    private JsonObject sensorToJson(Sensor s) {
        JsonObject json = new JsonObject();
        json.addProperty("id", s.getId());
        json.addProperty("name", s.getName());
        json.addProperty("type", s.getType().name());
        json.addProperty("location", s.getLocation());
        json.addProperty("lastValue", s.getLastValue());
        json.addProperty("minValue", s.getMinValue());
        json.addProperty("maxValue", s.getMaxValue());
        json.addProperty("active", s.isActive());
        return json;
    }
}