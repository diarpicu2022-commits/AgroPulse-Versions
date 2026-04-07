package com.agropulse.model.enums;

/**
 * Tipos de sensor del invernadero según el diagrama.
 */
public enum SensorType {
    TEMPERATURE_INTERNAL("Temperatura Interna", "°C"),
    TEMPERATURE_EXTERNAL("Temperatura Externa", "°C"),
    HUMIDITY("Humedad Relativa", "%"),
    SOIL_MOISTURE("Humedad del Suelo", "%");

    private final String displayName;
    private final String unit;

    SensorType(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return displayName + " (" + unit + ")";
    }
}
