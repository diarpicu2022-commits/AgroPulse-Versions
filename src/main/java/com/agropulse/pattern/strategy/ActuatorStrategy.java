package com.agropulse.pattern.strategy;

import com.agropulse.model.Actuator;
import com.agropulse.pattern.observer.GreenhouseEvent;

/**
 * PATRÓN STRATEGY - Interfaz de estrategia para control de actuadores.
 * Cada estrategia define cómo reaccionar ante un evento del invernadero.
 * Permite cambiar la lógica de control sin modificar el código existente.
 */
public interface ActuatorStrategy {

    /**
     * Evaluar si el actuador debe activarse/desactivarse según el evento.
     * @param event   Evento del invernadero con lectura y rangos.
     * @param actuator Actuador a controlar.
     * @return true si se cambió el estado del actuador.
     */
    boolean evaluate(GreenhouseEvent event, Actuator actuator);

    /**
     * Nombre descriptivo de la estrategia.
     */
    String getStrategyName();
}
