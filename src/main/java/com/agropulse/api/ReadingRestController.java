package com.agropulse.api;

import com.agropulse.dao.SensorReadingDao;
import com.agropulse.model.SensorReading;
import com.agropulse.model.enums.SensorType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ReadingRestController extends JsonRestController {

    private static final SensorReadingDao readingDao = new SensorReadingDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/readings")) {
                if ("GET".equals(method)) listReadings(req, resp);
                else if ("POST".equals(method)) createReading(req, resp);
                else sendError(resp, 405, "Method not allowed");
            } else sendError(resp, 404, "Endpoint not found");
        } catch (Exception e) { sendError(resp, 500, e.getMessage()); }
    }

    private void listReadings(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sensorParam = req.getParameter("sensor");
        String limitParam = req.getParameter("limit");
        
        int limit = limitParam != null ? Integer.parseInt(limitParam) : 100;
        List<SensorReading> list;
        
        if (sensorParam != null && !sensorParam.isEmpty()) {
            // Filtrar por sensor específico
            int sensorId = Integer.parseInt(sensorParam);
            list = readingDao.findBySensorId(sensorId, limit);
        } else {
            // Sin filtro: devolver TODAS las últimas N lecturas
            list = readingDao.findAll();
            if (list.size() > limit) {
                list = list.subList(0, limit);
            }
        }
        
        JsonArray arr = new JsonArray();
        for (SensorReading r : list) arr.add(readingToJson(r));
        JsonObject result = new JsonObject(); result.add("readings", arr);
        sendJson(resp, result);
    }

    private void createReading(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        SensorReading r = new SensorReading(
            body.get("sensorId").getAsInt(),
            SensorType.valueOf(body.get("sensorType").getAsString()),
            body.get("value").getAsDouble()
        );
        readingDao.save(r);
        sendJson(resp, readingToJson(r));
    }

    private JsonObject readingToJson(SensorReading r) {
        JsonObject json = new JsonObject();
        json.addProperty("id", r.getId());
        json.addProperty("sensorId", r.getSensorId());
        json.addProperty("sensorType", r.getSensorType().name());
        json.addProperty("value", r.getValue());
        json.addProperty("timestamp", r.getTimestamp().toString());
        json.addProperty("source", r.getSource());
        return json;
    }
}