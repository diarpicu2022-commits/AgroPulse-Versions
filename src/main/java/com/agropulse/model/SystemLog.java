package com.agropulse.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro de actividad del sistema (logs).
 * El admin puede revisar los logs desde el menú.
 */
public class SystemLog {
    private int id;
    private String action;
    private String details;
    private String performedBy;    // Usuario que ejecutó la acción
    private LocalDateTime timestamp;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --- Constructores ---

    public SystemLog() {
        this.timestamp = LocalDateTime.now();
    }

    public SystemLog(String action, String details, String performedBy) {
        this();
        this.action = action;
        this.details = details;
        this.performedBy = performedBy;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp.format(FMT) + "] " +
               action + " | " + details +
               " | Por: " + performedBy;
    }
}
