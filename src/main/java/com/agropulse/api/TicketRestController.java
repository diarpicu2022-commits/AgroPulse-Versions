package com.agropulse.api;

import com.agropulse.dao.SupportTicketDao;
import com.agropulse.dao.SystemLogDao;
import com.agropulse.model.SupportTicket;
import com.agropulse.model.SystemLog;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class TicketRestController extends JsonRestController {

    private static final SupportTicketDao ticketDao = new SupportTicketDao();
    private static final SystemLogDao     logDao    = new SystemLogDao();

    @Override
    public void handle(String path, String method, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (path.equals("/api/tickets")) {
                switch (method) {
                    case "GET"  -> listTickets(req, resp);
                    case "POST" -> createTicket(req, resp);
                    default     -> sendError(resp, 405, "Method not allowed");
                }
            } else if (path.matches("/api/tickets/\\d+")) {
                int id = Integer.parseInt(path.substring("/api/tickets/".length()));
                switch (method) {
                    case "GET"    -> getTicket(id, resp);
                    case "PUT"    -> updateTicket(id, req, resp);
                    case "DELETE" -> deleteTicket(id, resp);
                    default       -> sendError(resp, 405, "Method not allowed");
                }
            } else {
                sendError(resp, 404, "Endpoint not found");
            }
        } catch (Exception e) {
            sendError(resp, 500, e.getMessage());
        }
    }

    private void listTickets(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String userRole  = req.getHeader("X-User-Role");
        String userIdStr = req.getHeader("X-User-Id");

        List<SupportTicket> list;
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            list = ticketDao.findAll();
        } else if (userIdStr != null && !userIdStr.isEmpty()) {
            try { list = ticketDao.findByUser(Integer.parseInt(userIdStr)); }
            catch (NumberFormatException e) { list = ticketDao.findAll(); }
        } else {
            list = ticketDao.findAll();
        }

        JsonArray arr = new JsonArray();
        for (SupportTicket t : list) arr.add(toJson(t));
        JsonObject result = new JsonObject();
        result.add("tickets", arr);
        sendJson(resp, result);
    }

    private void getTicket(int id, HttpServletResponse resp) throws IOException {
        var opt = ticketDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Ticket no encontrado"); return; }
        sendJson(resp, toJson(opt.get()));
    }

    private void createTicket(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body      = parseBody(req);
        String     userIdStr = req.getHeader("X-User-Id");
        int        userId    = (userIdStr != null && !userIdStr.isEmpty()) ? Integer.parseInt(userIdStr) : 0;

        SupportTicket t = new SupportTicket(
            userId,
            body.has("greenhouseId") ? body.get("greenhouseId").getAsInt() : 0,
            body.get("subject").getAsString(),
            body.get("description").getAsString()
        );
        if (body.has("priority")) {
            try { t.setPriority(SupportTicket.Priority.valueOf(body.get("priority").getAsString())); }
            catch (Exception ignored) {}
        }

        ticketDao.save(t);
        logDao.save(new SystemLog("TICKET_CREATE", "Ticket creado: " + t.getSubject(), String.valueOf(userId)));
        sendJson(resp, toJson(t));
    }

    private void updateTicket(int id, HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var opt = ticketDao.findById(id);
        if (opt.isEmpty()) { sendError(resp, 404, "Ticket no encontrado"); return; }

        SupportTicket t    = opt.get();
        JsonObject    body = parseBody(req);

        if (body.has("status")) {
            try { t.setStatus(SupportTicket.Status.valueOf(body.get("status").getAsString())); }
            catch (Exception ignored) {}
        }
        if (body.has("priority")) {
            try { t.setPriority(SupportTicket.Priority.valueOf(body.get("priority").getAsString())); }
            catch (Exception ignored) {}
        }
        if (body.has("adminResponse")) {
            t.setAdminResponse(body.get("adminResponse").getAsString());
            if (t.getStatus() == SupportTicket.Status.OPEN)
                t.setStatus(SupportTicket.Status.IN_PROGRESS);
        }

        ticketDao.update(t);
        String who = req.getHeader("X-Admin-Email");
        logDao.save(new SystemLog("TICKET_UPDATE", "Ticket #" + id + " actualizado", who != null ? who : "sistema"));
        sendJson(resp, toJson(t));
    }

    private void deleteTicket(int id, HttpServletResponse resp) throws IOException {
        ticketDao.delete(id);
        JsonObject result = new JsonObject();
        result.addProperty("deleted", true);
        sendJson(resp, result);
    }

    private JsonObject toJson(SupportTicket t) {
        JsonObject j = new JsonObject();
        j.addProperty("id",              t.getId());
        j.addProperty("userId",          t.getUserId());
        j.addProperty("userName",        t.getUserName());
        j.addProperty("greenhouseId",    t.getGreenhouseId());
        j.addProperty("greenhouseName",  t.getGreenhouseName());
        j.addProperty("subject",         t.getSubject());
        j.addProperty("description",     t.getDescription());
        j.addProperty("adminResponse",   t.getAdminResponse());
        j.addProperty("status",          t.getStatus().name());
        j.addProperty("statusDisplay",   t.getStatusDisplay());
        j.addProperty("priority",        t.getPriority().name());
        j.addProperty("priorityDisplay", t.getPriorityDisplay());
        j.addProperty("createdAt",       t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        j.addProperty("updatedAt",       t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : "");
        return j;
    }
}
