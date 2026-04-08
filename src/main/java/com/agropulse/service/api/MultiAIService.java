package com.agropulse.service.api;

import com.agropulse.model.Crop;
import com.agropulse.model.SensorReading;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Orquestador de múltiples servicios de IA que los ejecuta en PARALELO.
 *
 * Implementa el patrón Strategy + Composite:
 * - Consulta TODOS los servicios activos simultáneamente (ExecutorService).
 * - Consolida las respuestas en un reporte unificado.
 * - Si solo un servicio está activo, devuelve su respuesta directamente.
 * - Si ninguno está activo, devuelve un mensaje informativo.
 *
 * Servicios integrados (gratuitos):
 *   ✅ Groq (LLaMA3 ultra-rápido)
 *   ✅ Ollama (modelos locales)
 *   ✅ GitHub Models (phi-4-mini, gratis)
 */
public class MultiAIService implements AIService {

    private final Map<String, AIService> services = new LinkedHashMap<>();
    private final ExecutorService executor;

    // Timeout por servicio (segundos) — Groq y Ollama son los extremos
    private static final int TIMEOUT_SECONDS = 60;

    // Constructor con solo las 3 APIs funcionales
    public MultiAIService(GroqService groq, OllamaService ollama, GitHubModelsService github) {
        if (groq    != null) services.put("⚡ Groq",       groq);
        if (ollama != null) services.put("💻 Ollama",     ollama);
        if (github != null) services.put("🐙 GitHub",     github);

        this.executor = Executors.newFixedThreadPool(Math.max(services.size(), 1));
    }

    // ─── AIService interface ──────────────────────────────────────────

    @Override
    public String getCropRecommendation(Crop crop, List<SensorReading> readings) {
        return runParallel(svc -> svc.getCropRecommendation(crop, readings));
    }

    @Override
    public String predictActuatorNeeds(List<SensorReading> readings) {
        return runParallel(svc -> svc.predictActuatorNeeds(readings));
    }

    @Override
    public String analyzeGreenhouseStatus(List<SensorReading> readings) {
        return runParallel(svc -> svc.analyzeGreenhouseStatus(readings));
    }

    @Override
    public boolean isAvailable() {
        return services.values().stream().anyMatch(AIService::isAvailable);
    }

    @Override
    public String getProviderName() {
        long count = services.values().stream().filter(AIService::isAvailable).count();
        return "Multi-IA (" + count + " servicios activos)";
    }

    // ─── Gestión de servicios ─────────────────────────────────────────

    /** Devuelve todos los servicios registrados (activos o no). */
    public Map<String, AIService> getAllServices() {
        return Collections.unmodifiableMap(services);
    }

    /** Devuelve solo los servicios disponibles en este momento. */
    public Map<String, AIService> getActiveServices() {
        return services.entrySet().stream()
                .filter(e -> e.getValue().isAvailable())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /** Número de servicios activos. */
    public int activeCount() {
        return (int) services.values().stream().filter(AIService::isAvailable).count();
    }

    /** Apagar el pool de hilos al cerrar la aplicación. */
    public void shutdown() {
        executor.shutdownNow();
    }

    // ─── Motor paralelo ───────────────────────────────────────────────

    @FunctionalInterface
    private interface AICall {
        String call(AIService service) throws Exception;
    }

    /**
     * Ejecuta la llamada en paralelo sobre todos los servicios activos.
     * Consolida los resultados en un único String formateado.
     */
    private String runParallel(AICall call) {
        Map<String, AIService> active = getActiveServices();

        if (active.isEmpty()) {
            return "⚠️  Ningún servicio de IA está activo.\n\n"
                 + "Ve a  🔌 Configurar APIs  y activa al menos un proveedor:\n"
                 + "  • Groq    — necesita API key (gsk_...)\n"
                 + "  • GitHub  — necesita PAT token\n"
                 + "  • Ollama  — local, gratis (ollama serve)";
        }

        // Si hay un solo servicio activo, respuesta directa sin overhead
        if (active.size() == 1) {
            Map.Entry<String, AIService> entry = active.entrySet().iterator().next();
            try {
                return entry.getKey() + "\n" + "─".repeat(40) + "\n"
                     + call.call(entry.getValue());
            } catch (Exception e) {
                return "[Error] " + entry.getKey() + ": " + e.getMessage();
            }
        }

        // Múltiples servicios: ejecutar en paralelo
        List<Future<String>> futures = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (Map.Entry<String, AIService> entry : active.entrySet()) {
            final String name = entry.getKey();
            final AIService svc = entry.getValue();
            names.add(name);
            futures.add(executor.submit(() -> {
                try {
                    return call.call(svc);
                } catch (Exception e) {
                    return "[Error de conexión: " + e.getMessage() + "]";
                }
            }));
        }

        // Recolectar resultados con timeout individual
        StringBuilder sb = new StringBuilder();
        sb.append("🧠 RESPUESTAS DE ").append(active.size()).append(" IAs EN PARALELO\n");
        sb.append("═".repeat(50)).append("\n\n");

        for (int i = 0; i < futures.size(); i++) {
            String name = names.get(i);
            sb.append(name).append("\n");
            sb.append("─".repeat(40)).append("\n");
            try {
                String result = futures.get(i).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                sb.append(result != null ? result : "[Sin respuesta]");
            } catch (TimeoutException e) {
                sb.append("[Tiempo agotado — el servicio tardó más de ")
                  .append(TIMEOUT_SECONDS).append("s]");
            } catch (Exception e) {
                sb.append("[Error: ").append(e.getMessage()).append("]");
            }
            sb.append("\n\n");
        }

        return sb.toString().trim();
    }
}
