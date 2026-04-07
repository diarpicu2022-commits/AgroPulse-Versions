package com.agropulse.service.whatsapp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.agropulse.model.Alert;

import okhttp3.*;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del servicio de WhatsApp usando Green API.
 *
 * Para usar:
 * 1. Crear cuenta en https://green-api.com
 * 2. Obtener idInstance y apiTokenInstance desde la consola
 * 3. Configurar los valores en config/AppConfig
 *
 * También se puede reemplazar por Twilio, Meta Cloud API, etc.
 * creando otra clase que implemente WhatsAppService.
 */
public class GreenAPIWhatsAppService implements WhatsAppService {

    // ═══════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE GREEN API - REEMPLAZAR CON TUS DATOS
    // ═══════════════════════════════════════════════════════════
    private String apiUrlBase;      // Ej: "https://7107.api.greenapi.com"
    private String idInstance;      // Ej: "7107555413"
    private String apiTokenInstance; // Ej: "tu-token-aqui"
    // ═══════════════════════════════════════════════════════════

    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean enabled;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public GreenAPIWhatsAppService(String apiUrlBase, String idInstance, String apiTokenInstance) {
        this.apiUrlBase = apiUrlBase;
        this.idInstance = idInstance;
        this.apiTokenInstance = apiTokenInstance;
        this.enabled = isConfigured();
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public boolean sendMessage(String phone, String message) {
        if (!enabled) {
            System.out.println("  [WhatsApp] Servicio desactivado. Configure Green API.");
            return false;
        }

        String url = apiUrlBase + "/waInstance" + idInstance +
                     "/sendMessage/" + apiTokenInstance;

        // Formato del número: sin + ni espacios (ej: 573001234567)
        String cleanPhone = phone.replaceAll("[^0-9]", "");

        JsonObject body = new JsonObject();
        body.addProperty("chatId", cleanPhone + "@c.us");
        body.addProperty("message", message);

        RequestBody requestBody = RequestBody.create(
                gson.toJson(body),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                System.out.println("  [WhatsApp] Mensaje enviado a " + cleanPhone);
                return true;
            } else {
                System.err.println("  [WhatsApp] Error " + response.code() +
                        ": " + (response.body() != null ? response.body().string() : ""));
                return false;
            }
        } catch (IOException e) {
            System.err.println("  [WhatsApp] Error de conexión: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendAlert(String phone, Alert alert) {
        String emoji;
        switch (alert.getLevel()) {
            case CRITICAL: emoji = "🚨"; break;
            case WARNING:  emoji = "⚠️"; break;
            default:       emoji = "ℹ️"; break;
        }

        String message = emoji + " *ALERTA AgroPulse* " + emoji + "\n\n" +
                "*Nivel:* " + alert.getLevel().getDisplayName() + "\n" +
                "*Fecha:* " + alert.getCreatedAt().format(FMT) + "\n" +
                "*Mensaje:* " + alert.getMessage() + "\n\n" +
                "— Sistema AgroPulse 🌱";

        return sendMessage(phone, message);
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public String getProviderName() {
        return "Green API (WhatsApp)";
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled && isConfigured();
    }

    public void configure(String apiUrlBase, String idInstance, String apiTokenInstance) {
        this.apiUrlBase = apiUrlBase;
        this.idInstance = idInstance;
        this.apiTokenInstance = apiTokenInstance;
        this.enabled = isConfigured();
    }

    private boolean isConfigured() {
        return apiUrlBase != null && !apiUrlBase.isEmpty() &&
               idInstance != null && !idInstance.isEmpty() &&
               apiTokenInstance != null && !apiTokenInstance.isEmpty();
    }
}
