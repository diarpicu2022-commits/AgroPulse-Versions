package com.agropulse.pattern.factory;

import com.agropulse.model.Actuator;
import com.agropulse.model.Sensor;
import com.agropulse.model.enums.ActuatorType;
import com.agropulse.model.enums.SensorType;

/**
 * PATRÓN FACTORY - Crea sensores y actuadores preconfigurados.
 * Centraliza la creación de objetos del hardware del invernadero.
 */
public class SensorFactory {

    /**
     * Crear un sensor con configuración por defecto según su tipo.
     */
    public static Sensor createSensor(SensorType type) {
        switch (type) {
            case TEMPERATURE_INTERNAL:
                return new Sensor("Sensor Temp. Interna", type, "Interno - Centro");
            case TEMPERATURE_EXTERNAL:
                return new Sensor("Sensor Temp. Externa", type, "Externo - Techo");
            case HUMIDITY:
                return new Sensor("Sensor Humedad Rel.", type, "Interno - Lateral");
            case SOIL_MOISTURE:
                return new Sensor("Sensor Humedad Suelo", type, "Interno - Suelo");
            default:
                throw new IllegalArgumentException("Tipo de sensor no soportado: " + type);
        }
    }

    /**
     * Crear un actuador con configuración por defecto según su tipo.
     */
    public static Actuator createActuator(ActuatorType type) {
        switch (type) {
            case EXTRACTOR:
                return new Actuator("Extractor Principal", type);
            case HEAT_GENERATOR:
                return new Actuator("Generador de Calor", type);
            case DOOR:
                return new Actuator("Puerta Invernadero", type);
            case WATER_PUMP:
                return new Actuator("Bomba de Agua", type);
            default:
                throw new IllegalArgumentException("Tipo de actuador no soportado: " + type);
        }
    }
}
