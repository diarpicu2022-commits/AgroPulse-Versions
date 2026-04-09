#ifndef COMM_SERIAL_H
#define COMM_SERIAL_H

#include <stdint.h>
#include "sensors.h"
#include "actuators.h"

// Inicializar comunicación Serial
void comm_serial_init(void);

// Enviar datos por Serial
void comm_serial_send_reading(sensor_reading_t *reading);

// Procesar comandos recibidos por Serial
void comm_serial_process_command(char *command);

// Enviar estado de actuadores
void comm_serial_send_actuator_state(actuator_state_t *state);

#endif // COMM_SERIAL_H
