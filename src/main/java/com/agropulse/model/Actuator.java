package com.agropulse.model;

import com.agropulse.model.enums.ActuatorType;
import java.time.LocalDateTime;

/**
 * Modelo de actuador del invernadero.
 * Extractor, generador de calor, puerta, bomba de agua.
 */
public class Actuator {
    private int id;
    private String name;
    private ActuatorType type;
    private boolean enabled;       // Encendido/Apagado
    private boolean autoMode;      // Modo automático o manual
    private LocalDateTime lastToggled;

    // --- Constructores ---

    public Actuator() {
        this.enabled = false;
        this.autoMode = true;
    }

    public Actuator(String name, ActuatorType type) {
        this();
        this.name = name;
        this.type = type;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ActuatorType getType() { return type; }
    public void setType(ActuatorType type) { this.type = type; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.lastToggled = LocalDateTime.now();
    }

    public boolean isAutoMode() { return autoMode; }
    public void setAutoMode(boolean autoMode) { this.autoMode = autoMode; }

    public LocalDateTime getLastToggled() { return lastToggled; }
    public void setLastToggled(LocalDateTime lastToggled) { this.lastToggled = lastToggled; }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    @Override
    public String toString() {
        return "Actuador: " + name +
               " | Tipo: " + type.getDisplayName() +
               " | Estado: " + (enabled ? "ENCENDIDO" : "APAGADO") +
               " | Modo: " + (autoMode ? "Automático" : "Manual");
    }
}
