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
 * Implementación del servicio de IA usando OpenRouter.
 *
 * ✅ VENTAJAS sobre Gemini directo:
 *   - NO requiere tarjeta de crédito ni billing de Google Cloud
 *   - Modelos gratuitos disponibles las 24h (200 req/día gratis)
 *   - API 100% compatible con OpenAI (mismo formato JSON)
 *   - Acceso a Gemini, LLaMA, Mistral y más desde una sola key
 *   - Fallback automático si un modelo falla
 *
 * MODELOS GRATUITOS disponibles (todos con :free):
 *   google/gemini-exp-1121:free         → Gemini Experimental de Google
 *   anthropic/claude-3-haiku:free      → Claude Haiku gratis
 *   meta-llama/llama-3.3-70b-instruct:free
 *   mistralai/mistral-small-3.1-24b-instruct:free
 *   qwen/qwen3-8b:free
 *
 * CÓMO OBTENER KEY GRATIS:
 *   1. Ir a https://openrouter.ai
 *   2. Sign up (sin tarjeta)
 *   3. Keys → Create Key
 *   4. Pegar aquí o en el panel "🔌 Configurar APIs"
 */
public class OpenRouterService implements AIService {

    private static final String API_URL  = "https://openrouter.ai/api/v1/chat/completions";
    // Modelo gratuito por defecto — Gemini 2.0 Flash experimental (gratis)
    private static final String MODEL    = "google/gemini-exp-1121:free";
    // Fallback si el modelo principal está saturado
    private static final String FALLBACK = "meta-llama/llama-3.3-70b-instruct:free";

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean enabled;

    public OpenRouterService(String apiKey) {
        this.apiKey  = (apiKey != null && !apiKey.isBlank()) ? apiKey : "";
        this.enabled = !this.apiKey.isBlank();
        this.gson    = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getCropRecommendation(Crop crop, List<SensorReading> readings) {
        if (!enabled) return "[OpenRouter desactivado] Configura tu API key en openrouter.ai (gratis).";
        return callAPI(buildCropPrompt(crop, readings), MODEL);
    }

    @Override
    public String predictActuatorNeeds(List<SensorReading> readings) {
        if (!enabled) return "[OpenRouter desactivado] Configura tu API key en openrouter.ai (gratis).";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto en invernaderos. Basándote en estas lecturas, ");
        sb.append("predice qué actuadores se necesitarán activar en las próximas horas:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResponde en español, de forma concisa.");
        return callAPI(sb.toString(), MODEL);
    }

    @Override
    public String analyzeGreenhouseStatus(List<SensorReading> readings) {
        if (!enabled) return "[OpenRouter desactivado] Configura tu API key en openrouter.ai (gratis).";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto en agronomía. Analiza el estado del invernadero:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nDa un resumen, identifica problemas y sugiere acciones. Español, conciso.");
        return callAPI(sb.toString(), MODEL);
    }

    @Override public boolean isAvailable() { return enabled; }
    @Override public String getProviderName() { return "OpenRouter (Gemini 2.0 Flash — Gratis)"; }
    public void setEnabled(boolean v) { this.enabled = v && !apiKey.isBlank(); }

    // ─── Privados ─────────────────────────────────────────────────────

    private String buildCropPrompt(Crop crop, List<SensorReading> readings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un experto agrónomo. Cultivo: '")
          .append(crop.getName()).append(" (").append(crop.getVariety()).append(")'.\n");
        sb.append("Temperatura óptima: ").append(crop.getTempMin()).append("–").append(crop.getTempMax()).append("°C\n");
        sb.append("Humedad óptima: ").append(crop.getHumidityMin()).append("–").append(crop.getHumidityMax()).append("%\n\n");
        sb.append("Lecturas actuales:\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nRecomendación concisa en español.");
        return sb.toString();
    }

    private String callAPI(String prompt, String model) {
        try {
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.add("messages", messages);
            body.addProperty("max_tokens", 600);
            body.addProperty("temperature", 0.7);

            // Headers requeridos por OpenRouter
            RequestBody rb = RequestBody.create(gson.toJson(body), MediaType.parse("application/json"));
            Request req = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "https://agropulse.app")
                    .header("X-Title", "AgroPulse Invernadero")
                    .post(rb)
                    .build();

            try (Response response = httpClient.newCall(req).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    return json.getAsJsonArray("choices")
                               .get(0).getAsJsonObject()
                               .getAsJsonObject("message")
                               .get("content").getAsString().trim();
                }

                // Si el modelo gratuito está saturado (429), intentar fallback
                if (response.code() == 429 && !model.equals(FALLBACK)) {
                    System.out.println("  [OpenRouter] Modelo primario saturado, usando fallback: " + FALLBACK);
                    return callAPI(prompt, FALLBACK);
                }

                return "[Error OpenRouter] Código " + response.code() + ": " + responseBody;
            }
        } catch (IOException e) {
            return "[Error OpenRouter] Conexión fallida: " + e.getMessage();
        }
    }
}
