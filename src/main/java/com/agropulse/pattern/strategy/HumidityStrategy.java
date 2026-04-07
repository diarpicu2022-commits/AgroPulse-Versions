package com.agropulse.pattern.strategy;

import com.agropulse.model.Actuator;
import com.agropulse.model.enums.SensorType;
import com.agropulse.pattern.observer.GreenhouseEvent;

/**
 * ESTRATEGIA: Control de humedad del suelo.
 * Si la humedad del suelo baja del mínimo → Enciende bomba de agua.
 * Si la humedad vuelve al rango → Apaga bomba de agua.
 */
public class HumidityStrategy implements ActuatorStrategy {

    @Override
    public boolean evaluate(GreenhouseEvent event, Actuator actuator) {
        if (event.getReading().getSensorType() != SensorType.SOIL_MOISTURE) {
            return false;
        }

        if (!actuator.isAutoMode()) {
            return false;
        }

        boolean changed = false;

        if (event.isBelowMin() && !actuator.isEnabled()) {
            actuator.setEnabled(true);
            changed = true;
            System.out.println("    [Strategy] " + getStrategyName() +
                    " → Activando " + actuator.getName());
        } else if (!event.isBelowMin() && actuator.isEnabled()) {
            actuator.setEnabled(false);
            changed = true;
            System.out.println("    [Strategy] " + getStrategyName() +
                    " → Desactivando " + actuator.getName());
        }

        return changed;
    }

    @Override
    public String getStrategyName() {
        return "Control Humedad Suelo (Bomba Agua)";
    }
}
