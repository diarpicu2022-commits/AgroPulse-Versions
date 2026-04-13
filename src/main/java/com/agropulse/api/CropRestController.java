package com.agropulse.api;

import com.agropulse.dao.CropDao;
import com.agropulse.model.Crop;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class CropRestController extends JsonRestController {

    private static final CropDao cropDao = new CropDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/crops")) {
                if ("GET".equals(method)) {
                    listCrops(resp);
                } else if ("POST".equals(method)) {
                    createCrop(req, resp);
                } else {
                    sendError(resp, 405, "Method not allowed");
                }
            } else if (path.startsWith("/api/crops/")) {
                int id = Integer.parseInt(path.substring("/api/crops/".length()));
                if ("GET".equals(method)) {
                    getCrop(id, resp);
                } else if ("PUT".equals(method)) {
                    updateCrop(id, req, resp);
                } else if ("DELETE".equals(method)) {
                    deleteCrop(id, resp);
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

    private void listCrops(HttpServletResponse resp) throws IOException {
        List<Crop> crops = cropDao.findAll();
        JsonArray arr = new JsonArray();
        for (Crop c : crops) {
            arr.add(cropToJson(c));
        }
        JsonObject result = new JsonObject();
        result.add("crops", arr);
        sendJson(resp, result);
    }

    private void getCrop(int id, HttpServletResponse resp) throws IOException {
        var opt = cropDao.findById(id);
        if (opt.isEmpty()) {
            sendError(resp, 404, "Cultivo no encontrado");
            return;
        }
        sendJson(resp, cropToJson(opt.get()));
    }

    private void createCrop(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        Crop crop = new Crop(
            body.get("name").getAsString(),
            body.has("variety") ? body.get("variety").getAsString() : null,
            body.get("tempMin").getAsDouble(),
            body.get("tempMax").getAsDouble(),
            body.get("humidityMin").getAsDouble(),
            body.get("humidityMax").getAsDouble(),
            body.get("soilMoistureMin").getAsDouble(),
            body.get("soilMoistureMax").getAsDouble()
        );
        cropDao.save(crop);
        sendJson(resp, cropToJson(crop));
    }

    private void updateCrop(int id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var opt = cropDao.findById(id);
        if (opt.isEmpty()) {
            sendError(resp, 404, "Cultivo no encontrado");
            return;
        }
        Crop crop = opt.get();
        JsonObject body = parseBody(req);
        if (body.has("name")) crop.setName(body.get("name").getAsString());
        if (body.has("tempMin")) crop.setTempMin(body.get("tempMin").getAsDouble());
        if (body.has("tempMax")) crop.setTempMax(body.get("tempMax").getAsDouble());
        cropDao.update(crop);
        sendJson(resp, cropToJson(crop));
    }

    private void deleteCrop(int id, HttpServletResponse resp) throws IOException {
        cropDao.delete(id);
        JsonObject result = new JsonObject();
        result.addProperty("deleted", true);
        sendJson(resp, result);
    }

    private JsonObject cropToJson(Crop c) {
        JsonObject json = new JsonObject();
        json.addProperty("id", c.getId());
        json.addProperty("name", c.getName());
        json.addProperty("variety", c.getVariety());
        json.addProperty("tempMin", c.getTempMin());
        json.addProperty("tempMax", c.getTempMax());
        json.addProperty("humidityMin", c.getHumidityMin());
        json.addProperty("humidityMax", c.getHumidityMax());
        json.addProperty("soilMoistureMin", c.getSoilMoistureMin());
        json.addProperty("soilMoistureMax", c.getSoilMoistureMax());
        json.addProperty("active", c.isActive());
        return json;
    }
}