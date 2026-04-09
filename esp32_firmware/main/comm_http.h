#ifndef COMM_HTTP_H
#define COMM_HTTP_H

#include <stdint.h>
#include "sensors.h"
#include "actuators.h"

// Inicializar WiFi y HTTP
void comm_http_init(void);

// Conectar a WiFi
void comm_http_connect_wifi(void);

// Enviar datos por HTTP POST
void comm_http_send_reading(sensor_reading_t *reading);

// Verificar conexión WiFi
uint8_t comm_http_is_connected(void);

// Obtener IP del dispositivo
char* comm_http_get_ip(void);

#endif // COMM_HTTP_H
