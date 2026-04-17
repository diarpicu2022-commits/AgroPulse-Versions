/*
 * AgroPulse ESP32 Firmware - PlatformIO Version
 * Plataforma: ESP32 Dev Board
 */

#include <Arduino.h>
#include <WiFi.h>
#include <DHT.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <Wire.h>
#include <Adafruit_SSD1306.h>
#include <Adafruit_GFX.h>

// ==================== DECLARACIONES ====================
void connectWiFi();
void readSensors();
void sendSerial();
void sendHTTP();
void processCommand(String cmd);
void controlPump();
void controlFan();
void updateOLED();
void showStartupScreen();
void showWiFiConnected();

// ==================== CONFIGURACIÓN DE PINES ====================
// SENSORES
#define DHT11_EXT_PIN     4     // DHT11 - Sensor temperatura/humedad EXTERNA
#define DTH22_INT_PIN     34    // DTH22 - Sensor temperatura/humedad INTERNA (ADC)
#define SOIL_SENSOR_PIN   35    // Sensor humedad suelo (ADC)

// OLED SSD1306 (I2C)
#define OLED_SDA          21    // SDA
#define OLED_SCL          22    // SCL
#define SCREEN_WIDTH      128   // OLED width
#define SCREEN_HEIGHT     64    // OLED height
#define OLED_ADDR         0x3C  // Dirección I2C OLED (0x3C o 0x3D)

// MÓDULO RELAY HW-383 (4 canales)
#define RELAY_PUMP        12    // Canal 1: Motobomba
#define RELAY_FAN         13    // Canal 2: Ventilador
#define RELAY_HEATER      14    // Canal 3: Calefactor
#define RELAY_DOOR        15    // Canal 4: Puerta

// LEDs
#define LED_STATUS       2
#define LED_WIFI         25
#define LED_SENSOR       26

// ==================== SENSORES ====================
DHT dhtExt(DHT11_EXT_PIN, DHT11);  // DHT11 externo

// OLED Display
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// ==================== VARIABLES GLOBALES ====================
float tempIn = 0, tempOut = 0;
float humidity = 0, soilMoisture = 0;
unsigned long lastRead = 0;
const long interval = 5000;  // 5 segundos

// Control automático - UMBRALES CONFIGURABLES
bool autoMode = false;  // Control manual por defecto
float pumpOnThreshold = 30;   // Activar bomba si humedad < 30%
float pumpOffThreshold = 60;  // Desactivar bomba si humedad > 60%
float fanOnThreshold = 28;    // Activar ventilador si temp > 28°C
float fanOffThreshold = 24;   // Desactivar ventilador si temp < 24°C

// Estados de los relés
bool pumpState = false;
bool fanState = false;

// WiFi
const char* ssid = "MARCILLO";
const char* password = "3188524275";
const char* serverIP = "192.168.1.28";
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
    pinMode(RELAY_FAN, OUTPUT);
    pinMode(RELAY_HEATER, OUTPUT);
    pinMode(RELAY_DOOR, OUTPUT);
    
    // Apagar actuadores
    digitalWrite(RELAY_PUMP, LOW);
    digitalWrite(RELAY_FAN, LOW);
    digitalWrite(RELAY_HEATER, LOW);
    digitalWrite(RELAY_DOOR, LOW);
    
    // Iniciar sensores
    dhtExt.begin();  // Iniciar DHT11 externo
    
    // Inicializar OLED
    Wire.begin(OLED_SDA, OLED_SCL);
    if (!display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDR)) {
        Serial.println("❌ SSD1306 allocation failed");
    } else {
        display.clearDisplay();
        display.setTextSize(1);
        display.setTextColor(SSD1306_WHITE);
        display.setCursor(0, 0);
        display.println("AgroPulse v9");
        display.println("Iniciando...");
        display.display();
        Serial.println("✅ OLED iniciado");
    }
    
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
    showStartupScreen();
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
        showWiFiConnected();
    } else {
        Serial.println("\nError: WiFi no conectado");
        display.clearDisplay();
        display.setTextSize(1);
        display.setTextColor(SSD1306_WHITE);
        display.setCursor(0, 20);
        display.println("Error: WiFi no conectado");
        display.println("Sistema en modo offline");
        display.display();
    }
}

void readSensors() {
    // DHT11: Temperatura e Humedad EXTERNA
    tempOut = dhtExt.readTemperature();
    humidity = dhtExt.readHumidity();
    
    // DTH22: Temperatura y humedad INTERNA (analógica)
    // Rango: 0-3.3V = -40 a +125°C
    int dth22Raw = analogRead(DTH22_INT_PIN);
    tempIn = (dth22Raw * 165.0 / 4095.0) - 40.0;  // Conversión a °C
    
    // Sensor de humedad del suelo (analógico)
    int soilRaw = analogRead(SOIL_SENSOR_PIN);
    soilMoisture = map(soilRaw, 4095, 1000, 0, 100);
    soilMoisture = constrain(soilMoisture, 0, 100);
    
    // Control automático de actuadores
    if (autoMode) {
        controlPump();
        controlFan();
    }
    
    // LED indicator
    digitalWrite(LED_SENSOR, HIGH);
    delay(50);
    digitalWrite(LED_SENSOR, LOW);
    
    // Actualizar pantalla OLED
    updateOLED();
    
    Serial.printf("📊 DHT11(EXT): %.1f°C | Hum: %.0f%% | DTH22(INT): %.1f°C | Suelo: %.0f%%\n", 
                 tempOut, humidity, tempIn, soilMoisture);
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
        autoMode = false;  // Desactivar modo automático
        bool state = cmd.substring(5) == "ON";
        digitalWrite(RELAY_PUMP, state);
        pumpState = state;
        Serial.println("💧 Motobomba: " + String(state ? "ON ✓" : "OFF ✗"));
    }
    else if (cmd.startsWith("FAN:") || cmd.startsWith("VENT:")) {
        autoMode = false;  // Desactivar modo automático
        bool state = cmd.substring(4) == "ON";
        digitalWrite(RELAY_FAN, state);
        fanState = state;
        Serial.println("🌬️  Ventilador: " + String(state ? "ON ✓" : "OFF ✗"));
    }
    else if (cmd.startsWith("HEAT:")) {
        bool state = cmd.substring(5) == "ON";
        digitalWrite(RELAY_HEATER, state);
        Serial.println("🔥 Calefactor: " + String(state ? "ON ✓" : "OFF ✗"));
    }
    else if (cmd.startsWith("DOOR:")) {
        bool state = cmd.substring(5) == "ON";
        digitalWrite(RELAY_DOOR, state);
        Serial.println("🚪 Puerta: " + String(state ? "ABIERTA ✓" : "CERRADA ✗"));
    }
    else if (cmd == "AUTO:ON") {
        autoMode = true;
        Serial.println("🤖 Modo AUTOMÁTICO activado");
    }
    else if (cmd == "AUTO:OFF") {
        autoMode = false;
        Serial.println("🤖 Modo AUTOMÁTICO desactivado");
    }
    else if (cmd == "STATUS") {
        Serial.printf("\n=== ESTADO DEL SISTEMA ===\n");
        Serial.printf("Modo: %s\n", autoMode ? "AUTOMÁTICO" : "MANUAL");
        Serial.printf("Motobomba: %s\n", digitalRead(RELAY_PUMP) ? "ON" : "OFF");
        Serial.printf("Ventilador: %s\n", digitalRead(RELAY_FAN) ? "ON" : "OFF");
        Serial.printf("Calefactor: %s\n", digitalRead(RELAY_HEATER) ? "ON" : "OFF");
        Serial.printf("Puerta: %s\n", digitalRead(RELAY_DOOR) ? "ABIERTA" : "CERRADA");
        Serial.printf("Umbrales - Bomba: <%d%% ON | >%d%% OFF\n", (int)pumpOnThreshold, (int)pumpOffThreshold);
        Serial.printf("Umbrales - Ventilador: >%d°C ON | <%d°C OFF\n", (int)fanOnThreshold, (int)fanOffThreshold);
        Serial.println("==========================\n");
    }
    else if (cmd.startsWith("PUMP_ON:")) {
        pumpOnThreshold = cmd.substring(8).toFloat();
        Serial.printf("✓ Umbral bomba ON: %.1f%%\n", pumpOnThreshold);
    }
    else if (cmd.startsWith("PUMP_OFF:")) {
        pumpOffThreshold = cmd.substring(9).toFloat();
        Serial.printf("✓ Umbral bomba OFF: %.1f%%\n", pumpOffThreshold);
    }
    else if (cmd.startsWith("FAN_ON:")) {
        fanOnThreshold = cmd.substring(7).toFloat();
        Serial.printf("✓ Umbral ventilador ON: %.1f°C\n", fanOnThreshold);
    }
    else if (cmd.startsWith("FAN_OFF:")) {
        fanOffThreshold = cmd.substring(8).toFloat();
        Serial.printf("✓ Umbral ventilador OFF: %.1f°C\n", fanOffThreshold);
    }
    else if (cmd == "HELP") {
        Serial.println("\n=== COMANDOS DISPONIBLES ===");
        Serial.println("PUMP:ON/OFF      - Control manual motobomba");
        Serial.println("FAN:ON/OFF       - Control manual ventilador");
        Serial.println("HEAT:ON/OFF      - Control calefactor");
        Serial.println("DOOR:ON/OFF      - Control puerta");
        Serial.println("AUTO:ON/OFF      - Activar/desactivar modo automático");
        Serial.println("PUMP_ON:X        - Establecer umbral ON bomba (X%)");
        Serial.println("PUMP_OFF:X       - Establecer umbral OFF bomba (X%)");
        Serial.println("FAN_ON:X         - Establecer umbral ON ventilador (X°C)");
        Serial.println("FAN_OFF:X        - Establecer umbral OFF ventilador (X°C)");
        Serial.println("STATUS           - Ver estado del sistema");
        Serial.println("HELP             - Mostrar esta ayuda");
        Serial.println("============================\n");
    }
    else if (cmd == "RESET") {
        digitalWrite(RELAY_PUMP, LOW);
        digitalWrite(RELAY_FAN, LOW);
        digitalWrite(RELAY_HEATER, LOW);
        digitalWrite(RELAY_DOOR, LOW);
        pumpState = false;
        fanState = false;
        Serial.println("⚠️  Todos los actuadores apagados");
    }
}

// ==================== CONTROL AUTOMÁTICO ====================

void controlPump() {
    // Lógica de control con histéresis para evitar oscilaciones
    if (!pumpState && soilMoisture < pumpOnThreshold) {
        digitalWrite(RELAY_PUMP, HIGH);
        pumpState = true;
        Serial.println("💧 Motobomba ACTIVADA (humedad baja)");
    }
    else if (pumpState && soilMoisture > pumpOffThreshold) {
        digitalWrite(RELAY_PUMP, LOW);
        pumpState = false;
        Serial.println("💧 Motobomba DESACTIVADA (humedad alta)");
    }
}

void controlFan() {
    // Control del ventilador basado en temperatura interna (DTH22)
    if (!fanState && tempIn > fanOnThreshold) {
        digitalWrite(RELAY_FAN, HIGH);
        fanState = true;
        Serial.println("🌬️  Ventilador ACTIVADO (temperatura alta)");
    }
    else if (fanState && tempIn < fanOffThreshold) {
        digitalWrite(RELAY_FAN, LOW);
        fanState = false;
        Serial.println("🌬️  Ventilador DESACTIVADO (temperatura baja)");
    }
}

// ==================== PANTALLA OLED ====================

void updateOLED() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    
    // Línea 1: Título y conexión WiFi
    display.print("AgroPulse v9  ");
    if (WiFi.status() == WL_CONNECTED) {
        display.println("[WiFi OK]");
    } else {
        display.println("[No WiFi]");
    }
    display.println("=");
    
    // Línea 3-4: Temperatura externa (DHT11)
    display.printf("EXT: %.1fC %.0f%%H\n", tempOut, humidity);
    
    // Línea 5-6: Temperatura interna (DTH22)
    display.printf("INT: %.1fC\n", tempIn);
    
    // Línea 7-8: Humedad del suelo
    display.printf("SUELO: %.0f%%\n", soilMoisture);
    
    display.println("=");
    
    // Línea 10-11: Estados de actuadores
    display.printf("B:%s V:%s | C:%s P:%s\n",
                   digitalRead(RELAY_PUMP) ? "ON" : "OFF",
                   digitalRead(RELAY_FAN) ? "ON" : "OFF",
                   digitalRead(RELAY_HEATER) ? "ON" : "OFF",
                   digitalRead(RELAY_DOOR) ? "AB" : "CR");
    
    // Línea 13: Modo
    display.printf("Modo: %s\n", autoMode ? "AUTO" : "MANUAL");
    
    display.display();
}

void showStartupScreen() {
    display.clearDisplay();
    display.setTextSize(2);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(10, 10);
    display.println("AgroPulse");
    display.setTextSize(1);
    display.setCursor(20, 35);
    display.println("Conectando WiFi...");
    display.setCursor(25, 50);
    display.println("Espere...");
    display.display();
}

void showWiFiConnected() {
    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0, 0);
    display.println("WiFi Conectado!");
    display.println("");
    display.printf("SSID: %s\n", ssid);
    display.printf("IP: %s\n", WiFi.localIP().toString().c_str());
    display.println("");
    display.println("Sistema listo");
    display.display();
    delay(2000);
}
