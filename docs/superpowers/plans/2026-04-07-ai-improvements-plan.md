# Modificaciones AgroPulse - Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar 3 modificaciones: 1) IAs gratuitas (Groq+Ollama+GitHub), 2) Auto-rellenar cultivo con IA, 3) Rangos por invernadero

**Architecture:** 
- Mod1: Crear GitHubModelsService y modificar MultiAIService para usar solo IAs gratuitas
- Mod2: Añadir botón en CropPanel que usa Groq para obtener rangos del cultivo
- Mod3: Nueva tabla greenhouse_sensor_ranges + modificar ConfigRangesPanel

**Tech Stack:** Java Swing, SQLite, Groq API, Ollama local, GitHub Models API

---

## Task 1: Crear GitHubModelsService (Nueva IA gratuita)

**Files:**
- Create: `src/main/java/com/agropulse/service/api/GitHubModelsService.java`

- [ ] **Step 1: Crear GitHubModelsService.java**

```java
package com.agropulse.service.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.agropulse.model.Crop;
import com.agropulse.model.SensorReading;

import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementación usando GitHub Models API.
 * Modelos gratuitos: phi-4-mini, llama-3-8b, mistral-small
 * Sin tarjeta requerida - solo login con GitHub.
 */
public class GitHubModelsService implements AIService {

    private static final String API_URL = "https://models.github.ai/v1/chat/completions";
    private static final String MODEL = "phi-4-mini";

    private final String apiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private boolean enabled;

    public GitHubModelsService(String apiKey) {
        this.apiKey = (apiKey != null && !apiKey.isBlank()) ? apiKey : "";
        this.enabled = !this.apiKey.isBlank();
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String getCropRecommendation(Crop crop, List<SensorReading> readings) {
        if (!enabled) return "[GitHub Models desactivado] Configure su PAT.";
        return callAPI(buildCropPrompt(crop, readings));
    }

    @Override
    public String predictActuatorNeeds(List<SensorReading> readings) {
        if (!enabled) return "[GitHub Models desactivado] Configure su PAT.";
        StringBuilder sb = new StringBuilder();
        sb.append("Eres experto en invernaderos. Predice actuadores necesarios:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResponde en español, conciso.");
        return callAPI(sb.toString());
    }

    @Override
    public String analyzeGreenhouseStatus(List<SensorReading> readings) {
        if (!enabled) return "[GitHub Models desactivado] Configure su PAT.";
        StringBuilder sb = new StringBuilder();
        sb.append("Analiza el estado del invernadero:\n\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nResumen y recomendaciones en español.");
        return callAPI(sb.toString());
    }

    @Override
    public boolean isAvailable() { return enabled; }

    @Override
    public String getProviderName() { return "GitHub Models (phi-4-mini)"; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled && !apiKey.isBlank();
    }

    private String buildCropPrompt(Crop crop, List<SensorReading> readings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres experto agrónomo. Cultivo: '").append(crop.getName())
          .append(" (").append(crop.getVariety()).append(")'.\n");
        sb.append("Temperatura óptima: ").append(crop.getTempMin())
          .append("–").append(crop.getTempMax()).append("°C\n");
        sb.append("Humedad óptima: ").append(crop.getHumidityMin())
          .append("–").append(crop.getHumidityMax()).append("%\n\n");
        sb.append("Lecturas actuales:\n");
        for (SensorReading r : readings) sb.append("- ").append(r.toString()).append("\n");
        sb.append("\nRecomendación concisa en español.");
        return sb.toString();
    }

    private String callAPI(String prompt) {
        try {
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.add("messages", messages);
            body.addProperty("max_tokens", 500);
            body.addProperty("temperature", 0.7);

            RequestBody requestBody = RequestBody.create(
                    gson.toJson(body), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    return json.getAsJsonArray("choices")
                               .get(0).getAsJsonObject()
                               .getAsJsonObject("message")
                               .get("content").getAsString().trim();
                } else {
                    return "[Error GitHub] Código: " + response.code();
                }
            }
        } catch (IOException e) {
            return "[Error GitHub] Conexión: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/agropulse/service/api/GitHubModelsService.java
git commit -m "feat: add GitHubModelsService for free AI"
```

---

## Task 2: Modificar MultiAIService para IAs gratuitas

**Files:**
- Modify: `src/main/java/com/agropulse/controller/GreenhouseController.java:72-88`

- [ ] **Step 1: Modificar initializeServices() en GreenhouseController**

Cambiar la creación de servicios para usar solo los gratuitos:

```java
private void initializeServices() {
    // Solo IAs gratuitas sin tarjeta
    GroqService   groq    = new GroqService(config.getGroqKey());
    OllamaService ollama = new OllamaService(config.getOllamaHost(), config.getOllamaModel());
    GitHubModelsService github = new GitHubModelsService(config.getGitHubToken());

    // Activar solo si hay key configurada
    groq.setEnabled(config.isGroqEnabled());
    ollama.setEnabled(config.isOllamaEnabled());
    github.setEnabled(config.isGitHubEnabled());

    // MultiAIService con solo las 3 gratuitas
    this.multiAIService = new MultiAIService(null, null, groq, ollama, null, github);

    this.whatsAppService = new GreenAPIWhatsAppService(
            config.getGreenApiUrl(),
            config.getGreenIdInstance(),
            config.getGreenToken()
    );
}
```

- [ ] **Step 2: Añadir métodos en AppConfig para GitHub**

En AppConfig.java, añadir:

```java
// GitHub Models
public String getGitHubToken() { return get("github_token"); }
public boolean isGitHubEnabled() { return "true".equals(get("github_enabled")); }
public void setGitHubEnabled(boolean v) { set("github_enabled", String.valueOf(v)); }
```

En initializeDefaults():

```java
initKey("github_token", "GITHUB_TOKEN", "");
initEnabled("github_enabled", "GITHUB_TOKEN", "false");
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agropulse/controller/GreenhouseController.java
git add src/main/java/com/agropulse/config/AppConfig.java
git commit -m "feat: use only free AI services (Groq, Ollama, GitHub)"
```

---

## Task 3: Añadir botón auto-rellenar cultivo en CropPanel

**Files:**
- Modify: `src/main/java/com/agropulse/ui/panels/CropPanel.java:66-106`

- [ ] **Step 1: Modificar showAddDialog() para añadir botón IA**

En CropPanel.java, cambiar showAddDialog() para añadir un botón "🤖 Auto-completar con IA" antes de los campos numéricos:

```java
private void showAddDialog() {
    JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nuevo Cultivo", true);
    dlg.setSize(400, 550);
    dlg.setLocationRelativeTo(this);

    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
    p.setBackground(Color.WHITE);
    p.setBorder(new EmptyBorder(20, 24, 20, 24));

    JTextField txName    = f(); JTextField txVariety = f();
    JTextField txTMin    = f(); JTextField txTMax = f();
    JTextField txHMin    = f(); JTextField txHMax = f();
    JTextField txSMin    = f(); JTextField txSMax = f();

    // Nombre y variedad
    p.add(new JLabel("Nombre del cultivo:")); p.add(txName);
    p.add(new JLabel("Variedad:")); p.add(txVariety);

    // Botón auto-completar
    JButton btnAuto = AppTheme.secondaryButton("🤖 Auto-completar con IA");
    btnAuto.setAlignmentX(LEFT_ALIGNMENT);
    btnAuto.addActionListener(e -> {
        String name = txName.getText().trim();
        String variety = txVariety.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(dlg, "Ingresa nombre del cultivo primero.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        btnAuto.setEnabled(false);
        btnAuto.setText("⏳ Consultando...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return controller.getCropAIFill(name, variety);
            }
            @Override protected void done() {
                btnAuto.setEnabled(true);
                btnAuto.setText("🤖 Auto-completar con IA");
                try {
                    String result = get();
                    parseAndFill(result, txTMin, txTMax, txHMin, txHMax, txSMin, txSMax);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    });
    p.add(btnAuto);
    p.add(Box.createVerticalStrut(10));

    // Resto de campos
    String[] lbls = {"Temp. mínima (°C):","Temp. máxima (°C):","Humedad mín. (%):","Humedad máx. (%):","Hum. suelo mín. (%):","Hum. suelo máx. (%):"};
    JTextField[] inps = {txTMin, txTMax, txHMin, txHMax, txSMin, txSMax};
    for (int i = 0; i < lbls.length; i++) {
        p.add(new JLabel(lbls[i])); p.add(inps[i]);
    }

    JButton btnSave = AppTheme.primaryButton("💾 Guardar Cultivo");
    btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); btnSave.setAlignmentX(LEFT_ALIGNMENT);
    btnSave.addActionListener(e -> {
        try {
            Crop crop = new Crop(txName.getText().trim(), txVariety.getText().trim(),
                d(txTMin), d(txTMax), d(txHMin), d(txHMax), d(txSMin), d(txSMax));
            controller.getCropDao().save(crop);
            controller.getLogDao().save(new SystemLog("CULTIVO","Agregado: "+crop.getName(), user.getUsername()));
            refresh(); dlg.dispose();
            JOptionPane.showMessageDialog(this, "✅ Cultivo guardado.", "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(dlg, "Valores numéricos inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    });
    p.add(btnSave);
    dlg.setContentPane(new JScrollPane(p));
    dlg.setVisible(true);
}
```

- [ ] **Step 2: Añadir método parseAndFill en CropPanel**

```java
private void parseAndFill(String result, JTextField txTMin, JTextField txTMax, 
                        JTextField txHMin, JTextField txHMax, 
                        JTextField txSMin, JTextField txSMax) {
    try {
        // Buscar números en formato "X-Y" o "X a Y"
        double[] temps = extractRange(result, "temperatura");
        double[] hums = extractRange(result, "humedad");
        double[] soils = extractRange(result, "suelo");

        if (temps != null) { txTMin.setText(String.valueOf((int)temps[0])); txTMax.setText(String.valueOf((int)temps[1])); }
        if (hums != null) { txHMin.setText(String.valueOf((int)hums[0])); txHMax.setText(String.valueOf((int)hums[1])); }
        if (soils != null) { txSMin.setText(String.valueOf((int)soils[0])); txSMax.setText(String.valueOf((int)soils[1])); }
    } catch (Exception e) {
        System.err.println("Error parseando respuesta IA: " + e.getMessage());
    }
}

private double[] extractRange(String text, String keyword) {
    // Buscar patrón como "20-25°C" o "20 a 25 grados"
    java.util.regex.Pattern p = java.util.regex.Pattern.compile(
        keyword + "[^0-9]*(\\d+)[^0-9]*[-a..]+[^0-9]*(\\d+)", 
        java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher m = p.matcher(text);
    if (m.find()) return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
    return null;
}
```

- [ ] **Step 3: Añadir método getCropAIFill en GreenhouseController**

```java
public String getCropAIFill(String name, String variety) {
    // Usar Groq para obtener rangos del cultivo
    String prompt = "Eres experto agrónomo. Para el cultivo '" + name + 
        "' (variedad: " + variety + "), proporciona los rangos óptimos en este formato EXACTO:\n\n" +
        "Temperatura: MIN-MAX °C\n" +
        "Humedad: MIN-MAX %\n" +
        "Humedad suelo: MIN-MAX %\n\n" +
        "Ejemplo: Temperatura: 18-25 °C | Humedad: 60-80 % | Humedad suelo: 40-60 %";

    GroqService groq = (GroqService) multiAIService.getAllServices().get("⚡ Groq");
    if (groq != null && groq.isAvailable()) {
        return groq.callAPI(prompt);
    }
    return "Groq no está disponible.";
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/agropulse/ui/panels/CropPanel.java
git add src/main/java/com/agropulse/controller/GreenhouseController.java
git commit -m "feat: add AI auto-fill button for new crops"
```

---

## Task 4: Crear tabla greenhouse_sensor_ranges

**Files:**
- Create: `src/main/java/com/agropulse/dao/GreenhouseSensorRangeDao.java`

- [ ] **Step 1: Crear modelo GreenhouseSensorRange**

```java
package com.agropulse.model;

public class GreenhouseSensorRange {
    private int id;
    private int greenhouseId;
    private String sensorType;
    private double rangeMin;
    private double rangeMax;

    public GreenhouseSensorRange() {}

    public GreenhouseSensorRange(int greenhouseId, String sensorType, double rangeMin, double rangeMax) {
        this.greenhouseId = greenhouseId;
        this.sensorType = sensorType;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(int id) { this.greenhouseId = id; }
    public String getSensorType() { return sensorType; }
    public void setSensorType(String t) { this.sensorType = t; }
    public double getRangeMin() { return rangeMin; }
    public void setRangeMin(double v) { this.rangeMin = v; }
    public double getRangeMax() { return rangeMax; }
    public void setRangeMax(double v) { this.rangeMax = v; }
}
```

- [ ] **Step 2: Crear DAO**

```java
package com.agropulse.dao;

import com.agropulse.model.GreenhouseSensorRange;
import com.agropulse.pattern.singleton.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GreenhouseSensorRangeDao {

    private Connection conn;

    public GreenhouseSensorRangeDao() {
        this.conn = DatabaseConnection.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS greenhouse_sensor_ranges (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "greenhouse_id INTEGER NOT NULL, " +
                "sensor_type VARCHAR(50) NOT NULL, " +
                "range_min REAL NOT NULL, " +
                "range_max REAL NOT NULL, " +
                "UNIQUE(greenhouse_id, sensor_type))");
        } catch (SQLException e) {
            System.err.println("Error creando tabla: " + e.getMessage());
        }
    }

    public void save(GreenhouseSensorRange r) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO greenhouse_sensor_ranges (greenhouse_id, sensor_type, range_min, range_max) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, r.getGreenhouseId());
            ps.setString(2, r.getSensorType());
            ps.setDouble(3, r.getRangeMin());
            ps.setDouble(4, r.getRangeMax());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error guardando rango: " + e.getMessage());
        }
    }

    public List<GreenhouseSensorRange> findByGreenhouse(int greenhouseId) {
        List<GreenhouseSensorRange> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM greenhouse_sensor_ranges WHERE greenhouse_id = ?")) {
            ps.setInt(1, greenhouseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GreenhouseSensorRange r = new GreenhouseSensorRange();
                r.setId(rs.getInt("id"));
                r.setGreenhouseId(rs.getInt("greenhouse_id"));
                r.setSensorType(rs.getString("sensor_type"));
                r.setRangeMin(rs.getDouble("range_min"));
                r.setRangeMax(rs.getDouble("range_max"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Error leyendo rangos: " + e.getMessage());
        }
        return list;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agropulse/model/GreenhouseSensorRange.java
git add src/main/java/com/agropulse/dao/GreenhouseSensorRangeDao.java
git commit -m "feat: add greenhouse_sensor_ranges table and DAO"
```

---

## Task 5: Calcular rangos promedio por invernadero

**Files:**
- Modify: `src/main/java/com/agropulse/controller/GreenhouseController.java`

- [ ] **Step 1: Añadir método para calcular rangos promedio**

```java
public void calculateAndSaveSensorRanges(int greenhouseId) {
    // Obtener cultivos del invernadero (asumimos que Crop tiene greenhouseId o se asocia de otra forma)
    List<Crop> crops = cropDao.findAll(); // Ajustar según cómo se filtren por invernadero

    if (crops.isEmpty()) return;

    double sumTempMin = 0, sumTempMax = 0;
    double sumHumMin = 0, sumHumMax = 0;
    double sumSoilMin = 0, sumSoilMax = 0;

    for (Crop c : crops) {
        sumTempMin += c.getTempMin();
        sumTempMax += c.getTempMax();
        sumHumMin += c.getHumidityMin();
        sumHumMax += c.getHumidityMax();
        sumSoilMin += c.getSoilMoistureMin();
        sumSoilMax += c.getSoilMoistureMax();
    }

    int count = crops.size();
    GreenhouseSensorRangeDao rangeDao = new GreenhouseSensorRangeDao();

    rangeDao.save(new GreenhouseSensorRange(greenhouseId, "TEMPERATURE", sumTempMin / count, sumTempMax / count));
    rangeDao.save(new GreenhouseSensorRange(greenhouseId, "HUMIDITY", sumHumMin / count, sumHumMax / count));
    rangeDao.save(new GreenhouseSensorRange(greenhouseId, "SOIL_MOISTURE", sumSoilMin / count, sumSoilMax / count));
}
```

- [ ] **Step 2: Modificar ConfigRangesPanel para usar rangos por invernadero**

En ConfigRangesPanel.java, cambiar para:
1. Obtener greenhouseId actual
2. Cargar rangos de greenhouse_sensor_ranges si existen
3. Si no existen, calcular promedio y sugerir

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/agropulse/controller/GreenhouseController.java
git commit -m "feat: calculate sensor ranges from crop averages"
```

---

## Ejecución recomendada

**Elige una opción:**

1. **Subagent-Driven (recommended)** - Dispacho un subagente por cada task, revisión entre tasks

2. **Inline Execution** - Ejecutar tasks en esta sesión usando checkpoints de revisión

**¿Cuál prefieres?**