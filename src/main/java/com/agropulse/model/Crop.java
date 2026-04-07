package com.agropulse.model;

import java.time.LocalDate;

/**
 * Modelo de cultivo del invernadero.
 * Esta versión incluye alias para los métodos, resolviendo conflictos 
 * en todo el proyecto AgroPulse.
 */
public class Crop {
    private int id;
    private String name;
    private String variety;
    private double tempMin;
    private double tempMax;
    private double humidityMin;
    private double humidityMax;
    private double soilMoistureMin;
    private double soilMoistureMax;
    private LocalDate plantingDate;
    private boolean active;

    public Crop() {
        this.active = true;
        this.plantingDate = LocalDate.now();
    }

    public Crop(String name, String variety,
                double tempMin, double tempMax,
                double humidityMin, double humidityMax,
                double soilMoistureMin, double soilMoistureMax) {
        this();
        this.name = name;
        this.variety = variety;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.humidityMin = humidityMin;
        this.humidityMax = humidityMax;
        this.soilMoistureMin = soilMoistureMin;
        this.soilMoistureMax = soilMoistureMax;
    }

    // --- Getters y Setters Estándar (Para archivos antiguos) ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVariety() { return variety; }
    public void setVariety(String variety) { this.variety = variety; }

    public double getTempMin() { return tempMin; }
    public void setTempMin(double tempMin) { this.tempMin = tempMin; }
    public double getTempMax() { return tempMax; }
    public void setTempMax(double tempMax) { this.tempMax = tempMax; }

    public double getHumidityMin() { return humidityMin; }
    public void setHumidityMin(double humidityMin) { this.humidityMin = humidityMin; }
    public double getHumidityMax() { return humidityMax; }
    public void setHumidityMax(double humidityMax) { this.humidityMax = humidityMax; }

    public double getSoilMoistureMin() { return soilMoistureMin; }
    public void setSoilMoistureMin(double soilMoistureMin) { this.soilMoistureMin = soilMoistureMin; }
    public double getSoilMoistureMax() { return soilMoistureMax; }
    public void setSoilMoistureMax(double soilMoistureMax) { this.soilMoistureMax = soilMoistureMax; }

    // --- Alias de Compatibilidad (Para el nuevo CropPanel) ---
    // Estos métodos simplemente llaman a los anteriores para que el nuevo código compile
    public double getMinTemp() { return getTempMin(); }
    public double getMaxTemp() { return getTempMax(); }
    public double getMinHumidity() { return getHumidityMin(); }
    public double getMaxHumidity() { return getHumidityMax(); }
    public double getMinSoilMoisture() { return getSoilMoistureMin(); }
    public double getMaxSoilMoisture() { return getSoilMoistureMax(); }

    public LocalDate getPlantingDate() { return plantingDate; }
    public void setPlantingDate(LocalDate plantingDate) { this.plantingDate = plantingDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}