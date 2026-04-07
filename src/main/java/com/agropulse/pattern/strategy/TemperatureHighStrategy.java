package com.agropulse.pattern.strategy;

import com.agropulse.model.Actuator;
import com.agropulse.model.enums.SensorType;
import com.agropulse.pattern.observer.GreenhouseEvent;

/**
 * ESTRATEGIA: Temperatura alta.
 * Si la temperatura interna supera el máximo → Enciende extractor y abre puerta.
 * Si la temperatura vuelve al rango → Apaga extractor y cierra puerta.
 */
public class TemperatureHighStrategy implements ActuatorStrategy {

    @Override
    public boolean evaluate(GreenhouseEvent event, Actuator actuator) {
        if (event.getReading().getSensorType() != SensorType.TEMPERATURE_INTERNAL) {
            return false; // Solo reacciona a temperatura interna
        }

        if (!actuator.isAutoMode()) {
            return false; // No tocar actuadores en modo manual
        }

        boolean changed = false;

        if (event.isAboveMax() && !actuator.isEnabled()) {
            actuator.setEnabled(true);
            changed = true;
            System.out.println("    [Strategy] " + getStrategyName() +
                    " → Activando " + actuator.getName());
        } else if (!event.isAboveMax() && !event.isBelowMin() && actuator.isEnabled()) {
            actuator.setEnabled(false);
            changed = true;
            System.out.println("    [Strategy] " + getStrategyName() +
                    " → Desactivando " + actuator.getName());
        }

        return changed;
    }

    @Override
    public String getStrategyName() {
        return "Control Temperatura Alta (Extractor/Puerta)";
    }
}
