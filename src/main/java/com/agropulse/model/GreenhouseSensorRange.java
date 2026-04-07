package com.agropulse.model;

public class GreenhouseSensorRange {
    private int id;
    private int greenhouseId;
    private String sensorType;
    private double rangeMin;
    private double rangeMax;

    public GreenhouseSensorRange() {}

    public GreenhouseSensorRange(int greenhouseId, String sensorType, double rangeMin, double rangeMax) {
        this.greenhouseId = greenhouseId;
        this.sensorType = sensorType;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int id) { this.greenhouseId = id; }
    public String getSensorType() { return sensorType; }
    public void setSensorType(String t) { this.sensorType = t; }
    public double getRangeMin() { return rangeMin; }
    public void setRangeMin(double v) { this.rangeMin = v; }
    public double getRangeMax() { return rangeMax; }
    public void setRangeMax(double v) { this.rangeMax = v; }
}