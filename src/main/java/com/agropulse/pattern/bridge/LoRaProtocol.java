package com.agropulse.pattern.bridge;

public class LoRaProtocol implements SensorProtocol {
    @Override
    public String transmitData(String data) {
        return "[LoRa] " + data;
    }
    
    @Override
    public boolean isConnected() {
        return true;
    }
    
    @Override
    public String getProtocolName() {
        return "LoRa";
    }
}
