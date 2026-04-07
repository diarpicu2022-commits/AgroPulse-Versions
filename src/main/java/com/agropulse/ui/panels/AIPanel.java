package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.service.api.AIService;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/**
 * Panel Consultar IA — Multi-proveedor en paralelo.
 *
 * Muestra el estado de cada proveedor de IA en tiempo real
 * y permite lanzar consultas que se ejecutan en paralelo.
 */
public class AIPanel extends javax.swing.JPanel {

    private final GreenhouseController controller;

    public AIPanel(GreenhouseController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Encabezado ──────────────────────────────────────────
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);

        lblTitle = new JLabel("🤖 Consultar Inteligencia Artificial (Multi-IA)");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        // ── Estado de proveedores ────────────────────────────────
        pnlStatus = buildStatusPanel();

        // ── Botones ──────────────────────────────────────────────
        pnlButtons = new JPanel(new GridLayout(1, 3, 12, 0));
        pnlButtons.setBackground(Color.WHITE);
        pnlButtons.setBorder(AppTheme.cardBorder());

        btnRecom    = buildAIBtn("💡 Recomendación", "Consejos para las condiciones actuales");
        btnPredict  = buildAIBtn("📊 Predicción",    "Predecir el comportamiento del invernadero");
        btnAnalysis = buildAIBtn("🔍 Análisis",      "Análisis completo del estado del sistema");

        btnRecom.addActionListener(e    -> queryAI("recommendation"));
        btnPredict.addActionListener(e  -> queryAI("prediction"));
        btnAnalysis.addActionListener(e -> queryAI("analysis"));

        pnlButtons.add(btnRecom);
        pnlButtons.add(btnPredict);
        pnlButtons.add(btnAnalysis);

        // ── Área de salida ───────────────────────────────────────
        txtOutput = new JTextArea();
        txtOutput.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtOutput.setEditable(false);
        txtOutput.setLineWrap(true);
        txtOutput.setWrapStyleWord(true);
        txtOutput.setBorder(new EmptyBorder(12, 14, 12, 14));
        txtOutput.setBackground(new Color(0xFAFAFA));
        txtOutput.setForeground(AppTheme.TEXT_PRIMARY);
        txtOutput.setText(buildWelcomeText());

        pnlOutput = new JPanel(new BorderLayout());
        pnlOutput.setBackground(Color.WHITE);
        pnlOutput.setBorder(AppTheme.cardBorder());
        lblOutputTitle = new JLabel("  📝 Respuestas de las IAs");
        lblOutputTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblOutputTitle.setForeground(AppTheme.PRIMARY_DARK);
        lblOutputTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        pnlOutput.add(lblOutputTitle, BorderLayout.NORTH);
        pnlOutput.add(new JScrollPane(txtOutput), BorderLayout.CENTER);

        // ── Layout ───────────────────────────────────────────────
        pnlTop = new JPanel(new BorderLayout(0, 10));
        pnlTop.setBackground(AppTheme.BG_MAIN);
        pnlTop.add(pnlHeader,  BorderLayout.NORTH);
        pnlTop.add(pnlStatus,  BorderLayout.CENTER);
        pnlTop.add(pnlButtons, BorderLayout.SOUTH);

        add(pnlTop,    BorderLayout.NORTH);
        add(pnlOutput, BorderLayout.CENTER);
    }

    /** Construye el panel de estado de cada proveedor de IA. */
    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        panel.setBackground(new Color(0xF3E5F5));
        panel.setBorder(new EmptyBorder(6, 12, 6, 12));

        Map<String, AIService> services = controller.getMultiAIService().getAllServices();
        if (services.isEmpty()) {
            panel.add(new JLabel("⚠️  Sin servicios de IA registrados"));
            return panel;
        }

        for (Map.Entry<String, AIService> entry : services.entrySet()) {
            boolean avail = entry.getValue().isAvailable();
            String icon   = avail ? "✅" : "⭕";
            JLabel lbl    = new JLabel(icon + " " + entry.getKey());
            lbl.setFont(AppTheme.FONT_SMALL);
            lbl.setForeground(avail ? new Color(0x2E7D32) : new Color(0x757575));
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(avail ? new Color(0xA5D6A7) : new Color(0xBDBDBD), 1, true),
                    new EmptyBorder(3, 8, 3, 8)));
            panel.add(lbl);
        }

        int active = controller.getMultiAIService().activeCount();
        JLabel lblCount = new JLabel(" → " + active + " activas");
        lblCount.setFont(AppTheme.FONT_SMALL);
        lblCount.setForeground(active > 0 ? AppTheme.PRIMARY_DARK : AppTheme.TEXT_SECONDARY);
        panel.add(lblCount);

        return panel;
    }

    private String buildWelcomeText() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nPresiona un botón para consultar todas las IAs activas en PARALELO.\n\n");
        sb.append("  • Recomendación: consejos para las condiciones actuales del cultivo\n");
        sb.append("  • Predicción:    qué puede pasar en las próximas horas\n");
        sb.append("  • Análisis:      reporte completo del estado del invernadero\n\n");

        Map<String, AIService> services = controller.getMultiAIService().getAllServices();
        sb.append("Proveedores de IA configurados:\n");
        for (Map.Entry<String, AIService> e : services.entrySet()) {
            boolean avail = e.getValue().isAvailable();
            sb.append("  ").append(avail ? "✅" : "⭕").append(" ")
              .append(e.getValue().getProviderName()).append("\n");
        }
        return sb.toString();
    }

    private void queryAI(String type) {
        if (!controller.getConfig().isAIEnabled()) {
            txtOutput.setText("\n⚠️  Ningún servicio de IA está activo.\n\n"
                + "Ve a  🔌 Configurar APIs  →  activa al menos un proveedor.\n");
            return;
        }

        int active = controller.getMultiAIService().activeCount();
        txtOutput.setText("⏳ Consultando " + active + " IA" + (active > 1 ? "s" : "") + " en paralelo...\n"
                        + "   Esto puede tardar hasta 60 segundos según los proveedores activos.");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return switch (type) {
                    case "recommendation" -> controller.getAIRecommendation();
                    case "prediction"     -> controller.getAIPrediction();
                    case "analysis"       -> controller.getAIAnalysis();
                    default               -> "Tipo de consulta desconocido.";
                };
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    String r = get();
                    txtOutput.setText(r != null && !r.isBlank() ? "\n" + r
                        : "\n⚠️  Sin respuesta. Verifica tus API Keys y conexión.");
                    txtOutput.setCaretPosition(0);
                } catch (Exception ex) {
                    txtOutput.setText("\n❌ Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private JButton buildAIBtn(String text, String tooltip) {
        JButton btn = AppTheme.primaryButton(text);
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(160, 48));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return btn;
    }

    // Variables privadas
    private javax.swing.JPanel    pnlHeader;
    private javax.swing.JPanel    pnlStatus;
    private javax.swing.JPanel    pnlButtons;
    private javax.swing.JPanel    pnlOutput;
    private javax.swing.JPanel    pnlTop;
    private javax.swing.JLabel    lblTitle;
    private javax.swing.JLabel    lblOutputTitle;
    private javax.swing.JButton   btnRecom;
    private javax.swing.JButton   btnPredict;
    private javax.swing.JButton   btnAnalysis;
    private javax.swing.JTextArea txtOutput;
}
