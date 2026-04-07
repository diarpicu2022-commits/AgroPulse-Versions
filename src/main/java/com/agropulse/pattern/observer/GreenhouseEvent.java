package com.agropulse.pattern.observer;

import com.agropulse.model.SensorReading;

/**
 * Evento que se genera cuando un sensor reporta una lectura.
 * Contiene la lectura y si se excedieron los límites.
 */
public class GreenhouseEvent {
    private final SensorReading reading;
    private final double configuredMin;
    private final double configuredMax;

    public GreenhouseEvent(SensorReading reading, double configuredMin, double configuredMax) {
        this.reading = reading;
        this.configuredMin = configuredMin;
        this.configuredMax = configuredMax;
    }

    public SensorReading getReading() { return reading; }
    public double getConfiguredMin() { return configuredMin; }
    public double getConfiguredMax() { return configuredMax; }

    /**
     * ¿El valor está por debajo del mínimo?
     */
    public boolean isBelowMin() {
        return reading.getValue() < configuredMin;
    }

    /**
     * ¿El valor está por encima del máximo?
     */
    public boolean isAboveMax() {
        return reading.getValue() > configuredMax;
    }

    /**
     * ¿El valor está fuera de rango?
     */
    public boolean isOutOfRange() {
        return isBelowMin() || isAboveMax();
    }

    @Override
    public String toString() {
        String status = isOutOfRange() ? "FUERA DE RANGO" : "Normal";
        return reading.toString() + " [Rango: " +
               configuredMin + " - " + configuredMax + "] → " + status;
    }
}
