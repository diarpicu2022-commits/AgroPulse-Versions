package com.agropulse.service.esp32;

import com.agropulse.model.SensorReading;
import java.util.List;

/**
 * Interfaz para fuentes de datos ESP32.
 * Patrón Strategy: cada canal de comunicación implementa esta interfaz.
 *
 * Canales disponibles:
 *   - ESP32SerialService   → USB/Serial (más fácil, depuración)
 *   - ESP32LoRaService     → LoRa via Serial del módulo LoRa
 *   - ESP32WiFiService     → HTTP Server embebido (LAN o internet)
 *   - ESP32FileService     → Cargar CSV/JSON desde memoria SD o USB
 */
public interface ESP32DataSource {

    /** Conectar/inicializar el canal. */
    boolean connect();

    /** Desconectar el canal limpiamente. */
    void disconnect();

    /** ¿Está conectado y listo? */
    boolean isConnected();

    /** Leer la última tanda de lecturas disponibles. */
    List<SensorReading> readData();

    /** Nombre del canal para la UI. */
    String getSourceName();

    /** Tipo de conexión. */
    ConnectionType getType();

    enum ConnectionType {
        SERIAL("🔌 USB/Serial"),
        LORA("📡 LoRa"),
        WIFI("🌐 WiFi/HTTP"),
        FILE("💾 Archivo/Memoria");

        private final String display;
        ConnectionType(String d) { this.display = d; }
        public String getDisplay() { return display; }
    }
}
