package com.agropulse.controller;

import com.agropulse.config.AppConfig;
import com.agropulse.dao.*;
import com.agropulse.model.*;
import com.agropulse.model.enums.*;
import com.agropulse.pattern.factory.SensorFactory;
import com.agropulse.pattern.observer.*;
import com.agropulse.pattern.strategy.*;
import com.agropulse.service.api.*;

import com.agropulse.service.whatsapp.GreenAPIWhatsAppService;
import com.agropulse.service.whatsapp.WhatsAppService;

import java.util.*;

/**
 * Controlador principal del invernadero.
 * Orchestra sensores, actuadores, estrategias, observadores, IA y WhatsApp.
 *
 * IA: Usa MultiAIService para consultar en PARALELO:
 *   OpenAI · Gemini · Groq · Ollama · Mistral
 */
public class GreenhouseController {

    // DAOs
    private final SensorDao sensorDao;
    private final ActuatorDao actuatorDao;
    private final CropDao cropDao;
    private final SensorReadingDao readingDao;
    private final AlertDao alertDao;
    private final SystemLogDao logDao;

    // Patrón Observer
    private final GreenhouseSubject subject;

    // Datos en memoria (cargados de BD)
    private final List<Sensor> sensors;
    private final List<Actuator> actuators;
    private final Map<ActuatorType, ActuatorStrategy> strategies;

    // Servicios externos
    private MultiAIService multiAIService;   // Orquestador paralelo de IAs
    private WhatsAppService whatsAppService;

    // Configuración
    private final AppConfig config;

    public GreenhouseController() {
        this.sensorDao   = new SensorDao();
        this.actuatorDao = new ActuatorDao();
        this.cropDao     = new CropDao();
        this.readingDao  = new SensorReadingDao();
        this.alertDao    = new AlertDao();
        this.logDao      = new SystemLogDao();
        this.subject     = new GreenhouseSubject();
        this.sensors     = new ArrayList<>();
        this.actuators   = new ArrayList<>();
        this.strategies  = new HashMap<>();
        this.config      = AppConfig.getInstance();

        initializeServices();
        initializeHardware();
        initializeStrategies();
        initializeObservers();
    }

    // ═══════════════════════════════════════════════════════════════
    //  INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    private void initializeServices() {
        // Solo IAs gratuitas sin tarjeta
        GroqService   groq    = new GroqService(config.getGroqKey());
        OllamaService ollama = new OllamaService(config.getOllamaHost(), config.getOllamaModel());
        GitHubModelsService github = new GitHubModelsService(config.getGitHubToken());

        groq.setEnabled(config.isGroqEnabled());
        ollama.setEnabled(config.isOllamaEnabled());
        github.setEnabled(config.isGitHubEnabled());

        this.multiAIService = new MultiAIService(groq, ollama, github);

        this.whatsAppService = new GreenAPIWhatsAppService(
                config.getGreenApiUrl(),
                config.getGreenIdInstance(),
                config.getGreenToken()
        );

        System.out.println("  [Controller] Servicios inicializados.");
        System.out.println("    IA: " + multiAIService.getProviderName());
        System.out.println("    WhatsApp: " + whatsAppService.getProviderName() +
                " - " + (whatsAppService.isAvailable() ? "Disponible" : "No configurada"));
    }

    /** Reconstruye los servicios de IA con la configuración actual (post-guardar en panel). */
    public void refreshAIServices() {
        if (multiAIService != null) multiAIService.shutdown();
        initializeServices();
    }

    public String getCropAIFill(String name, String variety) {
        String prompt = "Eres experto agrónomo. Para el cultivo '" + name + 
            "' (variedad: " + variety + "), proporciona los rangos óptimos en este formato EXACTO:\n\n" +
            "Temperatura: MIN-MAX °C\n" +
            "Humedad: MIN-MAX %\n" +
            "Humedad suelo: MIN-MAX %\n\n" +
            "Ejemplo: Temperatura: 18-25 °C | Humedad: 60-80 % | Humedad suelo: 40-60 %";

        Map<String, AIService> services = multiAIService.getAllServices();
        for (AIService svc : services.values()) {
            if (svc instanceof GroqService && svc.isAvailable()) {
                return ((GroqService) svc).callAPI(prompt);
            }
        }
        return "Groq no está disponible.";
    }

    public void calculateAndSaveSensorRanges(int greenhouseId) {
        List<Crop> crops = cropDao.findAll();
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

    public double[] calculateAverageRange(int sensorId) {
        List<Sensor> allSensors = sensorDao.findAll();
        Optional<Sensor> optSensor = allSensors.stream().filter(s -> s.getId() == sensorId).findFirst();
        if (optSensor.isEmpty()) return null;
        Sensor sensor = optSensor.get();

        List<Crop> crops = cropDao.findAll();
        if (crops.isEmpty()) return null;

        String typeName = sensor.getType().name();

        double sumMin = 0, sumMax = 0;
        int count = 0;

        for (Crop c : crops) {
            switch (typeName) {
                case "TEMPERATURE" -> {
                    sumMin += c.getTempMin();
                    sumMax += c.getTempMax();
                }
                case "HUMIDITY" -> {
                    sumMin += c.getHumidityMin();
                    sumMax += c.getHumidityMax();
                }
                case "SOIL_MOISTURE" -> {
                    sumMin += c.getSoilMoistureMin();
                    sumMax += c.getSoilMoistureMax();
                }
                default -> {
                    return null;
                }
            }
            count++;
        }

        if (count == 0) return null;
        return new double[]{sumMin / count, sumMax / count};
    }

    private void initializeHardware() {
        List<Sensor> existingSensors = sensorDao.findAll();
        if (existingSensors.isEmpty()) {
            for (SensorType type : SensorType.values()) {
                Sensor s = SensorFactory.createSensor(type);
                sensorDao.save(s);
                sensors.add(s);
            }
        } else {
            sensors.addAll(existingSensors);
        }

        List<Actuator> existingActuators = actuatorDao.findAll();
        if (existingActuators.isEmpty()) {
            for (ActuatorType type : ActuatorType.values()) {
                Actuator a = SensorFactory.createActuator(type);
                actuatorDao.save(a);
                actuators.add(a);
            }
        } else {
            actuators.addAll(existingActuators);
        }

        System.out.println("  [Controller] Hardware: " + sensors.size() +
                " sensores, " + actuators.size() + " actuadores.");
    }

    private void initializeStrategies() {
        strategies.put(ActuatorType.EXTRACTOR,      new TemperatureHighStrategy());
        strategies.put(ActuatorType.DOOR,           new TemperatureHighStrategy());
        strategies.put(ActuatorType.HEAT_GENERATOR, new TemperatureLowStrategy());
        strategies.put(ActuatorType.WATER_PUMP,     new HumidityStrategy());
        System.out.println("  [Controller] Estrategias registradas: " + strategies.size());
    }

    private void initializeObservers() {
        // Observador de alertas
        subject.addObserver(new GreenhouseObserver() {
            @Override
            public void onEvent(GreenhouseEvent event) {
                if (event.isOutOfRange()) {
                    String msg;
                    AlertLevel level;
                    if (event.isAboveMax()) {
                        msg = event.getReading().getSensorType().getDisplayName() +
                              " ALTA: " + String.format("%.1f", event.getReading().getValue()) +
                              " " + event.getReading().getSensorType().getUnit() +
                              " (Máx: " + event.getConfiguredMax() + ")";
                        level = AlertLevel.WARNING;
                    } else {
                        msg = event.getReading().getSensorType().getDisplayName() +
                              " BAJA: " + String.format("%.1f", event.getReading().getValue()) +
                              " " + event.getReading().getSensorType().getUnit() +
                              " (Mín: " + event.getConfiguredMin() + ")";
                        level = AlertLevel.WARNING;
                    }
                    Alert alert = new Alert(msg, level);
                    alertDao.save(alert);
                    if (config.isWhatsAppEnabled() && whatsAppService.isAvailable()) {
                        boolean sent = whatsAppService.sendAlert(config.getAlertPhone(), alert);
                        if (sent) { alert.setSent(true); alertDao.update(alert); }
                    }
                }
            }
            @Override public String getObserverName() { return "Generador de Alertas"; }
        });

        // Observador de actuadores
        subject.addObserver(new GreenhouseObserver() {
            @Override
            public void onEvent(GreenhouseEvent event) {
                for (Actuator actuator : actuators) {
                    ActuatorStrategy strategy = strategies.get(actuator.getType());
                    if (strategy != null) {
                        boolean changed = strategy.evaluate(event, actuator);
                        if (changed) {
                            actuatorDao.update(actuator);
                            logDao.save(new SystemLog(
                                    "ACTUADOR_CAMBIO",
                                    actuator.getName() + " → " +
                                            (actuator.isEnabled() ? "ENCENDIDO" : "APAGADO"),
                                    "SISTEMA"));
                        }
                    }
                }
            }
            @Override public String getObserverName() { return "Controlador de Actuadores"; }
        });

        // Observador de log
        subject.addObserver(new GreenhouseObserver() {
            @Override public void onEvent(GreenhouseEvent event) { readingDao.save(event.getReading()); }
            @Override public String getObserverName() { return "Registrador de Lecturas"; }
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  OPERACIONES DEL SISTEMA
    // ═══════════════════════════════════════════════════════════════

    public void processSensorReading(int sensorIndex, double value) {
        if (sensorIndex < 0 || sensorIndex >= sensors.size()) return;
        Sensor sensor = sensors.get(sensorIndex);
        sensor.setLastValue(value);
        sensorDao.update(sensor);

        double min = 0, max = 100;
        Optional<Crop> activeCrop = cropDao.findActiveCrop();
        if (activeCrop.isPresent()) {
            Crop crop = activeCrop.get();
            switch (sensor.getType()) {
                case TEMPERATURE_INTERNAL, TEMPERATURE_EXTERNAL -> { min = crop.getTempMin(); max = crop.getTempMax(); }
                case HUMIDITY     -> { min = crop.getHumidityMin();    max = crop.getHumidityMax(); }
                case SOIL_MOISTURE-> { min = crop.getSoilMoistureMin();max = crop.getSoilMoistureMax(); }
            }
        }

        SensorReading reading = new SensorReading(sensor.getId(), sensor.getType(), value);
        GreenhouseEvent event = new GreenhouseEvent(reading, min, max);
        System.out.println("  → " + event);
        subject.notifyObservers(event);
    }

    public void simulateReadings() {
        Random random = new Random();
        System.out.println("\n  ═══ Simulando lecturas de sensores ═══");
        for (int i = 0; i < sensors.size(); i++) {
            Sensor sensor = sensors.get(i);
            double value = switch (sensor.getType()) {
                case TEMPERATURE_INTERNAL -> 15 + random.nextDouble() * 25;
                case TEMPERATURE_EXTERNAL -> 5  + random.nextDouble() * 30;
                case HUMIDITY             -> 30 + random.nextDouble() * 60;
                case SOIL_MOISTURE        -> 20 + random.nextDouble() * 60;
                default                   -> random.nextDouble() * 100;
            };
            processSensorReading(i, value);
        }
    }

    public void toggleActuator(int index, String performedBy) {
        if (index < 0 || index >= actuators.size()) return;
        Actuator actuator = actuators.get(index);
        actuator.toggle();
        actuator.setAutoMode(false);
        actuatorDao.update(actuator);
        logDao.save(new SystemLog("ACTUADOR_MANUAL",
                actuator.getName() + " → " + (actuator.isEnabled() ? "ENCENDIDO" : "APAGADO") + " (Manual)",
                performedBy));
    }

    public void setAutoMode(int index, boolean auto, String performedBy) {
        if (index < 0 || index >= actuators.size()) return;
        Actuator actuator = actuators.get(index);
        actuator.setAutoMode(auto);
        actuatorDao.update(actuator);
        logDao.save(new SystemLog("ACTUADOR_MODO",
                actuator.getName() + " → Modo " + (auto ? "Automático" : "Manual"), performedBy));
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTAS CON IA (Paralelo multi-proveedor)
    // ═══════════════════════════════════════════════════════════════

    public String getAIRecommendation() {
        if (!config.isAIEnabled()) return "⚠️  Ningún servicio de IA está activo.\nVe a 🔌 Configurar APIs.";
        Optional<Crop> crop = cropDao.findActiveCrop();
        if (crop.isEmpty()) return "[Sin cultivo activo] Ingrese un cultivo primero.";
        List<SensorReading> readings = readingDao.findAll();
        return multiAIService.getCropRecommendation(crop.get(), readings);
    }

    public String getAIPrediction() {
        if (!config.isAIEnabled()) return "⚠️  Ningún servicio de IA está activo.\nVe a 🔌 Configurar APIs.";
        List<SensorReading> readings = readingDao.findAll();
        return multiAIService.predictActuatorNeeds(readings);
    }

    public String getAIAnalysis() {
        if (!config.isAIEnabled()) return "⚠️  Ningún servicio de IA está activo.\nVe a 🔌 Configurar APIs.";
        List<SensorReading> readings = readingDao.findAll();
        return multiAIService.analyzeGreenhouseStatus(readings);
    }

    // ═══════════════════════════════════════════════════════════════
    //  GETTERS
    // ═══════════════════════════════════════════════════════════════

    public List<Sensor> getSensors()           { return sensors; }
    public List<Actuator> getActuators()       { return actuators; }
    public SensorDao getSensorDao()            { return sensorDao; }
    public ActuatorDao getActuatorDao()        { return actuatorDao; }
    public CropDao getCropDao()                { return cropDao; }
    public SensorReadingDao getReadingDao()    { return readingDao; }
    public AlertDao getAlertDao()              { return alertDao; }
    public SystemLogDao getLogDao()            { return logDao; }
    public AppConfig getConfig()               { return config; }
    public MultiAIService getMultiAIService()  { return multiAIService; }
    /** Alias para compatibilidad con código que usaba getAiService() */
    public AIService getAiService()            { return multiAIService; }
    public WhatsAppService getWhatsAppService(){ return whatsAppService; }
    public GreenhouseSubject getSubject()      { return subject; }

    // ═══════════════════════════════════════════════════════════════
    //  INVERNADERO SELECCIONADO (para filtrar datos en paneles)
    // ═══════════════════════════════════════════════════════════════

    /** ID del invernadero actualmente seleccionado para ver datos. 0 = todos. */
    private int selectedGreenhouseId = 0;
    private String selectedGreenhouseName = "Todos los invernaderos";

    /** Cambia el invernadero activo. Los paneles deben refrescarse después. */
    public void setSelectedGreenhouse(int id, String name) {
        this.selectedGreenhouseId   = id;
        this.selectedGreenhouseName = (name != null && !name.isBlank()) ? name : "Todos";
    }

    public int    getSelectedGreenhouseId()   { return selectedGreenhouseId; }
    public String getSelectedGreenhouseName() { return selectedGreenhouseName; }
}
