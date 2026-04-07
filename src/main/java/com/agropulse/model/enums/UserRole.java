package com.agropulse.model.enums;

/**
 * Roles de usuario del sistema.
 * ADMIN: configura max/min, descarga datos, revisa logs, activa/desactiva APIs.
 * USER:  activa/desactiva actuadores, ingresa cultivos, monitorea sensores.
 */
public enum UserRole {
    ADMIN("Administrador"),
    USER("Usuario");

    private final String displayName;

    UserRole(String displayName) {
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
