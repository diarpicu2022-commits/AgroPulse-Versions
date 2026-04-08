package com.agropulse.pattern.bridge;

public class TemperatureSensorBridge extends SensorBridge {
    private double temperature;
    
    public TemperatureSensorBridge(SensorProtocol protocol) {
        super(protocol);
    }
    
    public void setTemperature(double temp) {
        this.temperature = temp;
    }
    
    @Override
    public String readAndTransmit() {
        String data = "TEMPERATURE:" + temperature + "°C";
        return protocol.transmitData(data);
    }
}
