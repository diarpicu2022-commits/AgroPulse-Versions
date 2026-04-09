/*
 * AgroPulse ESP32 Firmware - PlatformIO Version
 * Plataforma: ESP32 Dev Board
 */

#include <Arduino.h>
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// ==================== CONFIGURACIÓN DE PINES ====================
#define DHT22_INT_PIN     4    // DHT22 Interior
#define DHT22_EXT_PIN    5    // DHT22 Exterior
#define SOIL_SENSOR_PIN  36   // Sensor humedad suelo (A0)

// Actuadores
#define RELAY_PUMP       12   // Bomba agua
#define RELAY_VENT       13   // Ventilador
#define RELAY_HEATER     14   // Calefactor
#define RELAY_DOOR       15   // Puerta

// LEDs
#define LED_STATUS       2
#define LED_WIFI         25
#define LED_SENSOR       26

// ==================== SENSORES ====================
DHT dhtInt(DHT22_INT_PIN, DHT22);
DHT dhtExt(DHT22_EXT_PIN, DHT22);

OneWire oneWire(18);  // DS18B20 en GPIO18
DallasTemperature sensors(&oneWire);

// ==================== VARIABLES GLOBALES ====================
float tempIn = 0, tempOut = 0;
float humidity = 0, soilMoisture = 0;
unsigned long lastRead = 0;
const long interval = 5000;  // 5 segundos

// WiFi
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";
const char* serverIP = "192.168.1.100";
const int serverPort = 8765;

WiFiClient client;

// ==================== SETUP ====================
void setup() {
    Serial.begin(115200);
    
    // LEDs
    pinMode(LED_STATUS, OUTPUT);
    pinMode(LED_WIFI, OUTPUT);
    pinMode(LED_SENSOR, OUTPUT);
    
    // Actuadores
    pinMode(RELAY_PUMP, OUTPUT);
    pinMode(RELAY_VENT, OUTPUT);
    pinMode(RELAY_HEATER, OUTPUT);
    pinMode(RELAY_DOOR, OUTPUT);
    
    // Apagar actuadores
    digitalWrite(RELAY_PUMP, LOW);
    digitalWrite(RELAY_VENT, LOW);
    digitalWrite(RELAY_HEATER, LOW);
    digitalWrite(RELAY_DOOR, LOW);
    
    // Iniciar sensores
    dhtInt.begin();
    dhtExt.begin();
    sensors.begin();
    
    // LED inicio
    digitalWrite(LED_STATUS, HIGH);
    Serial.println("=== AgroPulse ESP32 Iniciado ===");
    
    // Conectar WiFi
    connectWiFi();
}

// ==================== LOOP ====================
void loop() {
    unsigned long now = millis();
    
    if (now - lastRead >= interval) {
        lastRead = now;
        readSensors();
        sendSerial();
        sendHTTP();
    }
    
    // Procesar comandos Serial
    if (Serial.available()) {
        String cmd = Serial.readStringUntil('\n');
        processCommand(cmd);
    }
}

// ==================== FUNCIONES ====================

void connectWiFi() {
    Serial.print("Conectando a WiFi...");
    WiFi.begin(ssid, password);
    
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20) {
        delay(500);
        Serial.print(".");
        attempts++;
    }
    
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\nWiFi conectado!");
        digitalWrite(LED_WIFI, HIGH);
    } else {
        Serial.println("\nError: WiFi no conectado");
    }
}

void readSensors() {
    // Temperatura y humedad interna (DHT22 #1)
    tempIn = dhtInt.readTemperature();
    humidity = dhtInt.readHumidity();
    
    // Temperatura externa (DHT22 #2)
    tempOut = dhtExt.readTemperature();
    
    // Humedad del suelo (analógico)
    int soilRaw = analogRead(SOIL_SENSOR_PIN);
    soilMoisture = map(soilRaw, 4095, 1000, 0, 100);
    soilMoisture = constrain(soilMoisture, 0, 100);
    
    // LED sensors
    digitalWrite(LED_SENSOR, HIGH);
    delay(50);
    digitalWrite(LED_SENSOR, LOW);
    
    Serial.printf("Lectura: TI=%.1f TE=%.1f Hum=%.1f Suelo=%d%%\n", 
                 tempIn, tempOut, humidity, (int)soilMoisture);
}

void sendSerial() {
    // Formato JSON
    Serial.printf("{\"temp_in\":%.1f,\"temp_out\":%.1f,\"humidity\":%.1f,\"soil\":%.1f,\"ts\":%lu}\n",
                 tempIn, tempOut, humidity, soilMoisture, millis());
}

void sendHTTP() {
    if (WiFi.status() == WL_CONNECTED) {
        if (client.connect(serverIP, serverPort)) {
            String json = "{\"temp_in\":" + String(tempIn) + 
                         ",\"temp_out\":" + String(tempOut) +
                         ",\"humidity\":" + String(humidity) +
                         ",\"soil\":" + String(soilMoisture) + "}";
            
            client.println("POST /data HTTP/1.1");
            client.println("Host: " + String(serverIP));
            client.println("Content-Type: application/json");
            client.println("Content-Length: " + String(json.length()));
            client.println();
            client.println(json);
            
            Serial.println("HTTP enviado");
        }
        client.stop();
    }
}

void processCommand(String cmd) {
    cmd.trim();
    cmd.toUpperCase();
    
    if (cmd.startsWith("PUMP:")) {
        bool state = cmd.substring(5) == "ON";
        digitalWrite(RELAY_PUMP, state);
        Serial.println("Bomba: " + String(state ? "ON" : "OFF"));
    }
    else if (cmd.startsWith("VENT:")) {
        bool state = cmd.substring(5) == "ON";
        digitalWrite(RELAY_VENT, state);
        Serial.println("Ventilador: " + String(state ? "ON" : "OFF"));
    }
    else if (cmd.startsWith("HEAT:")) {
        bool state = cmd.substring(5) == "ON";
        digitalWrite(RELAY_HEATER, state);
        Serial.println("Calefactor: " + String(state ? "ON" : "OFF"));
    }
    else if (cmd.startsWith("DOOR:")) {
        bool state = cmd.substring(5) == "ON";
        digitalWrite(RELAY_DOOR, state);
        Serial.println("Puerta: " + String(state ? "ABIERTA" : "CERRADA"));
    }
    else if (cmd == "STATUS") {
        Serial.printf("{\"pump\":%d,\"vent\":%d,\"heat\":%d,\"door\":%d}\n",
                     digitalRead(RELAY_PUMP), digitalRead(RELAY_VENT),
                     digitalRead(RELAY_HEATER), digitalRead(RELAY_DOOR));
    }
    else if (cmd == "RESET") {
        digitalWrite(RELAY_PUMP, LOW);
        digitalWrite(RELAY_VENT, LOW);
        digitalWrite(RELAY_HEATER, LOW);
        digitalWrite(RELAY_DOOR, LOW);
        Serial.println("Todos los actuadores apagados");
    }
}
