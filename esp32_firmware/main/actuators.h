#ifndef ACTUATORS_H
#define ACTUATORS_H

#include <stdint.h>

// Estados de los actuadores
typedef struct {
    uint8_t water_pump : 1;
    uint8_t ventilator : 1;
    uint8_t heater : 1;
    uint8_t door : 1;
} actuator_state_t;

// Inicializar actuadores
void actuators_init(void);

// Controlar actuadores
void actuators_set_water_pump(uint8_t state);
void actuators_set_ventilator(uint8_t state);
void actuators_set_heater(uint8_t state);
void actuators_set_door(uint8_t state);

// Obtener estado actual
actuator_state_t actuators_get_state(void);

// Apagar todos los actuadores
void actuators_all_off(void);

#endif // ACTUATORS_H
