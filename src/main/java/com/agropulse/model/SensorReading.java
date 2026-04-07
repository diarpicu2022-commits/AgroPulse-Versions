package com.agropulse.model;

import com.agropulse.model.enums.SensorType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lectura individual de un sensor.
 * Se almacena en la base de datos para gráficas e historial.
 */
public class SensorReading {
    private int id;
    private int sensorId;
    private String sensorName;    // Nombre del sensor (join opcional)
    private SensorType sensorType;
    private double value;
    private LocalDateTime timestamp;
    private String source = "MANUAL"; // MANUAL|ESP32_SERIAL|ESP32_LORA|ESP32_WIFI|ESP32_FILE

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // --- Constructores ---

    public SensorReading() {
        this.timestamp = LocalDateTime.now();
    }

    public SensorReading(int sensorId, SensorType sensorType, double value) {
        this();
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.value = value;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSensorId() { return sensorId; }
    public void setSensorId(int sensorId) { this.sensorId = sensorId; }

    public String getSensorName() { return sensorName; }
    public void setSensorName(String sensorName) { this.sensorName = sensorName; }

    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

        /** Unidad de la lectura, delegada al tipo de sensor. */
    public String getUnit() {
        return sensorType != null ? sensorType.getUnit() : "";
    }

    @Override
    public String toString() {
        return "[" + timestamp.format(FMT) + "] " +
               sensorType.getDisplayName() + ": " +
               String.format("%.2f", value) + " " + sensorType.getUnit();
    }
}
