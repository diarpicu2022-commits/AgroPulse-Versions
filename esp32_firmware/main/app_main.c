/*
 * AgroPulse ESP32 Firmware
 * Sistema de Monitoreo de Invernadero
 * 
 * Sensores: DHT22, DS18B20, Humedad del suelo
 * Actuadores: Bomba agua, Ventilador, Calefactor, Puerta
 * Comunicación: Serial, WiFi/HTTP, LoRa
 */

#include <stdio.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "esp_system.h"

#include "config.h"
#include "sensors.h"
#include "actuators.h"
#include "comm_serial.h"
#include "comm_http.h"

static const char *TAG = "APP_MAIN";

// Tareas
void sensor_read_task(void *pvParameters);
void http_send_task(void *pvParameters);

void app_main(void) {
    ESP_LOGI(TAG, "=== AgroPulse ESP32 Inicializando ===");
    
    // LED indicadores
    gpio_set_level(LED_STATUS, 1);  // LED de estado
    
    // Inicializar subsystems
    sensors_init();
    actuators_init();
    comm_serial_init();
    comm_http_init();
    
    ESP_LOGI(TAG, "Todos los subsistemas inicializados");
    
    // Conectar WiFi
    comm_http_connect_wifi();
    
    // Crear tareas
    xTaskCreate(sensor_read_task, "sensor_read", 4096, NULL, 5, NULL);
    xTaskCreate(http_send_task, "http_send", 4096, NULL, 3, NULL);
    
    ESP_LOGI(TAG, "Tareas creadas. Sistema operativo.");
}

// Tarea: Leer sensores y enviar por Serial
void sensor_read_task(void *pvParameters) {
    ESP_LOGI(TAG, "Tarea de lectura de sensores iniciada");
    
    while (1) {
        // Leer todos los sensores
        sensor_reading_t reading = sensors_read_all();
        
        // Enviar por Serial
        comm_serial_send_reading(&reading);
        
        // LED parpadeo
        gpio_set_level(LED_STATUS, 0);
        vTaskDelay(pdMS_TO_TICKS(100));
        gpio_set_level(LED_STATUS, 1);
        
        // Esperar intervalo configurado
        vTaskDelay(pdMS_TO_TICKS(SENSOR_READ_INTERVAL_MS));
    }
}

// Tarea: Enviar por HTTP cada 30 segundos
void http_send_task(void *pvParameters) {
    ESP_LOGI(TAG, "Tarea HTTP iniciada");
    
    while (1) {
        if (comm_http_is_connected()) {
            // Leer y enviar
            sensor_reading_t reading = sensors_read_all();
            comm_http_send_reading(&reading);
        }
        
        vTaskDelay(pdMS_TO_TICKS(HTTP_RETRY_INTERVAL_MS));
    }
}
