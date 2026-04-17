package com.agropulse.model;

import java.time.LocalDateTime;

/**
 * Modelo para las reglas de automatización IF/THEN
 * Ejemplo: SI temp > 28 ENTONCES activar_extractor
 */
public class AutomationRule {
    private long id;
    private String username;
    private String conditionType;      // "temp_high", "temp_low", "humidity_high", "humidity_low", "soil_dry"
    private double conditionValue;     // Ej: 28.5 para temp > 28.5
    private String actionType;         // "activate_extractor", "activate_pump", "open_door", "close_door"
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastExecuted;

    public AutomationRule() {}

    public AutomationRule(long id, String username, String conditionType, double conditionValue,
                        String actionType, boolean enabled) {
        this.id = id;
        this.username = username;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.actionType = actionType;
        this.enabled = enabled;
        this.createdAt = LocalDateTime.now();
    }

    // ──── Getters & Setters ────
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getConditionType() { return conditionType; }
    public void setConditionType(String conditionType) { this.conditionType = conditionType; }

    public double getConditionValue() { return conditionValue; }
    public void setConditionValue(double conditionValue) { this.conditionValue = conditionValue; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastExecuted() { return lastExecuted; }
    public void setLastExecuted(LocalDateTime lastExecuted) { this.lastExecuted = lastExecuted; }

    @Override
    public String toString() {
        return "AutomationRule{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", condition=" + conditionType + ":" + conditionValue +
            ", action=" + actionType +
            ", enabled=" + enabled +
            '}';
    }
}
