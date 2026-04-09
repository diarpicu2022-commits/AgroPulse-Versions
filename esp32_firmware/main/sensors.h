#ifndef SENSORS_H
#define SENSORS_H

#include <stdint.h>

// Estructura para almacenar lecturas de sensores
typedef struct {
    float temperature_internal;
    float temperature_external;
    float humidity;
    float soil_moisture;
    uint32_t timestamp;
} sensor_reading_t;

// Inicializar sensores
void sensors_init(void);

// Leer todos los sensores
sensor_reading_t sensors_read_all(void);

// Leer sensor individual
float sensors_read_temperature_internal(void);
float sensors_read_temperature_external(void);
float sensors_read_humidity(void);
float sensors_read_soil_moisture(void);

// Calibrar sensores
void sensors_calibrate_soil_moisture(float dry_value, float wet_value);

#endif // SENSORS_H
