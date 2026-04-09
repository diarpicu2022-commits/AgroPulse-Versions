#include "comm_serial.h"
#include "config.h"
#include "driver/uart.h"
#include "esp_log.h"
#include "esp_system.h"
#include <string.h>
#include <stdio.h>

static const char *TAG = "COMM_SERIAL";

// Buffer para UART
#define BUF_SIZE (1024)

// Inicializar UART para comunicación Serial
void comm_serial_init(void) {
    ESP_LOGI(TAG, "Inicializando UART...");

    uart_config_t uart_config = {
        .baud_rate = SERIAL_BAUD_RATE,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .rx_flow_ctrl_thresh = 122,
    };

    // Configurar UART0 (USB-Serial)
    uart_param_config(UART_NUM_0, &uart_config);
    uart_set_pin(UART_NUM_0, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE);
    uart_driver_install(UART_NUM_0, BUF_SIZE * 2, 0, 0, NULL, 0);

    ESP_LOGI(TAG, "UART inicializado a %d baud", SERIAL_BAUD_RATE);
}

// Enviar lectura por Serial en formato JSON
void comm_serial_send_reading(sensor_reading_t *reading) {
    char buffer[256];
    int len = snprintf(buffer, sizeof(buffer),
        "{\"temp_in\":%.1f,\"temp_out\":%.1f,\"humidity\":%.1f,\"soil\":%.1f,\"ts\":%lu}\n",
        reading->temperature_internal,
        reading->temperature_external,
        reading->humidity,
        reading->soil_moisture,
        (unsigned long)reading->timestamp
    );
    
    uart_write_bytes(UART_NUM_0, buffer, len);
    ESP_LOGD(TAG, "Serial enviado: %s", buffer);
}

// Procesar comandos recibidos por Serial
void comm_serial_process_command(char *command) {
    ESP_LOGI(TAG, "Comando recibido: %s", command);

    // Comandos disponibles:
    // PUMP:ON / PUMP:OFF
    // VENT:ON / VENT:OFF
    // HEAT:ON / HEAT:OFF
    // DOOR:ON / DOOR:OFF
    // STATUS
    // RESET

    if (strncmp(command, "PUMP:", 5) == 0) {
        actuators_set_water_pump(strcmp(command + 5, "ON") == 0 ? 1 : 0);
    }
    else if (strncmp(command, "VENT:", 5) == 0) {
        actuators_set_ventilator(strcmp(command + 5, "ON") == 0 ? 1 : 0);
    }
    else if (strncmp(command, "HEAT:", 5) == 0) {
        actuators_set_heater(strcmp(command + 5, "ON") == 0 ? 1 : 0);
    }
    else if (strncmp(command, "DOOR:", 5) == 0) {
        actuators_set_door(strcmp(command + 5, "ON") == 0 ? 1 : 0);
    }
    else if (strcmp(command, "STATUS") == 0) {
        actuator_state_t state = actuators_get_state();
        comm_serial_send_actuator_state(&state);
    }
    else if (strcmp(command, "RESET") == 0) {
        actuators_all_off();
        ESP_LOGI(TAG, "Actuadores reseteados");
    }
}

// Enviar estado de actuadores
void comm_serial_send_actuator_state(actuator_state_t *state) {
    char buffer[128];
    int len = snprintf(buffer, sizeof(buffer),
        "{\"pump\":%d,\"vent\":%d,\"heat\":%d,\"door\":%d}\n",
        state->water_pump,
        state->ventilator,
        state->heater,
        state->door
    );
    uart_write_bytes(UART_NUM_0, buffer, len);
}
