package com.agropulse.pattern.adapter;

import com.agropulse.service.whatsapp.GreenAPIWhatsAppService;

public class GreenAPIAdapter implements ExternalServiceAdapter {
    private GreenAPIWhatsAppService service;
    
    public GreenAPIAdapter(GreenAPIWhatsAppService service) {
        this.service = service;
    }
    
    @Override
    public String sendMessage(String to, String message) {
        boolean success = service.sendMessage(to, message);
        return success ? "Mensaje enviado" : "Error al enviar";
    }
    
    @Override
    public String getStatus() {
        return service.isAvailable() ? "Activo" : "Inactivo";
    }
    
    @Override
    public boolean isAvailable() {
        return service.isAvailable();
    }
}
