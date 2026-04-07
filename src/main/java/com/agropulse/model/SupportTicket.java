package com.agropulse.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ticket de soporte técnico.
 * Los usuarios abren tickets; el admin los gestiona y responde.
 */
public class SupportTicket {

    public enum Status { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int          id;
    private int          userId;           // Usuario que abrió el ticket
    private String       userName;         // Para mostrar en UI
    private int          greenhouseId;     // Invernadero relacionado (0 = general)
    private String       greenhouseName;
    private String       subject;          // Asunto
    private String       description;      // Descripción del problema
    private String       adminResponse;    // Respuesta del admin
    private Status       status;
    private Priority     priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Constructor ─────────────────────────────────────────────────

    public SupportTicket() {
        this.status    = Status.OPEN;
        this.priority  = Priority.MEDIUM;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SupportTicket(int userId, int greenhouseId, String subject, String description) {
        this();
        this.userId       = userId;
        this.greenhouseId = greenhouseId;
        this.subject      = subject;
        this.description  = description;
    }

    // ─── Getters / Setters ────────────────────────────────────────────

    public int      getId()                   { return id; }
    public void     setId(int v)              { this.id = v; }

    public int      getUserId()               { return userId; }
    public void     setUserId(int v)          { this.userId = v; }

    public String   getUserName()             { return userName; }
    public void     setUserName(String v)     { this.userName = v; }

    public int      getGreenhouseId()         { return greenhouseId; }
    public void     setGreenhouseId(int v)    { this.greenhouseId = v; }

    public String   getGreenhouseName()            { return greenhouseName; }
    public void     setGreenhouseName(String v)    { this.greenhouseName = v; }

    public String   getSubject()              { return subject; }
    public void     setSubject(String v)      { this.subject = v; }

    public String   getDescription()          { return description; }
    public void     setDescription(String v)  { this.description = v; }

    public String   getAdminResponse()             { return adminResponse; }
    public void     setAdminResponse(String v)     { this.adminResponse = v; }

    public Status   getStatus()               { return status; }
    public void     setStatus(Status v)       { this.status = v; this.updatedAt = LocalDateTime.now(); }

    public Priority getPriority()             { return priority; }
    public void     setPriority(Priority v)   { this.priority = v; }

    public LocalDateTime getCreatedAt()            { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    public String getStatusDisplay() {
        return switch (status) {
            case OPEN        -> "🟡 Abierto";
            case IN_PROGRESS -> "🔵 En proceso";
            case RESOLVED    -> "🟢 Resuelto";
            case CLOSED      -> "⚫ Cerrado";
        };
    }

    public String getPriorityDisplay() {
        return switch (priority) {
            case LOW      -> "🔽 Baja";
            case MEDIUM   -> "▶️ Media";
            case HIGH     -> "🔼 Alta";
            case CRITICAL -> "🚨 Crítica";
        };
    }

    @Override
    public String toString() {
        return "[#" + id + "] " + subject + " | " + getStatusDisplay() +
               " | " + (createdAt != null ? createdAt.format(FMT) : "");
    }
}
