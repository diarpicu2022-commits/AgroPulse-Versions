package com.agropulse.pattern.observer;

/**
 * PATRÓN OBSERVER - Interfaz del observador.
 * Cualquier componente que necesite reaccionar a eventos del invernadero
 * implementa esta interfaz: alertas, actuadores, logs, WhatsApp, etc.
 */
public interface GreenhouseObserver {

    /**
     * Llamado cuando ocurre un evento en el invernadero.
     * @param event Datos del evento (lectura del sensor + rangos).
     */
    void onEvent(GreenhouseEvent event);

    /**
     * Nombre descriptivo del observador (para logs).
     */
    String getObserverName();
}
