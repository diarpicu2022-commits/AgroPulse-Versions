# AgroPulse v9 - Plan de Implementación Completo

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar todas las mejoras: limpiar APIs, rangos automáticos, webapp, y todos los patrones/estructuras faltantes.

**Architecture:** 
- Limpiar código de APIs no usadas
- Implementar patrones Decorator, Adapter, Bridge, Prototype, Builder
- Implementar estructuras LinkedList, Stack, Queue, Array
- Actualizar webapp con mismas funcionalidades

**Tech Stack:** Java Swing, SQLite, React/Vite, Groq API, GitHub Models

---

## Task 1: Limpiar APIs no usadas del código

**Files:**
- Delete: `src/main/java/com/agropulse/service/api/OpenRouterService.java`
- Delete: `src/main/java/com/agropulse/service/api/OpenAIService.java`
- Delete: `src/main/java/com/agropulse/service/api/MistralService.java`
- Delete: `src/main/java/com/agropulse/service/api/DeepSeekService.java`
- Delete: `src/main/java/com/agropulse/service/api/GeminiService.java`
- Modify: `src/main/java/com/agropulse/service/api/MultiAIService.java`
- Modify: `src/main/java/com/agropulse/ui/panels/APIConfigPanel.java`

- [ ] **Step 1: Eliminar archivos de APIs no usadas**

Eliminar los 5 archivos de servicios de IA que no funcionan.

- [ ] **Step 2: Actualizar MultiAIService**

Modificar para usar solo las 3 APIs funcionales (Groq, GitHub, Ollama).

```java
public class MultiAIService implements AIService {
    private final Map<String, AIService> services = new LinkedHashMap<>();
    private final ExecutorService executor;

    public MultiAIService(GroqService groq, OllamaService ollama, GitHubModelsService github) {
        if (groq    != null) services.put("⚡ Groq",       groq);
        if (ollama != null) services.put("💻 Ollama",     ollama);
        if (github != null) services.put("🐙 GitHub",    github);
        this.executor = Executors.newFixedThreadPool(Math.max(services.size(), 1));
    }
}
```

- [ ] **Step 3: Actualizar APIConfigPanel**

Mostrar solo las 3 APIs funcionales en el UI.

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: eliminar APIs no usadas y mantener solo Groq, GitHub y Ollama"
```

---

## Task 2: Implementar Decorator para sensores

**Files:**
- Create: `src/main/java/com/agropulse/pattern/decorator/SensorComponent.java`
- Create: `src/main/java/com/agropulse/pattern/decorator/TemperatureSensor.java`
- Create: `src/main/java/com/agropulse/pattern/decorator/SensorDecorator.java`
- Create: `src/main/java/com/agropulse/pattern/decorator/NoiseFilterDecorator.java`
- Create: `src/main/java/com/agropulse/pattern/decorator/MovingAverageDecorator.java`

- [ ] **Step 1: Crear interfaz SensorComponent**

```java
package com.agropulse.pattern.decorator;

public interface SensorComponent {
    double getValue();
    String getType();
    long getTimestamp();
}
```

- [ ] **Step 2: Crear sensor concreto**

```java
package com.agropulse.pattern.decorator;

public class TemperatureSensor implements SensorComponent {
    private double value;
    private String type = "TEMPERATURE";
    private long timestamp;
    
    public TemperatureSensor(double value) {
        this.value = value;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override public double getValue() { return value; }
    @Override public String getType() { return type; }
    @Override public long getTimestamp() { return timestamp; }
}
```

- [ ] **Step 3: Crear Decorator base**

```java
package com.agropulse.pattern.decorator;

public abstract class SensorDecorator implements SensorComponent {
    protected SensorComponent wrapped;
    
    public SensorDecorator(SensorComponent sensor) {
        this.wrapped = sensor;
    }
    
    @Override public double getValue() { return wrapped.getValue(); }
    @Override public String getType() { return wrapped.getType(); }
    @Override public long getTimestamp() { return wrapped.getTimestamp(); }
}
```

- [ ] **Step 4: Crear NoiseFilterDecorator**

```java
package com.agropulse.pattern.decorator;

public class NoiseFilterDecorator extends SensorDecorator {
    private double lastValue;
    private static final double THRESHOLD = 10.0;
    
    public NoiseFilterDecorator(SensorComponent sensor) {
        super(sensor);
        this.lastValue = sensor.getValue();
    }
    
    @Override
    public double getValue() {
        double current = wrapped.getValue();
        if (Math.abs(current - lastValue) > THRESHOLD) {
            lastValue = current;
        }
        return lastValue;
    }
}
```

- [ ] **Step 5: Crear MovingAverageDecorator**

```java
package com.agropulse.pattern.decorator;

import java.util.LinkedList;
import java.util.List;

public class MovingAverageDecorator extends SensorDecorator {
    private List<Double> values = new LinkedList<>();
    private static final int WINDOW_SIZE = 5;
    
    public MovingAverageDecorator(SensorComponent sensor) {
        super(sensor);
    }
    
    @Override
    public double getValue() {
        values.add(wrapped.getValue());
        if (values.size() > WINDOW_SIZE) {
            values.remove(0);
        }
        return values.stream().mapToDouble(v -> v).average().orElse(0);
    }
}
```

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: implementar patrón Decorator para sensores"
```

---

## Task 3: Implementar Adapter para servicios externos

**Files:**
- Create: `src/main/java/com/agropulse/pattern/adapter/ExternalServiceAdapter.java`
- Create: `src/main/java/com/agropulse/pattern/adapter/GreenAPIAdapter.java`
- Modify: `src/main/java/com/agropulse/service/whatsapp/GreenAPIWhatsAppService.java`

- [ ] **Step 1: Crear interfaz ExternalServiceAdapter**

```java
package com.agropulse.pattern.adapter;

public interface ExternalServiceAdapter {
    String sendMessage(String to, String message);
    String getStatus();
    boolean isAvailable();
}
```

- [ ] **Step 2: Crear GreenAPIAdapter**

```java
package com.agropulse.pattern.adapter;

import com.agropulse.service.whatsapp.GreenAPIWhatsAppService;

public class GreenAPIAdapter implements ExternalServiceAdapter {
    private GreenAPIWhatsAppService service;
    
    public GreenAPIAdapter(GreenAPIWhatsAppService service) {
        this.service = service;
    }
    
    @Override
    public String sendMessage(String to, String message) {
        return service.sendMessage(to, message);
    }
    
    @Override
    public String getStatus() {
        return service.isAvailable() ? "Activo" : "Inactivo";
    }
    
    @Override
    public boolean isAvailable() {
        return service.isAvailable();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: implementar patrón Adapter para servicios externos"
```

---

## Task 4: Implementar Bridge para sensores

**Files:**
- Create: `src/main/java/com/agropulse/pattern/bridge/SensorProtocol.java`
- Create: `src/main/java/com/agropulse/pattern/bridge/WiFiProtocol.java`
- Create: `src/main/java/com/agropulse/pattern/bridge/LoRaProtocol.java`
- Create: `src/main/java/com/agropulse/pattern/bridge/SensorBridge.java`
- Create: `src/main/java/com/agropulse/pattern/bridge/TemperatureSensorBridge.java`

- [ ] **Step 1: Crear SensorProtocol (Implementor)**

```java
package com.agropulse.pattern.bridge;

public interface SensorProtocol {
    String transmitData(String data);
    boolean isConnected();
    String getProtocolName();
}
```

- [ ] **Step 2: Crear implementaciones concretas**

```java
package com.agropulse.pattern.bridge;

public class WiFiProtocol implements SensorProtocol {
    @Override
    public String transmitData(String data) {
        return "[WiFi] " + data;
    }
    
    @Override
    public boolean isConnected() {
        return true;
    }
    
    @Override
    public String getProtocolName() {
        return "WiFi";
    }
}

public class LoRaProtocol implements SensorProtocol {
    @Override
    public String transmitData(String data) {
        return "[LoRa] " + data;
    }
    
    @Override
    public boolean isConnected() {
        return true;
    }
    
    @Override
    public String getProtocolName() {
        return "LoRa";
    }
}
```

- [ ] **Step 3: Crear abstracción base**

```java
package com.agropulse.pattern.bridge;

public abstract class SensorBridge {
    protected SensorProtocol protocol;
    
    public SensorBridge(SensorProtocol protocol) {
        this.protocol = protocol;
    }
    
    public abstract String readAndTransmit();
    public boolean isConnected() {
        return protocol.isConnected();
    }
}
```

- [ ] **Step 4: Crear abstracción refinada**

```java
package com.agropulse.pattern.bridge;

public class TemperatureSensorBridge extends SensorBridge {
    private double temperature;
    
    public TemperatureSensorBridge(SensorProtocol protocol) {
        super(protocol);
    }
    
    public void setTemperature(double temp) {
        this.temperature = temp;
    }
    
    @Override
    public String readAndTransmit() {
        String data = "TEMPERATURE:" + temperature + "°C";
        return protocol.transmitData(data);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: implementar patrón Bridge para sensores"
```

---

## Task 5: Implementar Prototype para invernaderos

**Files:**
- Create: `src/main/java/com/agropulse/pattern/prototype/GreenhousePrototype.java`
- Modify: `src/main/java/com/agropulse/model/Greenhouse.java`

- [ ] **Step 1: Modificar Greenhouse para clonar**

Agregar método clone() a la clase Greenhouse.

```java
public class Greenhouse implements Cloneable {
    private String name;
    private List<Crop> crops;
    private Map<String, Double> sensorRanges;
    
    // getters y setters existentes
    
    public Greenhouse clone() {
        try {
            Greenhouse copy = (Greenhouse) super.clone();
            copy.name = this.name + "-copy";
            copy.crops = new ArrayList<>(this.crops);
            copy.sensorRanges = new HashMap<>(this.sensorRanges);
            return copy;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
```

- [ ] **Step 2: Crear interfaz Prototype**

```java
package com.agropulse.pattern.prototype;

public interface GreenhousePrototype {
    GreenhousePrototype clone();
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: implementar patrón Prototype para invernaderos"
```

---

## Task 6: Implementar Builder para cultivos

**Files:**
- Create: `src/main/java/com/agropulse/pattern/builder/CropBuilder.java`
- Modify: `src/main/java/com/agropulse/model/Crop.java`

- [ ] **Step 1: Crear CropBuilder**

```java
package com.agropulse.pattern.builder;

import com.agropulse.model.Crop;

public class CropBuilder {
    private String name = "";
    private String variety = "";
    private double tempMin = 15, tempMax = 30;
    private double humidityMin = 50, humidityMax = 80;
    private double soilMin = 40, soilMax = 70;
    
    public CropBuilder name(String name) {
        this.name = name;
        return this;
    }
    
    public CropBuilder variety(String variety) {
        this.variety = variety;
        return this;
    }
    
    public CropBuilder tempRange(double min, double max) {
        this.tempMin = min;
        this.tempMax = max;
        return this;
    }
    
    public CropBuilder humidityRange(double min, double max) {
        this.humidityMin = min;
        this.humidityMax = max;
        return this;
    }
    
    public CropBuilder soilRange(double min, double max) {
        this.soilMin = min;
        this.soilMax = max;
        return this;
    }
    
    public Crop build() {
        return new Crop(name, variety, tempMin, tempMax, humidityMin, humidityMax, soilMin, soilMax);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: implementar patrón Builder para cultivos"
```

---

## Task 7: Implementar LinkedList para lecturas de sensores

**Files:**
- Create: `src/main/java/com/agropulse/structure/LinkedList.java`
- Create: `src/main/java/com/agropulse/structure/Node.java`

- [ ] **Step 1: Crear Node**

```java
package com.agropulse.structure;

public class Node<T> {
    T data;
    Node<T> next;
    
    public Node(T data) {
        this.data = data;
    }
}
```

- [ ] **Step 2: Crear LinkedList**

```java
package com.agropulse.structure;

public class LinkedList<T> {
    private Node<T> head;
    private int size;
    
    public void add(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
        } else {
            Node<T> current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        size++;
    }
    
    public T get(int index) {
        if (index < 0 || index >= size) return null;
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }
    
    public int size() {
        return size;
    }
    
    public boolean isEmpty() {
        return size == 0;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: implementar LinkedList para lecturas de sensores"
```

---

## Task 8: Implementar Stack para historial de alertas

**Files:**
- Create: `src/main/java/com/agropulse/structure/Stack.java`

- [ ] **Step 1: Crear Stack**

```java
package com.agropulse.structure;

public class Stack<T> {
    private LinkedList<T> list = new LinkedList<>();
    
    public void push(T item) {
        list.add(item);
    }
    
    public T pop() {
        if (list.isEmpty()) return null;
        T item = list.get(list.size() - 1);
        return item;
    }
    
    public T peek() {
        if (list.isEmpty()) return null;
        return list.get(list.size() - 1);
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public int size() {
        return list.size();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: implementar Stack para historial de alertas"
```

---

## Task 9: Implementar Queue para eventos

**Files:**
- Create: `src/main/java/com/agropulse/structure/Queue.java`

- [ ] **Step 1: Crear Queue**

```java
package com.agropulse.structure;

public class Queue<T> {
    private LinkedList<T> list = new LinkedList<>();
    
    public void enqueue(T item) {
        list.add(item);
    }
    
    public T dequeue() {
        if (list.isEmpty()) return null;
        T item = list.get(0);
        return item;
    }
    
    public T front() {
        if (list.isEmpty()) return null;
        return list.get(0);
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public int size() {
        return list.size();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: implementar Queue para eventos de sensores"
```

---

## Task 10: Implementar Array para sensores

**Files:**
- Create: `src/main/java/com/agropulse/structure/SensorArray.java`

- [ ] **Step 1: Crear SensorArray**

```java
package com.agropulse.structure;

import com.agropulse.model.Sensor;

public class SensorArray {
    private Sensor[] array;
    private int count;
    private static final int MAX_SIZE = 20;
    
    public SensorArray() {
        array = new Sensor[MAX_SIZE];
        count = 0;
    }
    
    public void add(Sensor sensor) {
        if (count < MAX_SIZE) {
            array[count++] = sensor;
        }
    }
    
    public Sensor get(int index) {
        if (index >= 0 && index < count) {
            return array[index];
        }
        return null;
    }
    
    public int size() {
        return count;
    }
    
    public boolean isFull() {
        return count >= MAX_SIZE;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: implementar Array para sensores"
```

---

## Task 11: Rangos automáticos de sensores

**Files:**
- Modify: `src/main/java/com/agropulse/ui/panels/CropPanel.java`
- Modify: `src/main/java/com/agropulse/controller/GreenhouseController.java`
- Modify: `src/main/java/com/agropulse/dao/GreenhouseSensorRangeDao.java`

- [ ] **Step 1: Modificar CropPanel para calcular rangos al crear cultivo**

En el método de guardar cultivo, después de guardar:
1. Verificar si es el primer cultivo del invernadero
2. Si es primero: guardar rangos inicial
3. Si ya existen: recalcular promedio

```java
// Después de guardar cultivo
int greenhouseId = 1; // obtener del contexto
GreenhouseSensorRangeDao rangeDao = new GreenhouseSensorRangeDao();
List<GreenhouseSensorRange> existing = rangeDao.findByGreenhouse(greenhouseId);

if (existing.isEmpty()) {
    // Primer cultivo: inicializar rangos
    rangeDao.save(new GreenhouseSensorRange(greenhouseId, "TEMPERATURE", crop.getTempMin(), crop.getTempMax()));
    rangeDao.save(new GreenhouseSensorRange(greenhouseId, "HUMIDITY", crop.getHumidityMin(), crop.getHumidityMax()));
    rangeDao.save(new GreenhouseSensorRange(greenhouseId, "SOIL_MOISTURE", crop.getSoilMoistureMin(), crop.getSoilMoistureMax()));
} else {
    // Recalcular promedio
    controller.calculateAndSaveSensorRanges(greenhouseId);
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: implementar rangos automáticos de sensores por invernadero"
```

---

## Task 12: Actualizar webapp

**Files:**
- Modify: `webapp/src/services/ai.ts`
- Modify: `webapp/src/App.jsx`
- Modify: `webapp/.env`

- [ ] **Step 1: Actualizar servicio de IA en webapp**

Agregar GitHub Models y Groq al servicio de IA.

- [ ] **Step 2: Actualizar App.jsx**

Agregar panel de cultivos con auto-fill de IA.

- [ ] **Step 3: Configurar variables de entorno**

GROQ_API_KEY y GITHUB_TOKEN en webapp/.env

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: actualizar webapp con IAs funcionales y auto-fill de cultivos"
```

---

## Ejecución recomendada

**Elige una opción:**

1. **Subagent-Driven (recommended)** - Dispacho un subagente por cada tarea

2. **Inline Execution** - Ejecutar tareas en esta sesión

**¿Cuál prefieres?**