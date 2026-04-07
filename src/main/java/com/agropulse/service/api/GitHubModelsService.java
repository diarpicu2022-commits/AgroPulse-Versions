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

public class GitHubModelsService implements AIService {

    private static final String API_URL = "https://models.github.ai/inference/chat/completions";
    private static final String MODEL = "openai/gpt-4o-mini";

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean enabled;

    public GitHubModelsService(String apiKey) {
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey : "";
        this.enabled = !this.apiKey.isBlank();
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getCropRecommendation(Crop crop, List<SensorReading> readings) {
        if (!enabled) return "[GitHub Models desactivado] Configure su PAT.";
        return callAPI(buildCropPrompt(crop, readings));
    }

    @Override
    public String predictActuatorNeeds(List<SensorReading> readings) {
        if (!enabled) return "[GitHub Models desactivado] Configure su PAT.";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres experto en invernaderos. Predice actuadores necesarios:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResponde en español, conciso.");
        return callAPI(sb.toString());
    }

    @Override
    public String analyzeGreenhouseStatus(List<SensorReading> readings) {
        if (!enabled) return "[GitHub Models desactivado] Configure su PAT.";
        StringBuilder sb = new StringBuilder();
        sb.append("Analiza el estado del invernadero:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResumen y recomendaciones en español.");
        return callAPI(sb.toString());
    }

    @Override
    public boolean isAvailable() { return enabled; }

    @Override
    public String getProviderName() { return "GitHub Models (gpt-4o-mini)"; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled && !apiKey.isBlank();
    }

    private String buildCropPrompt(Crop crop, List<SensorReading> readings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres experto agrónomo. Cultivo: '").append(crop.getName())
          .append(" (").append(crop.getVariety()).append(")'.\n");
        sb.append("Temperatura óptima: ").append(crop.getTempMin())
          .append("–").append(crop.getTempMax()).append("°C\n");
        sb.append("Humedad óptima: ").append(crop.getHumidityMin())
          .append("–").append(crop.getHumidityMax()).append("%\n\n");
        sb.append("Lecturas actuales:\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nRecomendación concisa en español.");
        return sb.toString();
    }

    public String callAPI(String prompt) {
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
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
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
                    return "[Error GitHub] Código: " + response.code();
                }
            }
        } catch (IOException e) {
            return "[Error GitHub] Conexión: " + e.getMessage();
        }
    }
}