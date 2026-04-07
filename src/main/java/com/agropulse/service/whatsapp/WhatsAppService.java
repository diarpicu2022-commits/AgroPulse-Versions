package com.agropulse.service.whatsapp;

import com.agropulse.model.Alert;

/**
 * Interfaz del servicio de WhatsApp.
 * Permite enviar alertas y mensajes por WhatsApp.
 * Se puede implementar con Green API, Twilio, Meta Cloud API, etc.
 */
public interface WhatsAppService {

    /**
     * Enviar un mensaje de texto a un número de WhatsApp.
     * @param phone   Número en formato internacional (ej: 573001234567).
     * @param message Texto del mensaje.
     * @return true si se envió correctamente.
     */
    boolean sendMessage(String phone, String message);

    /**
     * Enviar una alerta formateada por WhatsApp.
     * @param phone Número destino.
     * @param alert Alerta a enviar.
     * @return true si se envió correctamente.
     */
    boolean sendAlert(String phone, Alert alert);

    /**
     * Verificar si el servicio está disponible.
     */
    boolean isAvailable();

    /**
     * Nombre del proveedor.
     */
    String getProviderName();
}
