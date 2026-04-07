package com.agropulse.service.esp32;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.SensorReading;
import com.agropulse.model.enums.SensorType;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Gestor central de conexiones ESP32.
 *
 * - Registra múltiples fuentes de datos (Serial, LoRa, WiFi, Archivo).
 * - Sondea automáticamente cada N segundos en hilo de fondo.
 * - Entrega las lecturas al GreenhouseController para procesarlas.
 * - Emite eventos a listeners de la UI (para actualizar indicadores).
 */
public class ESP32Manager {

    private final GreenhouseController controller;
    private final List<ESP32DataSource> sources = new ArrayList<>();
    private final List<Consumer<List<SensorReading>>> listeners = new ArrayList<>();

    private ScheduledExecutorService scheduler;
    private boolean polling = false;
    private int pollIntervalSeconds = 5;

    public ESP32Manager(GreenhouseController controller) {
        this.controller = controller;
    }

    // ─── Registro de fuentes ──────────────────────────────────────────

    public void addSource(ESP32DataSource source) {
        sources.add(source);
        System.out.println("  [ESP32Manager] Fuente registrada: " + source.getSourceName());
    }

    public void removeSource(ESP32DataSource source) {
        source.disconnect();
        sources.remove(source);
    }

    public List<ESP32DataSource> getSources() {
        return Collections.unmodifiableList(sources);
    }

    /** Listeners de la UI para recibir lecturas en tiempo real. */
    public void addListener(Consumer<List<SensorReading>> listener) {
        listeners.add(listener);
    }

    // ─── Control del sondeo ───────────────────────────────────────────

    /**
     * Inicia el sondeo automático de todas las fuentes activas.
     */
    public void startPolling(int intervalSeconds) {
        if (polling) return;
        this.pollIntervalSeconds = intervalSeconds;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ESP32-Poll");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::pollAll, 0, intervalSeconds, TimeUnit.SECONDS);
        polling = true;
        System.out.println("  [ESP32Manager] Sondeo iniciado cada " + intervalSeconds + "s");
    }

    public void stopPolling() {
        if (scheduler != null) scheduler.shutdownNow();
        polling = false;
        System.out.println("  [ESP32Manager] Sondeo detenido.");
    }

    public boolean isPolling() { return polling; }

    /** Conectar todas las fuentes registradas. */
    public Map<String, Boolean> connectAll() {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (ESP32DataSource src : sources) {
            boolean ok = src.connect();
            results.put(src.getSourceName(), ok);
        }
        return results;
    }

    /** Desconectar todas las fuentes. */
    public void disconnectAll() {
        stopPolling();
        sources.forEach(ESP32DataSource::disconnect);
    }

    // ─── Sondeo ───────────────────────────────────────────────────────

    private void pollAll() {
        List<SensorReading> allReadings = new ArrayList<>();

        for (ESP32DataSource src : sources) {
            if (!src.isConnected()) continue;
            try {
                List<SensorReading> batch = src.readData();
                allReadings.addAll(batch);
            } catch (Exception e) {
                System.err.println("  [ESP32Manager] Error en " + src.getSourceName() + ": " + e.getMessage());
            }
        }

        if (allReadings.isEmpty()) return;

        // Entregar al controlador
        deliverToController(allReadings);

        // Notificar listeners de la UI
        for (Consumer<List<SensorReading>> listener : listeners) {
            try { listener.accept(allReadings); } catch (Exception ignored) {}
        }
    }

    /**
     * Mapea lecturas a índices de sensor del controller y las procesa.
     */
    private void deliverToController(List<SensorReading> readings) {
        List<com.agropulse.model.Sensor> sensors = controller.getSensors();
        Map<SensorType, Integer> typeToIndex = new HashMap<>();
        for (int i = 0; i < sensors.size(); i++) {
            typeToIndex.put(sensors.get(i).getType(), i);
        }

        for (SensorReading r : readings) {
            Integer idx = typeToIndex.get(r.getSensorType());
            if (idx != null) {
                controller.processSensorReading(idx, r.getValue());
            }
        }
    }

    /**
     * Cargar datos de un archivo en un solo disparo (sin sondeo continuo).
     */
    public int loadFromFile(String filePath) {
        ESP32FileService fileSvc = new ESP32FileService(filePath);
        if (!fileSvc.connect()) return 0;
        List<SensorReading> readings = fileSvc.readData();
        fileSvc.disconnect();
        if (!readings.isEmpty()) {
            deliverToController(readings);
            for (Consumer<List<SensorReading>> l : listeners) l.accept(readings);
        }
        return readings.size();
    }
}
