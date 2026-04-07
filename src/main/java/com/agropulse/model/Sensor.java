package com.agropulse.model;

import com.agropulse.model.enums.SensorType;

/**
 * Modelo de sensor del invernadero.
 * Cada sensor tiene un tipo, ubicación y un último valor leído.
 */
public class Sensor {
    private int id;
    private String name;
    private SensorType type;
    private String location;      // "interno" o "externo"
    private double lastValue;
    private double minValue;          // Límite mínimo para generar alertas
    private double maxValue;          // Límite máximo para generar alertas
    private boolean active;

    // --- Constructores ---

    public Sensor() {
        this.active = true;
    }

    public Sensor(String name, SensorType type, String location) {
        this();
        this.name = name;
        this.type = type;
        this.location = location;
        this.lastValue = 0.0;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SensorType getType() { return type; }
    public void setType(SensorType type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getLastValue() { return lastValue; }
    public void setLastValue(double lastValue) { this.lastValue = lastValue; }

    public double getMinValue() { return minValue; }
    public void setMinValue(double minValue) { this.minValue = minValue; }

    public double getMaxValue() { return maxValue; }
    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "Sensor: " + name +
               " | Tipo: " + type.getDisplayName() +
               " | Ubicación: " + location +
               " | Valor: " + String.format("%.2f", lastValue) + " " + type.getUnit() +
               " | " + (active ? "Activo" : "Inactivo");
    }
}
