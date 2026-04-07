package com.agropulse.pattern.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN OBSERVER - Sujeto observable.
 * Gestiona la lista de observadores y notifica eventos.
 * Los sensores publican eventos aquí, y los observadores reaccionan.
 */
public class GreenhouseSubject {
    private final List<GreenhouseObserver> observers = new ArrayList<>();

    /**
     * Registrar un observador.
     */
    public void addObserver(GreenhouseObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("  [Observer] Registrado: " + observer.getObserverName());
        }
    }

    /**
     * Eliminar un observador.
     */
    public void removeObserver(GreenhouseObserver observer) {
        observers.remove(observer);
        System.out.println("  [Observer] Eliminado: " + observer.getObserverName());
    }

    /**
     * Notificar a todos los observadores de un nuevo evento.
     */
    public void notifyObservers(GreenhouseEvent event) {
        for (GreenhouseObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                System.err.println("  [Observer] Error en " +
                        observer.getObserverName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Obtener la lista de observadores registrados.
     */
    public List<GreenhouseObserver> getObservers() {
        return new ArrayList<>(observers);
    }

    public int getObserverCount() {
        return observers.size();
    }
}
