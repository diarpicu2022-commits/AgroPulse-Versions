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
 * Implementación del servicio de IA usando Google Gemma via Google AI Studio.
 * Modelo: gemma-4-31b-it (última versión de Gemma 4)
 * 
 * API Key se obtiene desde https://aistudio.google.com/app/apikey
 */
public class GemmaService implements AIService {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    private static final String MODEL   = "gemma-4-31b-it";
    private static final String DEFAULT_API_KEY = "";

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean enabled;

    public GemmaService(String apiKey) {
        this.apiKey  = (apiKey != null && !apiKey.isBlank()) ? apiKey : DEFAULT_API_KEY;
        this.enabled = !this.apiKey.isBlank();
        this.gson    = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)  // Gemma puede tardar más
                .build();
    }

    @Override
    public String getCropRecommendation(Crop crop, List<SensorReading> readings) {
        if (!enabled) return "[Gemma desactivado] Configure su API key.";
        return callAPI(buildCropPrompt(crop, readings));
    }

    @Override
    public String predictActuatorNeeds(List<SensorReading> readings) {
        if (!enabled) return "[Gemma desactivado] Configure su API key.";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres experto en invernaderos. Basándote en las lecturas actuales:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResponde: ¿Qué actuadores deberían activarse en las próximas horas?");
        sb.append("\nSé conciso y práctico en español.");
        return callAPI(sb.toString());
    }

    @Override
    public String analyzeGreenhouseStatus(List<SensorReading> readings) {
        if (!enabled) return "[Gemma desactivado] Configure su API key.";
        StringBuilder sb = new StringBuilder();
        sb.append("Analiza el estado del invernadero:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nProporciona: Estado general, alertas y recomendaciones específicas.");
        return callAPI(sb.toString());
    }

    @Override
    public boolean isAvailable() {
        if (!enabled || apiKey.isBlank()) return false;
        // Test de conexión simple
        try {
            JsonObject test = new JsonObject();
            test.addProperty("model", MODEL);
            
            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", "test");
            messages.add(msg);
            test.add("messages", messages);
            test.addProperty("max_tokens", 10);

            RequestBody body = RequestBody.create(
                test.toString(), 
                MediaType.parse("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                .url(API_URL + "?key=" + apiKey)
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (IOException e) {
            System.err.println("[Gemma] Error verificando: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Gemma 4 (Google AI Studio)";
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String callAPI(String prompt) {
        if (!enabled) return "[Gemma] Servicio desactivado.";

        JsonObject json = new JsonObject();
        json.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        messages.add(msg);
        
        json.add("messages", messages);
        json.addProperty("max_tokens", 600);
        json.addProperty("temperature", 0.7);

        RequestBody body = RequestBody.create(
            json.toString(), 
            MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(API_URL + "?key=" + apiKey)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "[Error Gemma] Código: " + response.code() + " - " + response.message();
            }
            
            String bodyStr = response.body().string();
            JsonObject result = gson.fromJson(bodyStr, JsonObject.class);
            
            if (result.has("choices") && result.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = result.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (choice.has("message")) {
                    JsonObject aiMsg = choice.getAsJsonObject().get("message").getAsJsonObject();
                    if (aiMsg.has("content")) {
                        return aiMsg.get("content").getAsString();
                    }
                }
            }
            
            if (result.has("error")) {
                JsonObject error = result.getAsJsonObject("error").getAsJsonObject();
                return "[Error Gemma] " + error.get("message").getAsString();
            }
            
            return "[Gemma] Respuesta sin formato: " + bodyStr;
            
        } catch (IOException e) {
            return "[Error Gemma] Conexión: " + e.getMessage();
        }
    }

    private String buildCropPrompt(Crop crop, List<SensorReading> readings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un agrónomo experto. Analiza las condiciones del cultivo:\n\n");
        sb.append("Cultivo: ").append(crop.getName()).append(" - ").append(crop.getVariety()).append("\n");
        sb.append("Requisitos: Temp ").append(crop.getTempMin()).append("-").append(crop.getTempMax())
              .append("°C, Humedad ").append(crop.getHumidityMin()).append("-").append(crop.getHumidityMax()).append("%\n\n");
        sb.append("Lecturas actuales:\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nProporciona recomendaciones específicas para optimizar el rendimiento.");
        return sb.toString();
    }
}