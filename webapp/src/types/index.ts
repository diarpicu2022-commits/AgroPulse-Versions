export enum SensorType {
  TEMPERATURE_INTERNAL = 'TEMPERATURE_INTERNAL',
  TEMPERATURE_EXTERNAL = 'TEMPERATURE_EXTERNAL',
  HUMIDITY = 'HUMIDITY',
  SOIL_MOISTURE = 'SOIL_MOISTURE',
  LIGHT = 'LIGHT',
  CO2 = 'CO2'
}

export enum AlertLevel {
  INFO = 'INFO',
  WARNING = 'WARNING',
  CRITICAL = 'CRITICAL'
}

export interface SensorReading {
  id?: number;
  sensorId: number;
  greenhouseId: number;
  type: SensorType;
  value: number;
  unit: string;
  timestamp: Date;
  source: 'ESP32' | 'SIMULATION' | 'MANUAL';
}

export interface Sensor {
  id: number;
  name: string;
  type: SensorType;
  unit: string;
  minValue: number;
  maxValue: number;
  greenhouseId: number;
  enabled: boolean;
  calibrationOffset: number;
}

export interface Greenhouse {
  id: number;
  name: string;
  location: string;
  area: number;
  description: string;
  currentCropId?: number;
  active: boolean;
  createdAt: Date;
}

export interface Crop {
  id: number;
  name: string;
  variety: string;
  tempMin: number;
  tempMax: number;
  humidityMin: number;
  humidityMax: number;
  soilMoistureMin: number;
  soilMoistureMax: number;
  daysToHarvest: number;
  active: boolean;
  plantedDate: Date;
}

export interface Alert {
  id: number;
  greenhouseId: number;
  level: AlertLevel;
  message: string;
  createdAt: Date;
  acknowledged: boolean;
  sensorType?: string;
  value?: number;
}

export interface Actuator {
  id: number;
  name: string;
  type: string;
  pin: number;
  greenhouseId: number;
  enabled: boolean;
  autoMode: boolean;
  threshold?: number;
}

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'OPERATOR' | 'VIEWER';
  active: boolean;
}
