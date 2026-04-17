package com.agropulse.service;

import com.agropulse.dao.AutomationRuleDao;
import com.agropulse.dao.SensorReadingDao;
import com.agropulse.dao.ActuatorDao;
import com.agropulse.dao.SystemLogDao;
import com.agropulse.model.AutomationRule;
import com.agropulse.model.SensorReading;
import com.agropulse.model.SystemLog;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio que ejecuta reglas de automatización basadas en valores de sensores
 * Se ejecuta cada 30 segundos verificando condiciones
 */
public class RuleExecutorService {
    private static final long EXECUTION_INTERVAL = 30_000; // 30 segundos
    private static final AutomationRuleDao ruleDao = new AutomationRuleDao();
    private static final SensorReadingDao readingDao = new SensorReadingDao();
    private static final ActuatorDao actuatorDao = new ActuatorDao();
    private static final SystemLogDao logDao = new SystemLogDao();
    private static volatile boolean running = false;
    private static Thread executorThread;

    /**
     * Inicia el servicio de ejecución de reglas
     */
    public static synchronized void start() {
        if (running) {
            System.out.println("RuleExecutorService ya está corriendo");
            return;
        }
        running = true;
        executorThread = new Thread(() -> {
            System.out.println("✅ RuleExecutorService iniciado");
            while (running) {
                try {
                    executeAllRules();
                    Thread.sleep(EXECUTION_INTERVAL);
                } catch (InterruptedException e) {
                    System.err.println("RuleExecutorService interrumpido: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    System.err.println("Error en RuleExecutorService: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, "RuleExecutorService");
        executorThread.setDaemon(true);
        executorThread.start();
    }

    /**
     * Detiene el servicio de ejecución de reglas
     */
    public static synchronized void stop() {
        running = false;
        if (executorThread != null) {
            executorThread.interrupt();
        }
        System.out.println("❌ RuleExecutorService detenido");
    }

    /**
     * Ejecuta todas las reglas habilitadas
     */
    private static void executeAllRules() {
        try {
            List<AutomationRule> enabledRules = ruleDao.findAllEnabled();
            
            for (AutomationRule rule : enabledRules) {
                try {
                    executeRule(rule);
                } catch (SQLException e) {
                    System.err.println("Error ejecutando regla " + rule.getId() + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error obteniendo reglas: " + e.getMessage());
        }
    }

    /**
     * Ejecuta una regla específica comparando valores de sensores
     */
    private static void executeRule(AutomationRule rule) throws SQLException {
        // Obtener última lectura relevante
        SensorReading lastReading = getLastRelevantReading(rule.getConditionType());
        
        if (lastReading == null) {
            return; // Sin datos de sensor
        }

        boolean conditionMet = evaluateCondition(rule.getConditionType(), 
                                                  rule.getConditionValue(), 
                                                  lastReading.getValue());

        if (conditionMet) {
            executeAction(rule);
            ruleDao.updateLastExecuted(rule.getId(), LocalDateTime.now());
            
            logDao.save(new SystemLog(
                "RULE_EXECUTED",
                "Regla " + rule.getId() + " ejecutada: " + rule.getConditionType() + " → " + rule.getActionType(),
                rule.getUsername()
            ));
        }
    }

    /**
     * Obtiene la última lectura del tipo de sensor relevante para la condición
     */
    private static SensorReading getLastRelevantReading(String conditionType) throws SQLException {
        com.agropulse.model.enums.SensorType sensorType = switch (conditionType) {
            case "temp_high", "temp_low" -> com.agropulse.model.enums.SensorType.TEMPERATURE_INTERNAL;
            case "humidity_high", "humidity_low" -> com.agropulse.model.enums.SensorType.HUMIDITY;
            case "soil_dry" -> com.agropulse.model.enums.SensorType.SOIL_MOISTURE;
            default -> null;
        };

        if (sensorType == null) return null;

        // Obtener última lectura de este tipo de sensor
        List<SensorReading> readings = readingDao.findByType(sensorType, 10);
        return readings.isEmpty() ? null : readings.get(0);
    }

    /**
     * Evalúa si la condición se cumple
     */
    private static boolean evaluateCondition(String conditionType, double threshold, double currentValue) {
        return switch (conditionType) {
            case "temp_high" -> currentValue > threshold;
            case "temp_low" -> currentValue < threshold;
            case "humidity_high" -> currentValue > threshold;
            case "humidity_low" -> currentValue < threshold;
            case "soil_dry" -> currentValue < threshold;
            default -> false;
        };
    }

    /**
     * Ejecuta la acción asociada a la regla
     */
    private static void executeAction(AutomationRule rule) {
        try {
            switch (rule.getActionType()) {
                case "activate_extractor" -> {
                    // Activar actuador "extractor" (ventilador)
                    System.out.println("🌀 Activando extractor para " + rule.getUsername());
                    setActuatorState(rule.getUsername(), "extractor", true);
                }
                case "activate_pump" -> {
                    // Activar bomba de agua
                    System.out.println("💦 Activando bomba de agua para " + rule.getUsername());
                    setActuatorState(rule.getUsername(), "pump", true);
                }
                case "open_door" -> {
                    System.out.println("🚪 Abriendo puerta para " + rule.getUsername());
                    setActuatorState(rule.getUsername(), "door", true);
                }
                case "close_door" -> {
                    System.out.println("🚪 Cerrando puerta para " + rule.getUsername());
                    setActuatorState(rule.getUsername(), "door", false);
                }
            }
        } catch (Exception e) {
            System.err.println("Error ejecutando acción: " + e.getMessage());
        }
    }

    /**
     * Activa/desactiva un actuador
     */
    private static void setActuatorState(String username, String actuatorName, boolean enabled) throws SQLException {
        // En producción: Buscar actuador por nombre y username, actualizar su estado
        // Por ahora: Log solamente
        System.out.println("  → " + actuatorName + " = " + (enabled ? "ON" : "OFF"));
    }

    public static boolean isRunning() {
        return running;
    }
}
