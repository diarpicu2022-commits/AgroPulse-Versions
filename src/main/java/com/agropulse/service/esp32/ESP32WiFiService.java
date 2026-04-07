package com.agropulse.service.esp32;

import com.agropulse.model.SensorReading;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servicio ESP32 — Canal WiFi/HTTP.
 *
 * Levanta un servidor HTTP embebido en el PC que recibe datos del ESP32.
 * El ESP32 hace POST a  http://<IP_PC>:<puerto>/data  con JSON:
 *
 *   POST /data
 *   Content-Type: application/json
 *   {"temp_in":24.5,"temp_out":19.2,"humidity":65.0,"soil":42.0,"device":"ESP32-001"}
 *
 * También acepta GET /status para que el ESP32 verifique conectividad.
 *
 * Código Arduino/ESP32 de ejemplo (src/main/resources/esp32/esp32_wifi_sender.ino):
 *
 *   void sendData() {
 *     HTTPClient http;
 *     http.begin("http://192.168.1.100:8765/data");
 *     http.addHeader("Content-Type", "application/json");
 *     String payload = "{\"temp_in\":" + String(temp) + ",\"humidity\":" + String(hum) + "}";
 *     http.POST(payload);
 *     http.end();
 *   }
 */
public class ESP32WiFiService implements ESP32DataSource {

    private final int  port;
    private HttpServer server;
    private boolean    running = false;
    private final Gson gson    = new Gson();

    // Cola thread-safe para acumular lecturas recibidas
    private final ConcurrentLinkedQueue<SensorReading> queue = new ConcurrentLinkedQueue<>();

    public ESP32WiFiService(int port) {
        this.port = port;
    }

    public ESP32WiFiService() {
        this(8765);
    }

    @Override
    public boolean connect() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 10);

            // Endpoint para recibir datos del ESP32
            server.createContext("/data", this::handleData);

            // Endpoint de health check
            server.createContext("/status", exchange -> {
                String resp = "{\"status\":\"ok\",\"server\":\"AgroPulse\"}";
                sendResponse(exchange, 200, resp);
            });

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            running = true;
            System.out.println("  [ESP32-WiFi] Servidor HTTP iniciado en puerto " + port);
            System.out.println("  [ESP32-WiFi] ESP32 debe enviar POST a: http://<IP_PC>:" + port + "/data");
            return true;
        } catch (IOException e) {
            System.err.println("  [ESP32-WiFi] Error iniciando servidor: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (server != null) {
            server.stop(1);
            running = false;
            System.out.println("  [ESP32-WiFi] Servidor HTTP detenido.");
        }
    }

    @Override
    public boolean isConnected() { return running; }

    @Override
    public List<SensorReading> readData() {
        List<SensorReading> batch = new ArrayList<>();
        SensorReading r;
        while ((r = queue.poll()) != null) batch.add(r);
        return batch;
    }

    @Override public String getSourceName() { return "ESP32 WiFi/HTTP (puerto " + port + ")"; }
    @Override public ConnectionType getType() { return ConnectionType.WIFI; }
    public int getPort() { return port; }

    // ─── HTTP handler ─────────────────────────────────────────────────

    private void handleData(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                List<SensorReading> readings = ESP32SerialService.parseLine(body.trim(), "ESP32_WIFI");
                queue.addAll(readings);
                System.out.println("  [ESP32-WiFi] Recibido: " + body.trim() +
                                   " → " + readings.size() + " lecturas");
                sendResponse(exchange, 200, "{\"received\":" + readings.size() + "}");
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        } else {
            sendResponse(exchange, 405, "{\"error\":\"Use POST\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
