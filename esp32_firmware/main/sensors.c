#include "sensors.h"
#include "config.h"
#include "driver/gpio.h"
#include "driver/adc.h"
#include "esp_adc_cal.h"
#include "esp_log.h"
#include "dht.h"
#include "onewire_bus.h"

static const char *TAG = "SENSORS";

// Valores de calibración para sensor de humedad del suelo
static float soil_dry_value = 4095.0f;  // Valor en aire
static float soil_wet_value = 1000.0f;  // Valor en agua

// ADC para sensor analógico
static esp_adc_cal_characteristics_t *adc_chars;

// Inicializar todos los sensores
void sensors_init(void) {
    ESP_LOGI(TAG, "Inicializando sensores...");

    // Configurar ADC para sensor de humedad del suelo
    adc1_config_width(ADC_WIDTH_BIT_12);
    adc1_config_channel_atten(SOIL_MOISTURE_PIN, ADC_ATTEN_DB_11);

    // Calibración ADC
    adc_chars = calloc(1, sizeof(esp_adc_cal_characterises_t));
    esp_adc_cal_characterize(ADC_UNIT_1, ADC_ATTEN_DB_11, ADC_WIDTH_BIT_12, 0, adc_chars);

    // Configurar GPIO para LEDs
    gpio_config_t led_config = {
        .pin_bit_mask = (1ULL << LED_STATUS) | (1ULL << LED_WIFI) | (1ULL << LED_SENSOR),
        .mode = GPIO_MODE_OUTPUT,
        .pull_up_en = GPIO_PULLUP_DISABLE,
        .pull_down_en = GPIO_PULLDOWN_DISABLE,
        .intr_type = GPIO_INTR_DISABLE
    };
    gpio_config(&led_config);

    // Apagar LEDs inicialmente
    gpio_set_level(LED_STATUS, 0);
    gpio_set_level(LED_WIFI, 0);
    gpio_set_level(LED_SENSOR, 0);

    ESP_LOGI(TAG, "Sensores inicializados");
}

// Leer temperatura interna (DHT22 #1)
float sensors_read_temperature_internal(void) {
    float temp = 0, hum = 0;
    int ret = dht_read_float_data(DHT22_PIN_1, &hum, &temp);
    if (ret == ESP_OK) {
        return temp;
    }
    ESP_LOGW(TAG, "Error leyendo DHT22 interno");
    return -1.0f;
}

// Leer temperatura externa (DHT22 #2 o DS18B20)
float sensors_read_temperature_external(void) {
    // Intentar DHT22 primero
    float temp = 0, hum = 0;
    int ret = dht_read_float_data(DHT22_PIN_2, &hum, &temp);
    if (ret == ESP_OK) {
        return temp;
    }
    
    // Si falla DHT, intentar DS18B20 (OneWire)
    // Implementación OneWire para DS18B20
    // Por ahora retornamos -1 si no hay sensor
    ESP_LOGW(TAG, "Error leyendo temperatura externa");
    return -1.0f;
}

// Leer humedad relativa (DHT22)
float sensors_read_humidity(void) {
    float temp = 0, hum = 0;
    int ret = dht_read_float_data(DHT22_PIN_1, &hum, &temp);
    if (ret == ESP_OK) {
        return hum;
    }
    ESP_LOGW(TAG, "Error leyendo humedad");
    return -1.0f;
}

// Leer humedad del suelo (sensor capacitivo analógico)
float sensors_read_soil_moisture(void) {
    uint32_t adc_value = 0;
    
    // Leer ADC varias veces y promediar
    for (int i = 0; i < 10; i++) {
        adc_value += adc1_get_raw(SOIL_MOISTURE_PIN);
        vTaskDelay(pdMS_TO_TICKS(10));
    }
    adc_value /= 10;

    // Convertir a porcentaje (0-100%)
    // dry_value = 4095 (seco), wet_value = 1000 (húmedo)
    float percentage = 100.0f * (1.0f - ((float)adc_value - soil_wet_value) / (soil_dry_value - soil_wet_value));
    
    // Limitar entre 0 y 100
    if (percentage < 0) percentage = 0;
    if (percentage > 100) percentage = 100;

    return percentage;
}

// Leer todos los sensores
sensor_reading_t sensors_read_all(void) {
    sensor_reading_t reading;
    
    reading.temperature_internal = sensors_read_temperature_internal();
    reading.temperature_external = sensors_read_temperature_external();
    reading.humidity = sensors_read_humidity();
    reading.soil_moisture = sensors_read_soil_moisture();
    reading.timestamp = esp_log_timestamp();

    // LED indicador de lectura
    gpio_set_level(LED_SENSOR, 1);
    vTaskDelay(pdMS_TO_TICKS(50));
    gpio_set_level(LED_SENSOR, 0);

    ESP_LOGI(TAG, "Lectura: TempInt=%.1f, TempExt=%.1f, Hum=%.1f, Soil=%.1f",
             reading.temperature_internal, reading.temperature_external,
             reading.humidity, reading.soil_moisture);

    return reading;
}

// Calibrar sensor de humedad del suelo
void sensors_calibrate_soil_moisture(float dry, float wet) {
    soil_dry_value = dry;
    soil_wet_value = wet;
    ESP_LOGI(TAG, "Calibración suelo: seco=%.0f, húmedo=%.0f", dry, wet);
}
