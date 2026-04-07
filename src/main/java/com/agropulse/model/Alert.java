package com.agropulse.model;

import com.agropulse.model.enums.AlertLevel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo de alerta generada por el sistema.
 * Se envían por WhatsApp y se registran en la BD.
 */
public class Alert {
    private int id;
    private String message;
    private AlertLevel level;
    private boolean sent;           // ¿Se envió por WhatsApp?
    private LocalDateTime createdAt;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --- Constructores ---

    public Alert() {
        this.createdAt = LocalDateTime.now();
        this.sent = false;
    }

    public Alert(String message, AlertLevel level) {
        this();
        this.message = message;
        this.level = level;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AlertLevel getLevel() { return level; }
    public void setLevel(AlertLevel level) { this.level = level; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "[" + level.getDisplayName() + "] " +
               createdAt.format(FMT) + " - " + message +
               (sent ? " (Enviada)" : " (Pendiente)");
    }
}
