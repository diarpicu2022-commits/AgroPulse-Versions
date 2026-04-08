package com.agropulse.pattern.decorator;

public interface SensorComponent {
    double getValue();
    String getType();
    long getTimestamp();
}
