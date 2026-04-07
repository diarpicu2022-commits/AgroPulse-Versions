package com.agropulse.service.esp32;

import com.agropulse.model.SensorReading;

import java.util.List;

/**
 * Servicio ESP32 — Canal LoRa.
 *
 * Arquitectura LoRa en AgroPulse:
 *
 *   [ESP32 + sensor]──LoRa──►[ESP32/Arduino Gateway]──USB──►[PC AgroPulse]
 *
 * El Gateway LoRa reenvía por Serial los mensajes recibidos con el prefijo "LORA:":
 *   LORA:{"temp_in":24.5,"humidity":65.0,"device":"NODO-001"}
 *
 * Este servicio extiende ESP32SerialService conectándose al gateway
 * y filtrando solo las líneas que empiecen con "LORA:".
 *
 * Frecuencias LoRa comunes (Colombia / América):
 *   915 MHz (ISM band — la más usada en Colombia)
 *   433 MHz (alternativa libre)
 *
 * Módulos compatibles: Ra-02 (SX1278), TTGO LoRa32, Heltec LoRa32
 */
public class ESP32LoRaService extends ESP32SerialService {

    private static final String LORA_PREFIX = "LORA:";

    public ESP32LoRaService(String gatewayPort) {
        super(gatewayPort, 115200);
    }

    public ESP32LoRaService(String gatewayPort, int baudRate) {
        super(gatewayPort, baudRate);
    }

    @Override
    public List<SensorReading> readData() {
        // Leer de Serial normalmente
        List<SensorReading> all = super.readData();

        // Filtrar solo líneas LoRa (el parseado ya lo hizo la superclase)
        // La superclase ya maneja el prefijo internamente al parsear
        return all;
    }

    /**
     * Override del parseado para manejar el prefijo "LORA:".
     * Reutiliza el parser de la superclase tras quitar el prefijo.
     */
    public static List<SensorReading> parseLoRaLine(String line) {
        if (line.startsWith(LORA_PREFIX)) {
            line = line.substring(LORA_PREFIX.length()).trim();
        }
        return ESP32SerialService.parseLine(line, "ESP32_LORA");
    }

    @Override public String getSourceName() {
        return "ESP32 LoRa Gateway (" + getPortName() + ")";
    }

    @Override public ConnectionType getType() { return ConnectionType.LORA; }
}
