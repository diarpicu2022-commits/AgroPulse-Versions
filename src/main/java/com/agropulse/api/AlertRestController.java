package com.agropulse.api;

import com.agropulse.dao.AlertDao;
import com.agropulse.model.Alert;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class AlertRestController extends JsonRestController {

    private static final AlertDao alertDao = new AlertDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/alerts")) {
                if ("GET".equals(method)) listAlerts(req, resp);
                else if ("POST".equals(method)) createAlert(req, resp);
                else sendError(resp, 405, "Method not allowed");
            } else if (path.startsWith("/api/alerts/") && path.endsWith("/read")) {
                int id = Integer.parseInt(path.substring("/api/alerts/".length(), path.lastIndexOf("/")));
                if ("PUT".equals(method)) markAsRead(id, resp);
                else sendError(resp, 405, "Method not allowed");
            } else sendError(resp, 404, "Endpoint not found");
        } catch (Exception e) { sendError(resp, 500, e.getMessage()); }
    }

    private void listAlerts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String unread = req.getParameter("unread");
        List<Alert> list = alertDao.findAll();
        
        JsonArray arr = new JsonArray();
        for (Alert a : list) {
            if (unread != null && !"true".equals(unread) || (unread != null && a.isSent())) continue;
            arr.add(alertToJson(a));
        }
        JsonObject result = new JsonObject(); result.add("alerts", arr);
        sendJson(resp, result);
    }

    private void createAlert(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = parseBody(req);
        Alert a = new Alert(
            body.get("message").getAsString(),
            body.get("level").getAsString()
        );
        alertDao.save(a);
        sendJson(resp, alertToJson(a));
    }

    private void markAsRead(int id, HttpServletResponse resp) throws IOException {
        var opt = alertDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Alerta no encontrada"); return; }
        Alert a = opt.get(); a.setSent(true);
        alertDao.update(a);
        JsonObject result = new JsonObject(); result.addProperty("marked", true);
        sendJson(resp, result);
    }

    private JsonObject alertToJson(Alert a) {
        JsonObject json = new JsonObject();
        json.addProperty("id", a.getId());
        json.addProperty("message", a.getMessage());
        json.addProperty("level", a.getLevel());
        json.addProperty("sent", a.isSent());
        json.addProperty("createdAt", a.getCreatedAt().toString());
        return json;
    }
}