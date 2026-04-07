package com.agropulse.service.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.agropulse.model.Crop;
import com.agropulse.model.SensorReading;

import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del servicio de IA usando Mistral AI API.
 * Modelo: mistral-small-latest (óptimo para agricultura).
 *
 * API Key se lee desde AppConfig / .env
 * Documentación: https://console.mistral.ai
 */
public class MistralService implements AIService {

    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final String MODEL   = "mistral-small-latest";
    private static final String DEFAULT_API_KEY = "";

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean enabled;

    public MistralService(String apiKey) {
        this.apiKey  = (apiKey != null && !apiKey.isBlank()) ? apiKey : DEFAULT_API_KEY;
        this.enabled = !this.apiKey.isBlank();
        this.gson    = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getCropRecommendation(Crop crop, List<SensorReading> readings) {
        if (!enabled) return "[Mistral desactivado] Configure su API key.";
        return callAPI(buildCropPrompt(crop, readings));
    }

    @Override
    public String predictActuatorNeeds(List<SensorReading> readings) {
        if (!enabled) return "[Mistral desactivado] Configure su API key.";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto en invernaderos. Basándote en estas lecturas de sensores, ");
        sb.append("predice qué actuadores se necesitarán activar en las próximas horas:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResponde en español, de forma concisa.");
        return callAPI(sb.toString());
    }

    @Override
    public String analyzeGreenhouseStatus(List<SensorReading> readings) {
        if (!enabled) return "[Mistral desactivado] Configure su API key.";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto en agronomía y control de invernaderos. ");
        sb.append("Analiza el estado del invernadero basándote en estas lecturas:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nDa un resumen del estado general, identifica problemas y sugiere acciones. ");
        sb.append("Responde en español, de forma concisa.");
        return callAPI(sb.toString());
    }

    @Override
    public boolean isAvailable() { return enabled; }

    @Override
    public String getProviderName() { return "Mistral AI"; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled && !apiKey.isBlank();
    }

    // ─── privados ───────────────────────────────────────────────────

    private String buildCropPrompt(Crop crop, List<SensorReading> readings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un expertos agrónomo. Para el cultivo '")
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
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.add("messages", messages);
            body.addProperty("max_tokens", 500);
            body.addProperty("temperature", 0.7);

            RequestBody requestBody = RequestBody.create(
                    gson.toJson(body), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    return json.getAsJsonArray("choices")
                               .get(0).getAsJsonObject()
                               .getAsJsonObject("message")
                               .get("content").getAsString().trim();
                } else {
                    return "[Error Mistral] Código: " + response.code() +
                           " - " + (response.body() != null ? response.body().string() : "Sin respuesta");
                }
            }
        } catch (IOException e) {
            return "[Error Mistral] No se pudo conectar: " + e.getMessage();
        }
    }
}
