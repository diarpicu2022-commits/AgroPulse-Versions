# AgroPulse ESP32 Firmware

Firmware para ESP32-WROOM-32D - Sistema de Monitoreo de Invernadero

## Sensores Soportados
- DHT22 (Temperatura Interna/Externa, Humedad)
- DS18B20 (Temperatura OneWire)
- Sensor de Humedad del Suelo (capacitivo)

## Actuadores
- Bomba de agua (GPIO 12)
- Ventilador/Extractor (GPIO 13)
- Calefactor (GPIO 14)
- Puerta/Servo (GPIO 15)

## Comunicación
- Serial/USB (115200 baud)
- WiFi/HTTP (POST al servidor Java)
- LoRa (vía gateway)

## Estructura de Archivos
```
esp32_firmware/
├── main/
│   ├── app_main.c      # Punto de entrada
│   ├── sensors.c/h    # Lectura de sensores
│   ├── actuators.c/h # Control de actuadores
│   ├── comm_serial.c/h  # Comunicación Serial
│   ├── comm_http.c/h   # Comunicación WiFi
│   ├── config.h        # Configuración de pines
│   └── CMakeLists.txt
├── CMakeLists.txt
├── sdkconfig.defaults
└── README.md
```

## Compilación

### Requisitos
- ESP-IDF instalado (`idf.py`)
- Python 3.8+
- CMake 3.16+

### Pasos

```bash
# 1. Ir al directorio del proyecto
cd esp32_firmware

# 2. Configurar proyecto
idf.py set-target esp32
idf.py menuconfig

# 3. Compilar
idf.py build

# 4. Flashear
idf.py -p COM3 flash monitor
```

(Reemplazar `COM3` con el puerto serie correcto)

## Configuración WiFi

Edita `main/config.h` y cambia:
```c
#define WIFI_SSID        "TU_SSID_WIFI"
#define WIFI_PASSWORD    "TU_PASSWORD"
#define HTTP_SERVER_IP   "192.168.1.100"  # IP de tu PC
```

## Conexión de Pines

| GPIO | Función |
|------|---------|
| 4    | DHT22 Interior |
| 5    | DHT22 Exterior |
| 12   | Relé Bomba |
| 13   | Relé Ventilador |
| 14   | Relé Calefactor |
| 15   | Relé Puerta |
| 2    | LED Estado |
| 25   | LED WiFi |
| 26   | LED Sensores |
| 36(A0)| Sensor Humedad Suelo |

## Formato de Datos Serial

```json
{"temp_in":24.5,"temp_out":22.1,"humidity":65.0,"soil":45.2,"ts":1234567890}
```

## Comandos Serial

- `PUMP:ON` / `PUMP:OFF` - Controlar bomba
- `VENT:ON` / `VENT:OFF` - Controlar ventilador
- `HEAT:ON` / `HEAT:OFF` - Controlar calefactor
- `DOOR:ON` / `DOOR:OFF` - Controlar puerta
- `STATUS` - Ver estado de actuadores
- `RESET` - Apagar todos los actuadores
