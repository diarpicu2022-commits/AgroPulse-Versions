package com.agropulse.service.esp32;

import com.agropulse.model.SensorReading;
import com.agropulse.model.enums.SensorType;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Servicio ESP32 — Canal Archivo/Memoria.
 *
 * Carga datos desde archivos CSV o JSON generados por el ESP32
 * cuando no hay conexión en tiempo real (memoria SD, USB, pendrive).
 *
 * FORMATOS SOPORTADOS:
 *
 * CSV (una lectura por fila):
 *   timestamp,temp_in,temp_out,humidity,soil
 *   2024-01-15T10:30:00,24.5,19.2,65.0,42.0
 *   2024-01-15T10:35:00,25.1,20.0,63.5,41.2
 *
 * JSON array:
 *   [
 *     {"timestamp":"2024-01-15T10:30:00","temp_in":24.5,"humidity":65.0},
 *     {"timestamp":"2024-01-15T10:35:00","temp_in":25.1,"humidity":63.5}
 *   ]
 *
 * JSON por línea (JSONL):
 *   {"timestamp":"...","temp_in":24.5,"humidity":65.0}
 *   {"timestamp":"...","temp_in":25.1,"humidity":63.5}
 */
public class ESP32FileService implements ESP32DataSource {

    private final String filePath;
    private final Gson   gson = new Gson();
    private List<SensorReading> loadedData = new ArrayList<>();
    private boolean loaded = false;

    public ESP32FileService(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public boolean connect() {
        File f = new File(filePath);
        if (!f.exists() || !f.canRead()) {
            System.err.println("  [ESP32-File] Archivo no encontrado: " + filePath);
            return false;
        }
        loaded = true;
        System.out.println("  [ESP32-File] Archivo listo: " + filePath);
        return true;
    }

    @Override
    public void disconnect() {
        loadedData.clear();
        loaded = false;
    }

    @Override
    public boolean isConnected() { return loaded; }

    @Override
    public List<SensorReading> readData() {
        if (!loaded) return Collections.emptyList();
        try {
            String lower = filePath.toLowerCase();
            if (lower.endsWith(".csv")) {
                loadedData = parseCsvFile(filePath);
            } else if (lower.endsWith(".json") || lower.endsWith(".jsonl")) {
                loadedData = parseJsonFile(filePath);
            } else {
                // Intentar auto-detectar por contenido
                String firstLine = Files.lines(Path.of(filePath)).findFirst().orElse("");
                loadedData = firstLine.trim().startsWith("{") || firstLine.trim().startsWith("[")
                        ? parseJsonFile(filePath)
                        : parseCsvFile(filePath);
            }
            System.out.println("  [ESP32-File] Cargadas " + loadedData.size() + " lecturas de " + filePath);
        } catch (IOException e) {
            System.err.println("  [ESP32-File] Error leyendo: " + e.getMessage());
        }
        return new ArrayList<>(loadedData);
    }

    @Override public String getSourceName() {
        return "ESP32 Archivo (" + Path.of(filePath).getFileName() + ")";
    }
    @Override public ConnectionType getType() { return ConnectionType.FILE; }
    public String getFilePath() { return filePath; }

    // ─── Parsers ──────────────────────────────────────────────────────

    private List<SensorReading> parseCsvFile(String path) throws IOException {
        List<SensorReading> list = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
        if (lines.isEmpty()) return list;

        // Encabezado
        String[] headers = lines.get(0).split(",");
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            col.put(headers[i].trim().toLowerCase(), i);
        }

        for (int row = 1; row < lines.size(); row++) {
            String[] vals = lines.get(row).split(",");
            if (vals.length < 2) continue;

            addCsvReading(list, vals, col, "temp_in",   SensorType.TEMPERATURE_INTERNAL);
            addCsvReading(list, vals, col, "temp",      SensorType.TEMPERATURE_INTERNAL);
            addCsvReading(list, vals, col, "temp_out",  SensorType.TEMPERATURE_EXTERNAL);
            addCsvReading(list, vals, col, "humidity",  SensorType.HUMIDITY);
            addCsvReading(list, vals, col, "hum",       SensorType.HUMIDITY);
            addCsvReading(list, vals, col, "soil",      SensorType.SOIL_MOISTURE);
            addCsvReading(list, vals, col, "moisture",  SensorType.SOIL_MOISTURE);
        }
        return list;
    }

    private void addCsvReading(List<SensorReading> list, String[] vals,
                               Map<String, Integer> col, String key, SensorType type) {
        Integer idx = col.get(key);
        if (idx == null || idx >= vals.length) return;
        try {
            double v = Double.parseDouble(vals[idx].trim());
            list.add(new SensorReading(0, type, v));
        } catch (NumberFormatException ignored) {}
    }

    private List<SensorReading> parseJsonFile(String path) throws IOException {
        List<SensorReading> list = new ArrayList<>();
        String content = Files.readString(Path.of(path), StandardCharsets.UTF_8).trim();

        if (content.startsWith("[")) {
            // JSON array
            JsonArray arr = gson.fromJson(content, JsonArray.class);
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    list.addAll(ESP32SerialService.parseLine(el.toString(), "ESP32_FILE"));
                }
            }
        } else {
            // JSONL (una línea por objeto) o JSON único
            for (String line : content.split("\n")) {
                line = line.trim();
                if (!line.isEmpty()) {
                    list.addAll(ESP32SerialService.parseLine(line, "ESP32_FILE"));
                }
            }
        }
        return list;
    }
}
