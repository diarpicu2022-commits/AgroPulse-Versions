package com.agropulse.pattern.decorator;

public class NoiseFilterDecorator extends SensorDecorator {
    private double lastValue;
    private static final double THRESHOLD = 10.0;
    
    public NoiseFilterDecorator(SensorComponent sensor) {
        super(sensor);
        this.lastValue = sensor.getValue();
    }
    
    @Override
    public double getValue() {
        double current = wrapped.getValue();
        if (Math.abs(current - lastValue) > THRESHOLD) {
            lastValue = current;
        }
        return lastValue;
    }
}
