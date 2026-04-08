package com.agropulse.structure;

import com.agropulse.model.Sensor;

public class SensorArray {
    private Sensor[] array;
    private int count;
    private static final int MAX_SIZE = 20;
    
    public SensorArray() {
        array = new Sensor[MAX_SIZE];
        count = 0;
    }
    
    public void add(Sensor sensor) {
        if (count < MAX_SIZE) {
            array[count++] = sensor;
        }
    }
    
    public Sensor get(int index) {
        if (index >= 0 && index < count) {
            return array[index];
        }
        return null;
    }
    
    public int size() {
        return count;
    }
    
    public boolean isFull() {
        return count >= MAX_SIZE;
    }
}
