# ESP-IDF Component

idf_component_register(SRCS "sensors.c" "actuators.c" "comm_serial.c" "comm_http.c" "app_main.c"
                       INCLUDES ".")

# No extra components needed - using built-in drivers
