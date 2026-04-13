package com.agropulse.api;

import com.agropulse.dao.SystemLogDao;
import com.agropulse.model.SystemLog;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class LogRestController extends JsonRestController {

    private static final SystemLogDao logDao = new SystemLogDao();
    private static final Gson gson = new Gson();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/logs") && "GET".equals(method)) {
                listLogs(req, resp);
            } else {
                sendError(resp, 404, "Endpoint not found");
            }
        } catch (Exception e) { sendError(resp, 500, e.getMessage()); }
    }

    private void listLogs(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String limitParam = req.getParameter("limit");
        int limit = limitParam != null ? Integer.parseInt(limitParam) : 100;
        
        List<SystemLog> list = logDao.findAll();
        
        JsonArray arr = new JsonArray();
        for (SystemLog l : list) arr.add(logToJson(l));
        JsonObject result = new JsonObject(); result.add("logs", arr);
        sendJson(resp, result);
    }

    private JsonObject logToJson(SystemLog l) {
        JsonObject json = new JsonObject();
        json.addProperty("id", l.getId());
        json.addProperty("action", l.getAction());
        json.addProperty("details", l.getDetails());
        json.addProperty("performedBy", l.getPerformedBy());
        json.addProperty("timestamp", l.getTimestamp().toString());
        return json;
    }
}