#include "actuators.h"
#include "config.h"
#include "driver/gpio.h"
#include "esp_log.h"

static const char *TAG = "ACTUATORS";

// Estado actual de los actuadores
static actuator_state_t current_state = {0, 0, 0, 0};

// Inicializar todos los actuadores
void actuators_init(void) {
    ESP_LOGI(TAG, "Inicializando actuadores...");

    // Configurar pines de actuadores como salida
    gpio_config_t out_config = {
        .pin_bit_mask = (1ULL << RELAY_WATER_PUMP) | 
                        (1ULL << RELAY_VENTILATOR) | 
                        (1ULL << RELAY_HEATER) |
                        (1ULL << RELAY_DOOR),
        .mode = GPIO_MODE_OUTPUT,
        .pull_up_en = GPIO_PULLUP_DISABLE,
        .pull_down_en = GPIO_PULLDOWN_DISABLE,
        .intr_type = GPIO_INTR_DISABLE
    };
    gpio_config(&out_config);

    // Apagar todos los actuadores inicialmente (relé activo bajo)
    actuators_all_off();

    ESP_LOGI(TAG, "Actuadores inicializados");
}

// Controlar bomba de agua
void actuators_set_water_pump(uint8_t state) {
    gpio_set_level(RELAY_WATER_PUMP, state);
    current_state.water_pump = state;
    ESP_LOGI(TAG, "Bomba agua: %s", state ? "ON" : "OFF");
}

// Controlar ventilador
void actuators_set_ventilator(uint8_t state) {
    gpio_set_level(RELAY_VENTILATOR, state);
    current_state.ventilator = state;
    ESP_LOGI(TAG, "Ventilador: %s", state ? "ON" : "OFF");
}

// Controlar calentador
void actuators_set_heater(uint8_t state) {
    gpio_set_level(RELAY_HEATER, state);
    current_state.heater = state;
    ESP_LOGI(TAG, "Calentador: %s", state ? "ON" : "OFF");
}

// Controlar puerta
void actuators_set_door(uint8_t state) {
    gpio_set_level(RELAY_DOOR, state);
    current_state.door = state;
    ESP_LOGI(TAG, "Puerta: %s", state ? "ABIERTA" : "CERRADA");
}

// Obtener estado actual
actuator_state_t actuators_get_state(void) {
    return current_state;
}

// Apagar todos los actuadores
void actuators_all_off(void) {
    gpio_set_level(RELAY_WATER_PUMP, 0);
    gpio_set_level(RELAY_VENTILATOR, 0);
    gpio_set_level(RELAY_HEATER, 0);
    gpio_set_level(RELAY_DOOR, 0);
    current_state = (actuator_state_t){0, 0, 0, 0};
    ESP_LOGI(TAG, "Todos los actuadores apagados");
}
