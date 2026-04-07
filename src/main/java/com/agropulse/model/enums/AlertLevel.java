package com.agropulse.model.enums;

/**
 * Niveles de alerta para las notificaciones.
 */
public enum AlertLevel {
    INFO("Información"),
    WARNING("Advertencia"),
    CRITICAL("Crítico");

    private final String displayName;

    AlertLevel(String displayName) {
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
