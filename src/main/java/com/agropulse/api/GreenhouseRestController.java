package com.agropulse.api;

import com.agropulse.dao.GreenhouseDao;
import com.agropulse.model.Greenhouse;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class GreenhouseRestController extends JsonRestController {

    private static final GreenhouseDao greenhouseDao = new GreenhouseDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/greenhouses")) {
                if ("GET".equals(method)) listGreenhouses(resp);
                else if ("POST".equals(method)) createGreenhouse(req, resp);
                else sendError(resp, 405, "Method not allowed");
            } else if (path.startsWith("/api/greenhouses/")) {
                int id = Integer.parseInt(path.substring("/api/greenhouses/".length()));
                if ("GET".equals(method)) getGreenhouse(id, resp);
                else if ("PUT".equals(method)) updateGreenhouse(id, req, resp);
                else if ("DELETE".equals(method)) deleteGreenhouse(id, resp);
                else sendError(resp, 405, "Method not allowed");
            } else sendError(resp, 404, "Endpoint not found");
        } catch (Exception e) { sendError(resp, 500, e.getMessage()); }
    }

    private void listGreenhouses(HttpServletResponse resp) throws IOException {
        List<Greenhouse> list = greenhouseDao.findAll();
        JsonArray arr = new JsonArray();
        for (Greenhouse g : list) arr.add(greenhouseToJson(g));
        JsonObject result = new JsonObject(); result.add("greenhouses", arr);
        sendJson(resp, result);
    }

    private void getGreenhouse(int id, HttpServletResponse resp) throws IOException {
        var opt = greenhouseDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Invernadero no encontrado"); return; }
        sendJson(resp, greenhouseToJson(opt.get()));
    }

    private void createGreenhouse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        Greenhouse g = new Greenhouse(
            body.get("name").getAsString(),
            body.has("location") ? body.get("location").getAsString() : null,
            body.has("description") ? body.get("description").getAsString() : null,
            body.has("ownerId") ? body.get("ownerId").getAsInt() : 1
        );
        greenhouseDao.save(g);
        sendJson(resp, greenhouseToJson(g));
    }

    private void updateGreenhouse(int id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var opt = greenhouseDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Invernadero no encontrado"); return; }
        Greenhouse g = opt.get();
        JsonObject body = parseBody(req);
        if (body.has("name")) g.setName(body.get("name").getAsString());
        if (body.has("location")) g.setLocation(body.get("location").getAsString());
        if (body.has("description")) g.setDescription(body.get("description").getAsString());
        greenhouseDao.update(g);
        sendJson(resp, greenhouseToJson(g));
    }

    private void deleteGreenhouse(int id, HttpServletResponse resp) throws IOException {
        greenhouseDao.delete(id);
        JsonObject result = new JsonObject(); result.addProperty("deleted", true);
        sendJson(resp, result);
    }

    private JsonObject greenhouseToJson(Greenhouse g) {
        JsonObject json = new JsonObject();
        json.addProperty("id", g.getId());
        json.addProperty("name", g.getName());
        json.addProperty("location", g.getLocation());
        json.addProperty("description", g.getDescription());
        json.addProperty("ownerId", g.getOwnerId());
        json.addProperty("active", g.isActive());
        return json;
    }
}