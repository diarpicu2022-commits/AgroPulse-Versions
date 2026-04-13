package com.agropulse.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class JsonRestController {

    protected void sendJson(HttpServletResponse resp, com.google.gson.JsonObject json) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter w = resp.getWriter()) {
            w.print(json.toString());
        }
    }

    protected void sendError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(code);
        com.google.gson.JsonObject error = new com.google.gson.JsonObject();
        error.addProperty("error", message);
        try (PrintWriter w = resp.getWriter()) {
            w.print(error.toString());
        }
    }

    protected com.google.gson.JsonObject parseBody(HttpServletRequest req) throws IOException {
        return com.google.gson.JsonParser.parseReader(req.getReader())
            .getAsJsonObject();
    }

    public abstract void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException;
}