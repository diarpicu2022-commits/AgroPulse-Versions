package com.agropulse.ml;

import com.agropulse.model.SensorReading;
import com.agropulse.model.enums.SensorType;

import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  AgroPulse ML — Motor de Machine Learning Local         ║
 * ║  Sin dependencias externas. Corre 100% offline.         ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║                                                          ║
 * ║  ALGORITMOS IMPLEMENTADOS:                               ║
 * ║    1. Regresión Lineal Simple (predicción de tendencia) ║
 * ║    2. Media Móvil Exponencial (suavizado de señal)      ║
 * ║    3. Detección de Anomalías (Z-Score)                  ║
 * ║    4. Clasificador de Estado (umbral adaptativo)        ║
 * ║                                                          ║
 * ╚══════════════════════════════════════════════════════════╝
 */
public class SensorPredictor {

    // ─── Predicción con regresión lineal ─────────────────────────────

    /**
     * Predice el valor de un sensor en N minutos usando regresión lineal
     * sobre las últimas lecturas.
     *
     * @param readings Lecturas históricas (mínimo 3)
     * @param minutesAhead Minutos hacia el futuro
     * @return Valor predicho, o NaN si no hay suficientes datos
     */
    public static double predictNextValue(List<SensorReading> readings, int minutesAhead) {
        if (readings == null || readings.size() < 3) return Double.NaN;

        int n = Math.min(readings.size(), 20); // Usar máximo 20 últimas lecturas
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = i;
            y[i] = readings.get(readings.size() - n + i).getValue();
        }

        // Calcular regresión lineal: y = a*x + b
        double[] coeff = linearRegression(x, y);
        double slope     = coeff[0]; // pendiente
        double intercept = coeff[1]; // intercepto

        // Predecir para x = n + minutesAhead (normalizado)
        double futureX = n + (double) minutesAhead / 5.0; // 5 min por paso
        return slope * futureX + intercept;
    }

    /**
     * Predice valores para los próximos 15, 30 y 60 minutos.
     */
    public static Map<String, Double> predictMultiple(List<SensorReading> readings) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("15min", predictNextValue(readings, 15));
        result.put("30min", predictNextValue(readings, 30));
        result.put("60min", predictNextValue(readings, 60));
        return result;
    }

    // ─── Media Móvil Exponencial ──────────────────────────────────────

    /**
     * Suaviza lecturas ruidosas con EMA (alpha = 0.3).
     * Útil para eliminar picos de ruido en sensores analógicos.
     */
    public static List<Double> exponentialMovingAverage(List<Double> values, double alpha) {
        List<Double> ema = new ArrayList<>();
        if (values.isEmpty()) return ema;
        double prev = values.get(0);
        ema.add(prev);
        for (int i = 1; i < values.size(); i++) {
            prev = alpha * values.get(i) + (1 - alpha) * prev;
            ema.add(prev);
        }
        return ema;
    }

    // ─── Detección de Anomalías (Z-Score) ────────────────────────────

    /**
     * Detecta si una lectura es anómala usando Z-Score.
     * Una lectura es anómala si |Z| > 2.5 (estándar).
     *
     * @return true si la lectura es una anomalía
     */
    public static boolean isAnomaly(List<SensorReading> history, double newValue, double threshold) {
        if (history == null || history.size() < 5) return false;

        double[] vals = history.stream().mapToDouble(SensorReading::getValue).toArray();
        double mean   = mean(vals);
        double stdDev = stdDev(vals);

        if (stdDev < 0.001) return false; // Sin variación → no hay anomalía
        double zScore = Math.abs((newValue - mean) / stdDev);
        return zScore > threshold;
    }

    // ─── Clasificador de Estado ───────────────────────────────────────

    /**
     * Clasifica el estado actual del sensor respecto a los rangos del cultivo.
     */
    public enum SensorState { CRITICAL_LOW, LOW, OPTIMAL, HIGH, CRITICAL_HIGH }

    public static SensorState classifyState(double value, double min, double max) {
        double range   = max - min;
        double bufLow  = min - range * 0.1;
        double bufHigh = max + range * 0.1;

        if (value < bufLow)  return SensorState.CRITICAL_LOW;
        if (value < min)     return SensorState.LOW;
        if (value > bufHigh) return SensorState.CRITICAL_HIGH;
        if (value > max)     return SensorState.HIGH;
        return SensorState.OPTIMAL;
    }

    // ─── Reporte completo de predicciones ────────────────────────────

    /**
     * Genera un reporte de texto con predicciones y estado para todos los sensores.
     */
    public static String generateReport(
            Map<SensorType, List<SensorReading>> readingsByType,
            double tempMin, double tempMax,
            double humMin, double humMax,
            double soilMin, double soilMax) {

        StringBuilder sb = new StringBuilder();
        sb.append("📊 ANÁLISIS ML LOCAL — PREDICCIONES\n");
        sb.append("═".repeat(46)).append("\n\n");

        appendSensorReport(sb, readingsByType.get(SensorType.TEMPERATURE_INTERNAL),
                "🌡️ Temperatura Interior", "°C", tempMin, tempMax);
        appendSensorReport(sb, readingsByType.get(SensorType.HUMIDITY),
                "💧 Humedad Relativa", "%", humMin, humMax);
        appendSensorReport(sb, readingsByType.get(SensorType.SOIL_MOISTURE),
                "🌱 Humedad del Suelo", "%", soilMin, soilMax);

        sb.append("\n⚡ Motor: Regresión Lineal + EMA + Z-Score (local, sin internet)");
        return sb.toString();
    }

    private static void appendSensorReport(StringBuilder sb, List<SensorReading> readings,
                                            String name, String unit, double min, double max) {
        if (readings == null || readings.isEmpty()) {
            sb.append(name).append(": sin datos\n\n");
            return;
        }

        double current = readings.get(readings.size() - 1).getValue();
        SensorState state = classifyState(current, min, max);

        // Suavizado EMA
        List<Double> raw = readings.stream().map(SensorReading::getValue).toList();
        List<Double> smooth = exponentialMovingAverage(raw, 0.3);
        double smoothed = smooth.get(smooth.size() - 1);

        // Predicciones
        Map<String, Double> preds = predictMultiple(readings);

        // Anomalía
        boolean anomaly = isAnomaly(readings, current, 2.5);

        sb.append(name).append("\n");
        sb.append("  Actual:   ").append(String.format("%.1f", current)).append(unit);
        sb.append("  (suavizado: ").append(String.format("%.1f", smoothed)).append(unit).append(")\n");
        sb.append("  Estado:   ").append(stateEmoji(state)).append("\n");
        if (anomaly) sb.append("  ⚠️  LECTURA ANÓMALA detectada\n");

        sb.append("  Pronóstico:\n");
        preds.forEach((label, val) -> {
            if (!Double.isNaN(val)) {
                String arrow = val > current ? "↗" : val < current ? "↘" : "→";
                sb.append("    ").append(label).append(": ")
                  .append(String.format("%.1f", val)).append(unit)
                  .append(" ").append(arrow).append("\n");
            }
        });
        sb.append("\n");
    }

    private static String stateEmoji(SensorState s) {
        return switch (s) {
            case CRITICAL_LOW  -> "🔴 Crítico bajo";
            case LOW           -> "🟡 Bajo";
            case OPTIMAL       -> "🟢 Óptimo";
            case HIGH          -> "🟡 Alto";
            case CRITICAL_HIGH -> "🔴 Crítico alto";
        };
    }

    // ─── Matemáticas ──────────────────────────────────────────────────

    /** Regresión lineal: devuelve [slope, intercept] */
    private static double[] linearRegression(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX  += x[i];
            sumY  += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }
        double denom = n * sumX2 - sumX * sumX;
        if (Math.abs(denom) < 1e-10) return new double[]{0, sumY / n};
        double slope     = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        return new double[]{slope, intercept};
    }

    private static double mean(double[] vals) {
        double s = 0; for (double v : vals) s += v; return s / vals.length;
    }

    private static double stdDev(double[] vals) {
        double m = mean(vals), s = 0;
        for (double v : vals) s += (v - m) * (v - m);
        return Math.sqrt(s / vals.length);
    }
}
