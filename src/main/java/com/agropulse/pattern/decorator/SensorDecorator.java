package com.agropulse.pattern.decorator;

public abstract class SensorDecorator implements SensorComponent {
    protected SensorComponent wrapped;
    
    public SensorDecorator(SensorComponent sensor) {
        this.wrapped = sensor;
    }
    
    @Override public double getValue() { return wrapped.getValue(); }
    @Override public String getType() { return wrapped.getType(); }
    @Override public long getTimestamp() { return wrapped.getTimestamp(); }
}
