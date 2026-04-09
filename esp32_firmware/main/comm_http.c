#include "comm_http.h"
#include "config.h"
#include "esp_http_client.h"
#include "esp_wifi.h"
#include "esp_log.h"
#include "nvs_flash.h"
#include "lwip/netdb.h"
#include <string.h>
#include <stdio.h>

static const char *TAG = "COMM_HTTP";

static char ip_address[16] = "0.0.0.0";
static uint8_t wifi_connected = 0;

// Prototipo de función de callback
esp_err_t http_event_handler(esp_http_client_event_t *evt);

// Inicializar WiFi
void comm_http_init(void) {
    ESP_LOGI(TAG, "Inicializando WiFi...");
    
    // Inicializar NVS
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        nvs_flash_erase();
        nvs_flash_init();
    }

    // Configurar WiFi Station
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    esp_wifi_init(&cfg);
    esp_wifi_set_mode(WIFI_MODE_STA);
    esp_wifi_start();

    ESP_LOGI(TAG, "WiFi inicializado");
}

// Conectar a WiFi
void comm_http_connect_wifi(void) {
    wifi_config_t wifi_config = {
        .sta = {
            .threshold.authmode = WIFI_AUTH_WPA2_PSK,
        },
    };
    
    strncpy((char*)wifi_config.sta.ssid, WIFI_SSID, sizeof(wifi_config.sta.ssid));
    strncpy((char*)wifi_config.sta.password, WIFI_PASSWORD, sizeof(wifi_config.sta.password));

    esp_wifi_set_config(WIFI_IF_STA, &wifi_config);
    esp_wifi_connect();

    int retry = 0;
    while (wifi_connected == 0 && retry < 10) {
        vTaskDelay(pdMS_TO_TICKS(1000));
        retry++;
    }

    if (wifi_connected) {
        gpio_set_level(LED_WIFI, 1);
        ESP_LOGI(TAG, "WiFi conectado!");
    } else {
        ESP_LOGE(TAG, "Error conectando a WiFi");
    }
}

// Verificar conexión
uint8_t comm_http_is_connected(void) {
    return wifi_connected;
}

// Obtener IP
char* comm_http_get_ip(void) {
    return ip_address;
}

// Enviar datos por HTTP
void comm_http_send_reading(sensor_reading_t *reading) {
    if (!wifi_connected) {
        ESP_LOGW(TAG, "WiFi no conectado,no se puede enviar");
        return;
    }

    char json_data[512];
    int len = snprintf(json_data, sizeof(json_data),
        "{\"temp_in\":%.1f,\"temp_out\":%.1f,\"humidity\":%.1f,\"soil\":%.1f,\"ts\":%lu}",
        reading->temperature_internal,
        reading->temperature_external,
        reading->humidity,
        reading->soil_moisture,
        (unsigned long)reading->timestamp
    );

    char url[128];
    snprintf(url, sizeof(url), "http://%s:%d%s", HTTP_SERVER_IP, HTTP_SERVER_PORT, HTTP_PATH);

    esp_http_client_config_t config = {
        .url = url,
        .event_handler = http_event_handler,
        .method = HTTP_METHOD_POST,
    };

    esp_http_client_handle_t client = esp_http_client_init(&config);
    esp_http_client_set_header(client, "Content-Type", "application/json");
    esp_http_client_set_post_field(client, json_data, len);
    
    esp_err_t err = esp_http_client_perform(client);
    if (err == ESP_OK) {
        ESP_LOGI(TAG, "HTTP POST Status = %d", esp_http_client_get_status_code(client));
    } else {
        ESP_LOGE(TAG, "HTTP POST request failed: %s", esp_err_to_name(err));
    }
    
    esp_http_client_cleanup(client);
}

// Handler de eventos HTTP
esp_err_t http_event_handler(esp_http_client_event_t *evt) {
    switch(evt->event_id) {
        case HTTP_EVENT_ON_DATA:
            // Procesar respuesta si es necesario
            break;
        default:
            break;
    }
    return ESP_OK;
}
