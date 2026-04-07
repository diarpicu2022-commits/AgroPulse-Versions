package com.agropulse.service.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.agropulse.model.Crop;
import com.agropulse.model.SensorReading;

import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del servicio de IA usando Ollama (modelos locales).
 * Ollama corre localmente — no requiere API key ni internet.
 *
 * Instalación en Windows:
 *   irm https://ollama.com/install.ps1 | iex
 *
 * Modelos recomendados para agriculture:
 *   ollama pull llama3          (uso general, 4.7GB)
 *   ollama pull mistral         (eficiente, 3.8GB)
 *   ollama pull phi3            (ligero, 2.3GB)
 *
 * El modelo por defecto es llama3. Si no está disponible, usa phi3.
 * Ollama corre en http://localhost:11434 por defecto.
 */
public class OllamaService implements AIService {

    private static final String DEFAULT_HOST  = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "llama3";   // Cambiar a phi3 si se prefiere uno ligero

    private final String host;
    private final String model;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean enabled;

    public OllamaService(String host, String model) {
        this.host  = (host  != null && !host.isBlank())  ? host  : DEFAULT_HOST;
        this.model = (model != null && !model.isBlank()) ? model : DEFAULT_MODEL;
        this.gson  = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // Modelos locales pueden ser lentos
                .build();
        this.enabled = checkAvailability();
    }

    /** Constructor por defecto — usa localhost y llama3 */
    public OllamaService() {
        this(DEFAULT_HOST, DEFAULT_MODEL);
    }

    @Override
    public String getCropRecommendation(Crop crop, List<SensorReading> readings) {
        if (!enabled) return "[Ollama no disponible] Instala y ejecuta Ollama en localhost:11434";
        return callAPI(buildCropPrompt(crop, readings));
    }

    @Override
    public String predictActuatorNeeds(List<SensorReading> readings) {
        if (!enabled) return "[Ollama no disponible] Instala y ejecuta Ollama en localhost:11434";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto en invernaderos. Basándote en estas lecturas de sensores, ");
        sb.append("predice qué actuadores se necesitarán activar en las próximas horas:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResponde en español, de forma concisa.");
        return callAPI(sb.toString());
    }

    @Override
    public String analyzeGreenhouseStatus(List<SensorReading> readings) {
        if (!enabled) return "[Ollama no disponible] Instala y ejecuta Ollama en localhost:11434";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto en agronomía y control de invernaderos. ");
        sb.append("Analiza el estado del invernadero basándote en estas lecturas:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nDa un resumen del estado general, identifica problemas y sugiere acciones. ");
        sb.append("Responde en español, de forma concisa.");
        return callAPI(sb.toString());
    }

    @Override
    public boolean isAvailable() {
        // Re-verificar en tiempo real por si el servicio se inició después
        this.enabled = checkAvailability();
        return enabled;
    }

    @Override
    public String getProviderName() { return "Ollama (" + model + " local)"; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getHost()  { return host; }
    public String getModel() { return model; }

    // ─── privados ───────────────────────────────────────────────────

    private boolean checkAvailability() {
        try {
            Request request = new Request.Builder()
                    .url(host + "/api/tags")
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String buildCropPrompt(Crop crop, List<SensorReading> readings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto agrónomo. Para el cultivo '")
          .append(crop.getName()).append(" (").append(crop.getVariety()).append(")'):\n");
        sb.append("- Rango temperatura óptimo: ").append(crop.getTempMin())
          .append("°C - ").append(crop.getTempMax()).append("°C\n");
        sb.append("- Rango humedad óptimo: ").append(crop.getHumidityMin())
          .append("% - ").append(crop.getHumidityMax()).append("%\n\n");
        sb.append("Lecturas actuales de los sensores:\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nDa una recomendación concisa en español sobre el cuidado del cultivo.");
        return sb.toString();
    }

    private String callAPI(String prompt) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("prompt", prompt);
            body.addProperty("stream", false);

            // Opciones de generación
            JsonObject options = new JsonObject();
            options.addProperty("num_predict", 500);
            options.addProperty("temperature", 0.7);
            body.add("options", options);

            RequestBody requestBody = RequestBody.create(
                    gson.toJson(body), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(host + "/api/generate")
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    return json.get("response").getAsString().trim();
                } else {
                    return "[Error Ollama] Código: " + response.code() +
                           " - " + (response.body() != null ? response.body().string() : "Sin respuesta");
                }
            }
        } catch (IOException e) {
            return "[Error Ollama] No se pudo conectar a " + host + ": " + e.getMessage() +
                   "\n\nAsegúrate de que Ollama esté ejecutándose:\n  ollama serve";
        }
    }
}
