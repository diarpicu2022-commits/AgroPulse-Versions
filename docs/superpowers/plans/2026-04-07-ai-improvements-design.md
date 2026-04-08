# Modificaciones AgroPulse - Diseño

## 1. IAs que funcionan

**Objetivo:** Dejar funcionando solo las IAs gratuitas sin tarjeta de crédito.

**Proveedores activos:**
- **Groq** - Ya configurado, funcionando
- **Ollama** - Ya instalado localmente, activar
- **GitHub Models** - Nuevo, gratuito via github.com

**Proveedores a desactivar:**
- OpenRouter (modelo gratuito no disponible)
- OpenAI (requiere API key de pago)
- Gemini (sin tier gratuito)
- DeepSeek (sin tier gratuito sin tarjeta)
- Mistral (sin tier gratuito)

---

## 2. Auto-rellenar cultivo con IA

**Objetivo:** Añadir botón en CropPanel que use Groq para obtener automáticamente los rangos óptimos del cultivo.

**Comportamiento:**
- Botón "🤖 Auto-completar con IA" siempre visible
- Al hacer clic: toma nombre + variedad del cultivo
- Envía prompt a Groq solicitando todos los rangos
- Rellena los campos vacíos con los valores devueltos
- Usuario puede editar manualmente después

**Datos a obtener:**
- Temperatura mínima (°C)
- Temperatura máxima (°C)
- Humedad mínima (%)
- Humedad máxima (%)
- Humedad suelo mínima (%)
- Humedad suelo máxima (%)

---

## 3. Rangos de sensores por invernadero

**Objetivo:** Los rangos de alertas sean independientes por invernadero, calculados como promedio de los cultivos activos.

**Diseño de datos:**

### Nueva tabla: `greenhouse_sensor_ranges`
| Campo | Tipo |
|-------|------|
| id | INTEGER PRIMARY KEY |
| greenhouse_id | INTEGER FK → greenhouse.id |
| sensor_type | VARCHAR (TEMPERATURE, HUMIDITY, SOIL_MOISTURE) |
| range_min | REAL |
| range_max | REAL |

### Cálculo de rangos:
- Obtener todos los cultivos activos del invernadero
- Calcular PROMEDIO de tempMin, tempMax, humidityMin, etc.
- Usar estos promedios como valores por defecto
- Usuario puede editar manualmente

### Alertas:
- Verificar contra el promedio del invernadero
- Notificar por WhatsApp si se sale de rangos