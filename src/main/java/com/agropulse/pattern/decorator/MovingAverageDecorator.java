package com.agropulse.pattern.decorator;

import java.util.LinkedList;
import java.util.List;

public class MovingAverageDecorator extends SensorDecorator {
    private List<Double> values = new LinkedList<>();
    private static final int WINDOW_SIZE = 5;
    
    public MovingAverageDecorator(SensorComponent sensor) {
        super(sensor);
    }
    
    @Override
    public double getValue() {
        values.add(wrapped.getValue());
        if (values.size() > WINDOW_SIZE) {
            values.remove(0);
        }
        return values.stream().mapToDouble(v -> v).average().orElse(0);
    }
}
