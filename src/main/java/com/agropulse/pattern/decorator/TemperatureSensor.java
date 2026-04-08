package com.agropulse.pattern.decorator;

public class TemperatureSensor implements SensorComponent {
    private double value;
    private String type = "TEMPERATURE";
    private long timestamp;
    
    public TemperatureSensor(double value) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override public double getValue() { return value; }
    @Override public String getType() { return type; }
    @Override public long getTimestamp() { return timestamp; }
}
