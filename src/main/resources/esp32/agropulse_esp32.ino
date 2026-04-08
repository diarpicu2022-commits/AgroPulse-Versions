/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║   AgroPulse ESP32 — Firmware de Sensores v2.0           ║
 * ║   Compatible con: ESP32, ESP32-S2, ESP32-S3             ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * SENSORES SOPORTADOS:
 *   - DHT22 / DHT11  → Temperatura + Humedad
 *   - DS18B20        → Temperatura (OneWire)
 *   - Sensor capacitivo de humedad de suelo
 *   - Cualquier sensor analógico
 *
 * MODOS DE ENVÍO (configurar en config.h):
 *   MODE_SERIAL  → Envía JSON por USB/Serial
 *   MODE_WIFI    → POST HTTP a AgroPulse
 *   MODE_LORA    → Envía por LoRa (requiere módulo Ra-02 / SX1278)
 *   MODE_SD      → Guarda CSV en tarjeta SD
 *
 * LIBRERÍAS REQUERIDAS (instalar en Arduino IDE / PlatformIO):
 *   - DHT sensor library by Adafruit
 *   - ArduinoJson by Benoit Blanchon
 *   - RadioLib (para LoRa)
 *   - SD (incluida en Arduino)
 *
 * INSTALACIÓN (Arduino IDE):
 *   Herramientas → Gestor de placas → buscar "esp32" → instalar
 */

#include <Arduino.h>
#include <ArduinoJson.h>

// ══════════════════════════════════════════════
//  CONFIGURACIÓN — EDITAR SEGÚN TU INSTALACIÓN
// ══════════════════════════════════════════════

// Modo de envío (descomentar UNO)
#define MODE_SERIAL    // USB/Serial → AgroPulse Serial
//#define MODE_WIFI    // WiFi       → AgroPulse HTTP
//#define MODE_LORA    // LoRa       → Gateway LoRa
//#define MODE_SD      // SD Card    → CSV para cargar luego

// Pines de sensores
#define PIN_DHT        4    // DHT22 o DHT11
#define PIN_SOIL_MOISTURE 34  // Sensor analógico humedad suelo (ADC)
#define PIN_SOIL_DRY   4095  // Lectura ADC en seco (calibrar)
#define PIN_SOIL_WET   1500  // Lectura ADC en agua (calibrar)

// Intervalo de lectura (ms)
#define INTERVAL_MS    5000   // 5 segundos

// WiFi (solo si MODE_WIFI)
#define WIFI_SSID      "TuRedWifi"
#define WIFI_PASSWORD  "TuContrasena"
#define SERVER_IP      "192.168.1.100"  // IP del PC con AgroPulse
#define SERVER_PORT    8765

// ID único del dispositivo
#define DEVICE_ID      "ESP32-INV-001"

// ══════════════════════════════════════════════
//  INCLUDES SEGÚN MODO
// ══════════════════════════════════════════════

#ifdef MODE_WIFI
  #include <WiFi.h>
  #include <HTTPClient.h>
#endif

#ifdef MODE_LORA
  #include <RadioLib.h>
  // Pines para módulo Ra-02 / SX1278
  SX1278 radio = new Module(5, 2, 14, 15); // NSS, DIO0, RST, DIO1
#endif

#ifdef MODE_SD
  #include <SD.h>
  #include <SPI.h>
  #define SD_CS_PIN 5
#endif

// DHT22
#include <DHT.h>
DHT dht(PIN_DHT, DHT22);

// ══════════════════════════════════════════════
//  SETUP
// ══════════════════════════════════════════════

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("AgroPulse ESP32 v2.0 iniciando...");

  dht.begin();

  #ifdef MODE_WIFI
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    Serial.print("Conectando WiFi");
    while (WiFi.status() != WL_CONNECTED) {
      delay(500); Serial.print(".");
    }
    Serial.println("\nWiFi conectado: " + WiFi.localIP().toString());
  #endif

  #ifdef MODE_LORA
    int state = radio.begin(915.0); // 915 MHz para Colombia/América
    if (state == RADIOLIB_ERR_NONE) {
      Serial.println("LoRa iniciado correctamente.");
    } else {
      Serial.println("LoRa error: " + String(state));
    }
  #endif

  #ifdef MODE_SD
    if (!SD.begin(SD_CS_PIN)) {
      Serial.println("Error: SD no encontrada.");
    } else {
      Serial.println("SD card lista.");
      // Crear encabezado CSV si el archivo no existe
      if (!SD.exists("/datos.csv")) {
        File f = SD.open("/datos.csv", FILE_WRITE);
        if (f) {
          f.println("timestamp,temp_in,humidity,soil,device");
          f.close();
        }
      }
    }
  #endif

  Serial.println("Listo. Enviando lecturas cada " + String(INTERVAL_MS/1000) + "s");
}

// ══════════════════════════════════════════════
//  LOOP
// ══════════════════════════════════════════════

void loop() {
  float temp     = dht.readTemperature();
  float humidity = dht.readHumidity();
  float soil     = readSoilMoisture();

  // Validar lecturas
  if (isnan(temp) || isnan(humidity)) {
    Serial.println("ERROR: Sensor DHT no responde. Verifica conexión.");
    delay(INTERVAL_MS);
    return;
  }

  // Construir JSON
  StaticJsonDocument<200> doc;
  doc["device"]   = DEVICE_ID;
  doc["temp_in"]  = round(temp * 10) / 10.0;
  doc["humidity"] = round(humidity * 10) / 10.0;
  doc["soil"]     = round(soil * 10) / 10.0;

  String json;
  serializeJson(doc, json);

  // Enviar según modo
  #ifdef MODE_SERIAL
    sendSerial(json);
  #endif

  #ifdef MODE_WIFI
    sendWifi(json);
  #endif

  #ifdef MODE_LORA
    sendLora(json);
  #endif

  #ifdef MODE_SD
    saveSD(temp, humidity, soil);
  #endif

  delay(INTERVAL_MS);
}

// ══════════════════════════════════════════════
//  FUNCIONES DE ENVÍO
// ══════════════════════════════════════════════

void sendSerial(String json) {
  Serial.println(json);
}

#ifdef MODE_WIFI
void sendWifi(String json) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi desconectado, reintentando...");
    WiFi.reconnect();
    return;
  }
  HTTPClient http;
  String url = "http://" + String(SERVER_IP) + ":" + String(SERVER_PORT) + "/data";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  int code = http.POST(json);
  if (code == 200) {
    Serial.println("✓ Enviado WiFi: " + json);
  } else {
    Serial.println("✗ Error WiFi: HTTP " + String(code));
  }
  http.end();
}
#endif

#ifdef MODE_LORA
void sendLora(String json) {
  String payload = "LORA:" + json;  // Prefijo para que AgroPulse lo identifique
  int state = radio.transmit(payload);
  if (state == RADIOLIB_ERR_NONE) {
    Serial.println("✓ Enviado LoRa: " + payload);
  } else {
    Serial.println("✗ Error LoRa: " + String(state));
  }
}
#endif

#ifdef MODE_SD
void saveSD(float temp, float humidity, float soil) {
  File f = SD.open("/datos.csv", FILE_APPEND);
  if (f) {
    // Formato: 2024-01-15T10:30:00,24.5,65.0,42.0,ESP32-001
    String row = String(millis()) + "," +
                 String(temp, 1) + "," +
                 String(humidity, 1) + "," +
                 String(soil, 1) + "," +
                 DEVICE_ID;
    f.println(row);
    f.close();
    Serial.println("✓ Guardado en SD: " + row);
  } else {
    Serial.println("✗ Error abriendo SD");
  }
}
#endif

// ══════════════════════════════════════════════
//  LECTURA DE SENSORES
// ══════════════════════════════════════════════

float readSoilMoisture() {
  int raw = analogRead(PIN_SOIL_MOISTURE);
  // Mapear ADC (0-4095) a porcentaje (0-100%)
  float percent = map(raw, PIN_SOIL_DRY, PIN_SOIL_WET, 0, 100);
  percent = constrain(percent, 0.0, 100.0);
  return percent;
}

/*
 * ══════════════════════════════════════════════
 *  CONEXIONES DE HARDWARE
 * ══════════════════════════════════════════════
 *
 * DHT22:
 *   VCC → 3.3V
 *   GND → GND
 *   DATA → GPIO4 (PIN_DHT)
 *   Resistencia pull-up 10kΩ entre DATA y VCC
 *
 * Sensor humedad suelo (capacitivo):
 *   VCC → 3.3V
 *   GND → GND
 *   AOUT → GPIO34 (PIN_SOIL_MOISTURE) — ADC1 CH6
 *
 * Módulo LoRa Ra-02 (SX1278) — solo si MODE_LORA:
 *   NSS  → GPIO5
 *   DIO0 → GPIO2
 *   RST  → GPIO14
 *   MOSI → GPIO23 (SPI default)
 *   MISO → GPIO19 (SPI default)
 *   SCK  → GPIO18 (SPI default)
 *   3.3V → 3.3V (¡NO usar 5V!)
 *   GND  → GND
 *
 * SD Card (SPI) — solo si MODE_SD:
 *   CS   → GPIO5 (SD_CS_PIN)
 *   MOSI → GPIO23
 *   MISO → GPIO19
 *   SCK  → GPIO18
 */
