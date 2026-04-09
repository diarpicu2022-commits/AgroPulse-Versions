#ifndef CONFIG_H
#define CONFIG_H

// ==================== PIN CONFIGURATION ====================
// Pines GPIO del ESP32-WROOM-32D

// Sensores DHT22 (Temperatura y Humedad)
#define DHT22_PIN_1      GPIO_NUM_4    // DHT22 Interior
#define DHT22_PIN_2      GPIO_NUM_5    // DHT22 Exterior

// Sensores DS18B20 (Temperatura OneWire)
#define DS18B20_PIN      GPIO_NUM_18   // OneWire para DS18B20

// Sensor de Humedad del Suelo (analógico)
#define SOIL_MOISTURE_PIN ADC1_CHANNEL_0  // GPIO36 (A0)

// Sensores analógicos (ADC)
#define TEMP_INTERNAL_ADC   ADC1_CHANNEL_3  // GPIO39
#define TEMP_EXTERNAL_ADC  ADC1_CHANNEL_4  // GPIO36

// Actuadores - Relés (GPIO)
#define RELAY_WATER_PUMP   GPIO_NUM_12   // Bomba de agua
#define RELAY_VENTILATOR   GPIO_NUM_13   // Ventilador/Extractor
#define RELAY_HEATER       GPIO_NUM_14   // Calefactor
#define RELAY_DOOR         GPIO_NUM_15   // Puerta (servomotor)

// LEDs de estado
#define LED_STATUS        GPIO_NUM_2    // LED integrado
#define LED_WIFI          GPIO_NUM_25   // LED WiFi conectado
#define LED_SENSOR        GPIO_NUM_26   // LED sensores activos

// ==================== WIFI CONFIGURATION ====================
#define WIFI_SSID        "YOUR_WIFI_SSID"
#define WIFI_PASSWORD    "YOUR_WIFI_PASSWORD"

// Servidor HTTP (PC con aplicación Java)
#define HTTP_SERVER_IP   "192.168.1.100"
#define HTTP_SERVER_PORT  8765
#define HTTP_PATH         "/data"

// ==================== SERIAL CONFIGURATION ====================
#define SERIAL_BAUD_RATE  115200

// ==================== LORA CONFIGURATION ====================
#define LORA_CS_PIN      GPIO_NUM_5
#define LORA_RST_PIN     GPIO_NUM_19
#define LORA_IRQ_PIN     GPIO_NUM_21

// ==================== TIMING CONFIGURATION ====================
#define SENSOR_READ_INTERVAL_MS  5000    // Leer cada 5 segundos
#define HTTP_RETRY_INTERVAL_MS   30000   // Reintentar HTTP cada 30s
#define WATCHDOG_TIMEOUT_S       60       // Watchdog timeout

// ==================== MEASUREMENT PINS ====================
// Mapa de pines a tipos de sensor
typedef struct {
    gpio_num_t pin;
    char name[32];
    char unit[8];
} sensor_pin_t;

static const sensor_pin_t sensor_pins[] = {
    {DHT22_PIN_1,  "TEMP_INTERNAL", "°C"},
    {DHT22_PIN_2,  "TEMP_EXTERNAL", "°C"},
    {DHT22_PIN_1,  "HUMIDITY",      "%"},
    {SOIL_MOISTURE_PIN, "SOIL_MOISTURE", "%"},
};

#endif // CONFIG_H
