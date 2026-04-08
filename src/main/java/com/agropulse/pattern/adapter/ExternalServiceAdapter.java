package com.agropulse.pattern.adapter;

public interface ExternalServiceAdapter {
    String sendMessage(String to, String message);
    String getStatus();
    boolean isAvailable();
}
