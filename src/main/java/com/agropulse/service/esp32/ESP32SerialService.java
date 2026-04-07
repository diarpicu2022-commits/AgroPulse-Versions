package com.agropulse.service.esp32;

import com.agropulse.model.SensorReading;
import com.agropulse.model.enums.SensorType;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Servicio ESP32 — Canal USB/Serial.
 *
 * El ESP32 debe enviar líneas JSON por Serial a 115200 baud:
 *
 *   {"temp_in":24.5,"temp_out":19.2,"humidity":65.0,"soil":42.0,"device":"ESP32-001"}
 *
 * También acepta formato simple con comas:
 *   temp_in:24.5,temp_out:19.2,humidity:65.0,soil:42.0
 *
 * DEPENDENCIA REQUERIDA: jSerialComm (añadida en pom.xml)
 *   <dependency>
 *     <groupId>com.fazecast</groupId>
 *     <artifactId>jSerialComm</artifactId>
 *     <version>2.10.4</version>
 *   </dependency>
 *
 * Usa reflexión para no fallar si la lib no está disponible.
 */
public class ESP32SerialService implements ESP32DataSource {

    private final String portName;
    private final int    baudRate;
    private Object       serialPort;   // com.fazecast.jSerialComm.SerialPort (por reflexión)
    private BufferedReader reader;
    private boolean       connected = false;
    private final Gson    gson = new Gson();

    public ESP32SerialService(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;
    }

    public ESP32SerialService(String portName) {
        this(portName, 115200);
    }

    @Override
    public boolean connect() {
        try {
            Class<?> spClass = Class.forName("com.fazecast.jSerialComm.SerialPort");
            serialPort = spClass.getMethod("getCommPort", String.class).invoke(null, portName);
            spClass.getMethod("setBaudRate", int.class).invoke(serialPort, baudRate);
            spClass.getMethod("setComPortTimeouts", int.class, int.class, int.class)
                   .invoke(serialPort, 1 /*TIMEOUT_READ_SEMI_BLOCKING*/, 2000, 0);

            boolean opened = (boolean) spClass.getMethod("openPort").invoke(serialPort);
            if (opened) {
                InputStream is = (InputStream) spClass.getMethod("getInputStream").invoke(serialPort);
                reader    = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                connected = true;
                System.out.println("  [ESP32-Serial] Conectado en " + portName + " @ " + baudRate);
            }
            return opened;
        } catch (ClassNotFoundException e) {
            System.err.println("  [ESP32-Serial] jSerialComm no encontrado. Agrega la dependencia al pom.xml");
            return false;
        } catch (Exception e) {
            System.err.println("  [ESP32-Serial] Error: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (reader != null) reader.close();
            if (serialPort != null) {
                serialPort.getClass().getMethod("closePort").invoke(serialPort);
            }
        } catch (Exception ignored) {}
        connected = false;
    }

    @Override
    public boolean isConnected() { return connected; }

    @Override
    public List<SensorReading> readData() {
        List<SensorReading> readings = new ArrayList<>();
        if (!connected || reader == null) return readings;
        try {
            // Leer hasta 10 líneas disponibles (sin bloqueo)
            for (int i = 0; i < 10 && reader.ready(); i++) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    List<SensorReading> parsed = parseLine(line.trim(), "ESP32_SERIAL");
                    readings.addAll(parsed);
                }
            }
        } catch (IOException e) {
            System.err.println("  [ESP32-Serial] Error lectura: " + e.getMessage());
            connected = false;
        }
        return readings;
    }

    @Override public String getSourceName() { return "ESP32 Serial (" + portName + ")"; }
    @Override public ConnectionType getType() { return ConnectionType.SERIAL; }
    public String getPortName() { return portName; }

    // ─── Parser JSON/CSV ──────────────────────────────────────────────

    static List<SensorReading> parseLine(String line, String source) {
        List<SensorReading> list = new ArrayList<>();
        try {
            if (line.startsWith("{")) {
                parseJson(line, list, source);
            } else {
                parseCsv(line, list, source);
            }
        } catch (Exception e) {
            System.err.println("  [ESP32] Error parseando: " + line + " → " + e.getMessage());
        }
        return list;
    }

    private static void parseJson(String json, List<SensorReading> out, String source) {
        Gson g = new Gson();
        JsonObject obj = g.fromJson(json, JsonObject.class);
        String device = obj.has("device") ? obj.get("device").getAsString() : "ESP32";

        addIfPresent(out, obj, "temp_in",   SensorType.TEMPERATURE_INTERNAL, source);
        addIfPresent(out, obj, "temp_out",  SensorType.TEMPERATURE_EXTERNAL, source);
        addIfPresent(out, obj, "temp",      SensorType.TEMPERATURE_INTERNAL, source);
        addIfPresent(out, obj, "humidity",  SensorType.HUMIDITY,             source);
        addIfPresent(out, obj, "hum",       SensorType.HUMIDITY,             source);
        addIfPresent(out, obj, "soil",      SensorType.SOIL_MOISTURE,        source);
        addIfPresent(out, obj, "moisture",  SensorType.SOIL_MOISTURE,        source);
    }

    private static void addIfPresent(List<SensorReading> out, JsonObject obj,
                                     String key, SensorType type, String source) {
        if (obj.has(key)) {
            double val = obj.get(key).getAsDouble();
            SensorReading r = new SensorReading(0, type, val);
            out.add(r);
        }
    }

    private static void parseCsv(String line, List<SensorReading> out, String source) {
        // Formato: temp_in:24.5,humidity:65.0,soil:42.0
        for (String part : line.split(",")) {
            String[] kv = part.trim().split(":");
            if (kv.length != 2) continue;
            String key = kv[0].trim().toLowerCase();
            double val;
            try { val = Double.parseDouble(kv[1].trim()); }
            catch (NumberFormatException e) { continue; }

            SensorType type = switch (key) {
                case "temp_in","temperature_internal","temp" -> SensorType.TEMPERATURE_INTERNAL;
                case "temp_out","temperature_external"       -> SensorType.TEMPERATURE_EXTERNAL;
                case "humidity","hum","rh"                   -> SensorType.HUMIDITY;
                case "soil","moisture","soil_moisture"       -> SensorType.SOIL_MOISTURE;
                default -> null;
            };
            if (type != null) out.add(new SensorReading(0, type, val));
        }
    }
}
