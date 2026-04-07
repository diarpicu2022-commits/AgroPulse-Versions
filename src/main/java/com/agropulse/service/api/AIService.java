package com.agropulse.service.api;

import com.agropulse.model.Crop;
import com.agropulse.model.SensorReading;

import java.util.List;

/**
 * Interfaz del servicio de IA.
 * Permite conectar cualquier API de IA (OpenAI, Gemini, Claude, etc.)
 * simplemente implementando esta interfaz.
 *
 * PATRÓN STRATEGY aplicado: se puede cambiar la implementación de IA
 * sin afectar el resto del sistema.
 */
public interface AIService {

    /**
     * Obtener pronóstico/recomendación para un cultivo basado en lecturas.
     * @param crop     Cultivo actual.
     * @param readings Últimas lecturas de los sensores.
     * @return Texto con el pronóstico o recomendación.
     */
    String getCropRecommendation(Crop crop, List<SensorReading> readings);

    /**
     * Predecir si se necesita activar algún actuador.
     * @param readings Últimas lecturas de los sensores.
     * @return Texto con la predicción.
     */
    String predictActuatorNeeds(List<SensorReading> readings);

    /**
     * Analizar el estado general del invernadero.
     * @param readings Últimas lecturas de los sensores.
     * @return Resumen del estado y sugerencias.
     */
    String analyzeGreenhouseStatus(List<SensorReading> readings);

    /**
     * Verificar si la API está disponible.
     */
    boolean isAvailable();

    /**
     * Nombre del proveedor de IA.
     */
    String getProviderName();
}
