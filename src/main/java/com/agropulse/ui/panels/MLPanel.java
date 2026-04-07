package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.ml.SensorPredictor;
import com.agropulse.model.Crop;
import com.agropulse.model.SensorReading;
import com.agropulse.model.enums.SensorType;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Panel de Machine Learning Local.
 * Muestra predicciones, suavizado EMA y detección de anomalías.
 * No requiere internet ni APIs externas.
 */
public class MLPanel extends JPanel {

    private final GreenhouseController controller;
    private JTextArea  txtReport;
    private JLabel     lblStatus;
    private JButton    btnAnalyze;

    public MLPanel(GreenhouseController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Título
        JLabel title = new JLabel("🧠 Machine Learning Local");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.PRIMARY_DARK);

        // Info banner
        JPanel pnlInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlInfo.setBackground(new Color(0xE8F5E9));
        pnlInfo.setBorder(new EmptyBorder(6, 12, 6, 12));
        JLabel info = new JLabel("⚡ Funciona 100% offline — Regresión Lineal + EMA + Detección de Anomalías (Z-Score)");
        info.setFont(AppTheme.FONT_SMALL);
        info.setForeground(new Color(0x1B5E20));
        pnlInfo.add(info);

        JPanel pnlHead = new JPanel(new BorderLayout(0, 6));
        pnlHead.setBackground(AppTheme.BG_MAIN);
        pnlHead.add(title,   BorderLayout.NORTH);
        pnlHead.add(pnlInfo, BorderLayout.SOUTH);

        // Botones
        btnAnalyze = AppTheme.primaryButton("🔍 Analizar y Predecir");
        btnAnalyze.setPreferredSize(new Dimension(200, 38));
        btnAnalyze.addActionListener(e -> runAnalysis());

        JButton btnAuto = AppTheme.secondaryButton("⏱ Auto cada 30s");
        btnAuto.addActionListener(e -> toggleAuto(btnAuto));

        lblStatus = new JLabel("  Presiona 'Analizar' para iniciar.");
        lblStatus.setFont(AppTheme.FONT_SMALL);
        lblStatus.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        pnlBtns.setBackground(Color.WHITE);
        pnlBtns.setBorder(AppTheme.cardBorder());
        pnlBtns.add(btnAnalyze);
        pnlBtns.add(btnAuto);
        pnlBtns.add(lblStatus);

        // Área de reporte
        txtReport = new JTextArea();
        txtReport.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtReport.setEditable(false);
        txtReport.setBackground(new Color(0xF8FFF8));
        txtReport.setForeground(new Color(0x1B2E1B));
        txtReport.setBorder(new EmptyBorder(12, 14, 12, 14));
        txtReport.setText(
            "📊 MOTOR DE ML LOCAL — LISTO\n" +
            "══════════════════════════════════════════════\n\n" +
            "Este módulo analiza los datos de los sensores\n" +
            "usando algoritmos de ML implementados en Java:\n\n" +
            "  1️⃣  Regresión Lineal → predice valores futuros\n" +
            "        (15min, 30min, 60min)\n\n" +
            "  2️⃣  Media Móvil Exponencial (EMA) → suaviza\n" +
            "        lecturas ruidosas de sensores analógicos\n\n" +
            "  3️⃣  Z-Score → detecta lecturas anómalas\n" +
            "        (fallos de sensor, condiciones extremas)\n\n" +
            "  4️⃣  Clasificador de Estado → identifica si\n" +
            "        cada sensor está en rango óptimo\n\n" +
            "Presiona 'Analizar y Predecir' para ver los resultados.\n"
        );

        JPanel pnlReport = new JPanel(new BorderLayout());
        pnlReport.setBackground(Color.WHITE);
        pnlReport.setBorder(AppTheme.cardBorder());
        JLabel lblReportTitle = new JLabel("  📈 Resultados del análisis");
        lblReportTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblReportTitle.setForeground(AppTheme.PRIMARY_DARK);
        pnlReport.add(lblReportTitle,          BorderLayout.NORTH);
        pnlReport.add(new JScrollPane(txtReport), BorderLayout.CENTER);

        add(pnlHead,    BorderLayout.NORTH);
        add(pnlBtns,    BorderLayout.CENTER);
        add(pnlReport,  BorderLayout.SOUTH);
        pnlReport.setPreferredSize(new Dimension(0, 460));
    }

    // ─── Auto-análisis ────────────────────────────────────────────────

    private javax.swing.Timer autoTimer;
    private boolean autoRunning = false;

    private void toggleAuto(JButton btn) {
        if (!autoRunning) {
            autoTimer = new javax.swing.Timer(30_000, e -> runAnalysis());
            autoTimer.start();
            autoRunning = true;
            btn.setText("⏹ Detener Auto");
            lblStatus.setText("  Auto-análisis cada 30s activo.");
            runAnalysis();
        } else {
            if (autoTimer != null) autoTimer.stop();
            autoRunning = false;
            btn.setText("⏱ Auto cada 30s");
            lblStatus.setText("  Auto-análisis detenido.");
        }
    }

    // ─── Análisis principal ───────────────────────────────────────────

    private void runAnalysis() {
        btnAnalyze.setEnabled(false);
        lblStatus.setText("  ⏳ Analizando...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    // Agrupar lecturas por tipo de sensor
                    Map<SensorType, List<SensorReading>> byType = new EnumMap<>(SensorType.class);
                    for (SensorType t : SensorType.values()) {
                        List<SensorReading> readings = controller.getReadingDao().findByType(t, 30);
                        if (!readings.isEmpty()) byType.put(t, readings);
                    }

                    if (byType.isEmpty()) {
                        return "⚠️  Sin datos de sensores.\n\nSimula lecturas primero desde '📈 Simular Lecturas'\no conecta el ESP32 desde '📟 Conectar ESP32'.";
                    }

                    // Obtener rangos del cultivo activo
                    double tempMin = 15, tempMax = 30;
                    double humMin  = 50, humMax  = 80;
                    double soilMin = 40, soilMax = 70;

                    Optional<Crop> crop = controller.getCropDao().findActiveCrop();
                    if (crop.isPresent()) {
                        Crop c = crop.get();
                        tempMin = c.getTempMin(); tempMax = c.getTempMax();
                        humMin  = c.getHumidityMin(); humMax = c.getHumidityMax();
                        soilMin = c.getSoilMoistureMin(); soilMax = c.getSoilMoistureMax();
                    }

                    return SensorPredictor.generateReport(
                        byType, tempMin, tempMax, humMin, humMax, soilMin, soilMax);

                } catch (Exception e) {
                    return "❌ Error en análisis ML: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                btnAnalyze.setEnabled(true);
                try {
                    String result = get();
                    txtReport.setText(result);
                    txtReport.setCaretPosition(0);
                    lblStatus.setText("  ✅ Análisis completado — " +
                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                } catch (Exception ex) {
                    txtReport.setText("❌ Error: " + ex.getMessage());
                    lblStatus.setText("  ❌ Error en análisis.");
                }
            }
        }.execute();
    }
}
