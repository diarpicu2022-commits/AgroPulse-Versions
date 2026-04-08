# Diseño: AgroPulse v9 - Mejoras Completas

## 1. Limpiar APIs no usadas

**Objetivo:** Mantener solo las IAs funcionales.

**Cambios en código:**
- Eliminar: `OpenRouterService`, `OpenAIService`, `MistralService`, `DeepSeekService`, `GeminiService`
- Mantener: `GroqService`, `GitHubModelsService`, `OllamaService`

**Cambios en UI (APIConfigPanel):**
- Mostrar solo: Groq, GitHub Models, Ollama
- Ocultar las no funcionales

---

## 2. Rangos de sensores automáticos

**Objetivo:** Calcular rangos automáticamente según cultivos.

**Lógica:**
1. **Primer cultivo:** Inicializar `greenhouse_sensor_ranges` con valores del cultivo
2. **Más cultivos:** Recalcular promedio = (suma valores todos los cultivos) / cantidad
3. **Edición manual:** Admin puede modificar en `ConfigRangesPanel`

**Tabla:** `greenhouse_sensor_ranges`
- greenhouse_id, sensor_type, range_min, range_max

---

## 3. Webapp (Vercel)

**Cambios:**
- `webapp/src/services/ai.ts`: Groq + GitHub Models + Ollama
- `webapp/src/App.jsx`: Panel de cultivos con auto-fill IA
- Integrar rango de sensores calculado automáticamente

---

## 4. Patrones faltantes (implementar)

### 4.1 Decorator
**Uso:** Añadir funcionalidades a sensores sin modificar clases base.

**Implementación:**
```java
// Componente base
public interface SensorComponent {
    double getValue();
    String getType();
}

// Sensor concreto
public class TemperatureSensor implements SensorComponent {
    private double value;
    public double getValue() { return value; }
    public String getType() { return "TEMPERATURE"; }
}

// Decorator base
public abstract class SensorDecorator implements SensorComponent {
    protected SensorComponent wrapped;
    public SensorDecorator(SensorComponent s) { this.wrapped = s; }
}

// Decorator concret: filtro de ruido
public class NoiseFilterDecorator extends SensorDecorator {
    private double lastValue;
    public double getValue() {
        double current = wrapped.getValue();
        if (Math.abs(current - lastValue) > 10) {
            lastValue = current;
            return current;
        }
        return lastValue;
    }
}

// Decorator concreto: promedio móvil
public class MovingAverageDecorator extends SensorDecorator {
    private List<Double> values = new LinkedList<>();
    public double getValue() {
        values.add(wrapped.getValue());
        if (values.size() > 5) values.remove(0);
        return values.stream().mapToDouble(v->v).average().orElse(0);
    }
}
```

### 4.2 Adapter
**Uso:** Integrar servicios externos con interfaces diferentes.

**Implementación:**
```java
// Interfaz objetivo
public interface ExternalServiceAdapter {
    String sendMessage(String to, String message);
    String getStatus();
}

// Adapter para WhatsApp GreenAPI
public class GreenAPIAdapter implements ExternalServiceAdapter {
    private GreenAPIWhatsAppService service;
    public String sendMessage(String to, String message) {
        return service.sendMessage(to, message);
    }
    public String getStatus() {
        return service.isAvailable() ? "Activo" : "Inactivo";
    }
}

// Adapter para nueva API de WhatsApp (futuro)
public class TwilioAdapter implements ExternalServiceAdapter {
    public String sendMessage(String to, String message) { ... }
    public String getStatus() { ... }
}
```

### 4.3 Bridge
**Uso:** Separar abstracción de implementación (sensores vs protocolos de comunicación).

**Implementión:**
```java
// Implementor (protocolo de comunicación)
public interface SensorProtocol {
    String transmitData(String data);
    boolean isConnected();
}

// Implementor concreto: WiFi
public class WiFiProtocol implements SensorProtocol {
    public String transmitData(String data) { return "WiFi: " + data; }
    public boolean isConnected() { return true; }
}

// Implementor concreto: LoRa
public class LoRaProtocol implements SensorProtocol {
    public String transmitData(String data) { return "LoRa: " + data; }
    public boolean isConnected() { return true; }
}

// Abstracción
public abstract class SensorBridge {
    protected SensorProtocol protocol;
    public SensorBridge(SensorProtocol p) { this.protocol = p; }
    public abstract String readAndTransmit();
}

// Abstracción refinada
public class TemperatureSensorBridge extends SensorBridge {
    private double temperature;
    public TemperatureSensorBridge(SensorProtocol p) { super(p); }
    public String readAndTransmit() {
        temperature = readSensor();
        return protocol.transmitData("Temp:" + temperature);
    }
    private double readSensor() { return 25.5; }
}
```

### 4.4 Prototype
**Uso:** Clonar configuraciones de invernaderos.

**Implementación:**
```java
public class Greenhouse implements Cloneable {
    private String name;
    private List<Crop> crops;
    private Map<String, Double> sensorRanges;
    
    public Greenhouse clone() {
        Greenhouse copy = new Greenhouse();
        copy.name = this.name + "-copy";
        copy.crops = new LinkedList<>(this.crops);
        copy.sensorRanges = new HashMap<>(this.sensorRanges);
        return copy;
    }
}

// Uso en GreenhouseController
public Greenhouse cloneGreenhouse(Greenhouse original) {
    return original.clone();
}
```

### 4.5 Builder
**Uso:** Construir objetos complejos (cultivos, alertas, lecturas).

**Implementación:**
```java
public class CropBuilder {
    private String name = "";
    private String variety = "";
    private double tempMin = 15, tempMax = 30;
    private double humidityMin = 50, humidityMax = 80;
    private double soilMin = 40, soilMax = 70;
    
    public CropBuilder name(String n) { this.name = n; return this; }
    public CropBuilder variety(String v) { this.variety = v; return this; }
    public CropBuilder tempRange(double min, double max) { 
        this.tempMin = min; this.tempMax = max; return this; 
    }
    public CropBuilder humidityRange(double min, double max) {
        this.humidityMin = min; this.humidityMax = max; return this;
    }
    public CropBuilder soilRange(double min, double max) {
        this.soilMin = min; this.soilMax = max; return this;
    }
    public Crop build() {
        return new Crop(name, variety, tempMin, tempMax, humidityMin, humidityMax, soilMin, soilMax);
    }
}

// Uso: new CropBuilder().name("Tomate").variety("Cherry").tempRange(18,25).build()
```

---

## 5. Estructuras de datos (implementar)

### 5.1 Lista Ligada (LinkedList)
**Uso:** Gestionar lecturas de sensores con inserción O(1).

**Implementación:**
```java
public class LinkedList<T> {
    private Node<T> head;
    private int size;
    
    private class Node<T> {
        T data;
        Node<T> next;
        Node(T data) { this.data = data; }
    }
    
    public void add(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) { head = newNode; }
        else {
            Node<T> current = head;
            while (current.next != null) current = current.next;
            current.next = newNode;
        }
        size++;
    }
    
    public T get(int index) {
        if (index < 0 || index >= size) return null;
        Node<T> current = head;
        for (int i = 0; i < index; i++) current = current.next;
        return current.data;
    }
    
    public int size() { return size; }
}
```

### 5.2 Pila (Stack)
**Uso:** Historial de alertas (LIFO).

**Implementación:**
```java
public class Stack<T> {
    private LinkedList<T> list = new LinkedList<>();
    
    public void push(T item) { list.add(item); }
    public T pop() {
        if (list.size() == 0) return null;
        T item = list.get(list.size() - 1);
        // remover último
        return item;
    }
    public T peek() { 
        return list.size() > 0 ? list.get(list.size() - 1) : null; 
    }
    public boolean isEmpty() { return list.size() == 0; }
}
```

### 5.3 Cola (Queue)
**Uso:** Procesamiento de eventos de sensores (FIFO).

**Implementación:**
```java
public class Queue<T> {
    private LinkedList<T> list = new LinkedList<>();
    
    public void enqueue(T item) { list.add(item); }
    public T dequeue() {
        if (list.size() == 0) return null;
        T item = list.get(0);
        // remover primero (implementar en LinkedList)
        return item;
    }
    public T front() { return list.get(0); }
    public boolean isEmpty() { return list.size() == 0; }
}
```

### 5.4 Array (implementación)
**Uso:** Almacenamiento fijo de sensores/actuadores.

**Implementación:**
```java
public class SensorArray {
    private Sensor[] array;
    private int count;
    private static final int MAX = 10;
    
    public SensorArray() {
        array = new Sensor[MAX];
        count = 0;
    }
    
    public void add(Sensor s) {
        if (count < MAX) array[count++] = s;
    }
    
    public Sensor get(int index) {
        if (index >= 0 && index < count) return array[index];
        return null;
    }
    
    public int size() { return count; }
}
```

---

## 6. Commits en español

Cada cambio pequeño = un commit con mensaje descriptivo en español.

---

**Este diseño incluye TODOS los patrones y estructuras. ¿Lo approves para proceder?**