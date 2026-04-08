package com.agropulse.pattern.builder;

import com.agropulse.model.Crop;

public class CropBuilder {
    private String name = "";
    private String variety = "";
    private double tempMin = 15, tempMax = 30;
    private double humidityMin = 50, humidityMax = 80;
    private double soilMin = 40, soilMax = 70;
    
    public CropBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public CropBuilder variety(String variety) {
        this.variety = variety;
        return this;
    }
    
    public CropBuilder tempRange(double min, double max) {
        this.tempMin = min;
        this.tempMax = max;
        return this;
    }
    
    public CropBuilder humidityRange(double min, double max) {
        this.humidityMin = min;
        this.humidityMax = max;
        return this;
    }
    
    public CropBuilder soilRange(double min, double max) {
        this.soilMin = min;
        this.soilMax = max;
        return this;
    }
    
    public Crop build() {
        return new Crop(name, variety, tempMin, tempMax, humidityMin, humidityMax, soilMin, soilMax);
    }
}
