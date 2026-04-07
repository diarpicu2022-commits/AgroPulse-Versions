package com.agropulse.model;

import java.time.LocalDateTime;

/**
 * Modelo que representa un invernadero físico.
 * Un administrador puede gestionar múltiples invernaderos.
 * Cada usuario regular tiene acceso a uno o más invernaderos asignados.
 */
public class Greenhouse {

    private int    id;
    private String name;           // Nombre del invernadero
    private String location;       // Ubicación / dirección
    private String description;    // Descripción opcional
    private int    ownerId;        // ID del admin que lo creó
    private boolean active;
    private LocalDateTime createdAt;

    // ─── Constructores ────────────────────────────────────────────────

    public Greenhouse() {
        this.active    = true;
        this.createdAt = LocalDateTime.now();
    }

    public Greenhouse(String name, String location, String description, int ownerId) {
        this();
        this.name        = name;
        this.location    = location;
        this.description = description;
        this.ownerId     = ownerId;
    }

    // ─── Getters / Setters ────────────────────────────────────────────

    public int    getId()               { return id; }
    public void   setId(int id)         { this.id = id; }

    public String getName()             { return name; }
    public void   setName(String v)     { this.name = v; }

    public String getLocation()         { return location; }
    public void   setLocation(String v) { this.location = v; }

    public String getDescription()           { return description; }
    public void   setDescription(String v)   { this.description = v; }

    public int    getOwnerId()          { return ownerId; }
    public void   setOwnerId(int v)     { this.ownerId = v; }

    public boolean isActive()           { return active; }
    public void    setActive(boolean v) { this.active = v; }

    public LocalDateTime getCreatedAt()           { return createdAt; }
    public void          setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    @Override
    public String toString() { return name + (location != null && !location.isBlank() ? " — " + location : ""); }
}
