package com.agropulse.pattern.strategy;

import com.agropulse.model.Actuator;
import com.agropulse.model.enums.SensorType;
import com.agropulse.pattern.observer.GreenhouseEvent;

/**
 * ESTRATEGIA: Temperatura baja.
 * Si la temperatura interna baja del mínimo → Enciende generador de calor.
 * Si la temperatura vuelve al rango → Apaga generador de calor.
 */
public class TemperatureLowStrategy implements ActuatorStrategy {

    @Override
    public boolean evaluate(GreenhouseEvent event, Actuator actuator) {
        if (event.getReading().getSensorType() != SensorType.TEMPERATURE_INTERNAL) {
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
        } else if (!event.isBelowMin() && !event.isAboveMax() && actuator.isEnabled()) {
            actuator.setEnabled(false);
            changed = true;
            System.out.println("    [Strategy] " + getStrategyName() +
                    " → Desactivando " + actuator.getName());
        }

        return changed;
    }

    @Override
    public String getStrategyName() {
        return "Control Temperatura Baja (Generador Calor)";
    }
}
