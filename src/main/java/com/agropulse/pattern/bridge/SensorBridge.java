package com.agropulse.pattern.bridge;

public abstract class SensorBridge {
    protected SensorProtocol protocol;
    
    public SensorBridge(SensorProtocol protocol) {
        this.protocol = protocol;
    }
    
    public abstract String readAndTransmit();
    public boolean isConnected() {
        return protocol.isConnected();
    }
}
