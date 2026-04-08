package com.agropulse.pattern.bridge;

public class WiFiProtocol implements SensorProtocol {
    @Override
    public String transmitData(String data) {
        return "[WiFi] " + data;
    }
    
    @Override
    public boolean isConnected() {
        return true;
    }
    
    @Override
    public String getProtocolName() {
        return "WiFi";
    }
}
