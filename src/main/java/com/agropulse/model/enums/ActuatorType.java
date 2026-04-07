package com.agropulse.model.enums;

/**
 * Tipos de actuador del invernadero según el diagrama.
 */
public enum ActuatorType {
    EXTRACTOR("Extractor de Aire"),
    HEAT_GENERATOR("Generador de Calor"),
    DOOR("Puerta"),
    WATER_PUMP("Bomba de Agua");

    private final String displayName;

    ActuatorType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
