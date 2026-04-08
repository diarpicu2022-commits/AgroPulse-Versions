package com.agropulse.pattern.bridge;

public interface SensorProtocol {
    String transmitData(String data);
    boolean isConnected();
    String getProtocolName();
}
