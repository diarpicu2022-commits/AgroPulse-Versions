package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.*;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.util.List;

/** Panel Exportar Datos – compatible NetBeans */
public class DataExportPanel extends javax.swing.JPanel {

    private final GreenhouseController controller;

    public DataExportPanel(GreenhouseController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 20));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("📥 Exportar Datos");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        // Cards de exportación
        pnlCards = new JPanel(new GridLayout(1, 3, 16, 0));
        pnlCards.setBackground(AppTheme.BG_MAIN);
        pnlCards.setPreferredSize(new Dimension(0, 160));

        pnlCards.add(buildExportCard("📊 Lecturas de Sensores",
            "Exporta todas las lecturas históricas de sensores en formato CSV.", "readings"));
        pnlCards.add(buildExportCard("🔔 Alertas",
            "Exporta el historial de alertas generadas por el sistema.", "alerts"));
        pnlCards.add(buildExportCard("📋 Logs del Sistema",
            "Exporta el registro de actividad y eventos del sistema.", "logs"));

        // Área de estado
        pnlStatus = new JPanel(new BorderLayout());
        pnlStatus.setBackground(Color.WHITE);
        pnlStatus.setBorder(AppTheme.cardBorder());

        lblStatusTitle = new JLabel("  📟 Estado de la exportación");
        lblStatusTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblStatusTitle.setForeground(AppTheme.PRIMARY_DARK);
        lblStatusTitle.setBorder(new EmptyBorder(0, 0, 8, 0));

        txtStatus = new JTextArea(8, 40);
        txtStatus.setFont(AppTheme.FONT_MONOSPACE);
        txtStatus.setEditable(false);
        txtStatus.setBackground(new Color(0x1E1E1E));
        txtStatus.setForeground(new Color(0x4CAF50));
        txtStatus.setBorder(new EmptyBorder(10, 10, 10, 10));
        txtStatus.setText("Listo para exportar.\nSelecciona un tipo de exportación arriba.");

        pnlStatus.add(lblStatusTitle, BorderLayout.NORTH);
        pnlStatus.add(new JScrollPane(txtStatus), BorderLayout.CENTER);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCards,  BorderLayout.CENTER);
        add(pnlStatus, BorderLayout.SOUTH);
    }

    private JPanel buildExportCard(String title, String desc, String type) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(AppTheme.cardBorder());

        JLabel lblT = new JLabel(title);
        lblT.setFont(AppTheme.FONT_SUBTITLE);
        lblT.setForeground(AppTheme.PRIMARY_DARK);
        lblT.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lblD = new JLabel("<html><div style='width:180px'>" + desc + "</div></html>");
        lblD.setFont(AppTheme.FONT_SMALL);
        lblD.setForeground(AppTheme.TEXT_SECONDARY);
        lblD.setAlignmentX(LEFT_ALIGNMENT);

        JButton btn = AppTheme.primaryButton("💾 Exportar CSV");
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.addActionListener(e -> exportCSV(type));

        card.add(lblT);
        card.add(Box.createVerticalStrut(8));
        card.add(lblD);
        card.add(Box.createVerticalGlue());
        card.add(btn);
        return card;
    }

    private void exportCSV(String type) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar como CSV");
        fc.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        String def = switch (type) {
            case "readings" -> "lecturas_sensores.csv";
            case "alerts"   -> "alertas.csv";
            default         -> "logs_sistema.csv";
        };
        fc.setSelectedFile(new java.io.File(def));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().endsWith(".csv"))
            file = new File(file.getAbsolutePath() + ".csv");

        final File finalFile = file;
        txtStatus.setText("Exportando...\n");

        new SwingWorker<Integer, String>() {
            @Override protected Integer doInBackground() throws Exception {
                try (PrintWriter pw = new PrintWriter(new FileWriter(finalFile))) {
                    return switch (type) {
                        case "readings" -> exportReadings(pw);
                        case "alerts"   -> exportAlerts(pw);
                        default         -> exportLogs(pw);
                    };
                }
            }
            @Override protected void done() {
                try {
                    int rows = get();
                    txtStatus.setText("✅ Exportación completada.\n"
                        + "Archivo: " + finalFile.getAbsolutePath() + "\n"
                        + "Registros: " + rows + "\n");
                } catch (Exception ex) {
                    txtStatus.setText("❌ Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private int exportReadings(PrintWriter pw) {
        pw.println("id,sensor_id,sensor_nombre,valor,unidad,timestamp");
        List<com.agropulse.model.SensorReading> list = controller.getReadingDao().findAll();
        list.forEach(r -> pw.println(r.getId()+","+r.getSensorId()+","
            +csv(r.getSensorName())+","+r.getValue()+","+csv(r.getUnit())+","+r.getTimestamp()));
        return list.size();
    }

    private int exportAlerts(PrintWriter pw) {
        pw.println("id,nivel,mensaje,enviada,timestamp");
        List<Alert> list = controller.getAlertDao().findAll();
        list.forEach(a -> pw.println(a.getId()+","+a.getLevel()+","
            +csv(a.getMessage())+","+a.isSent()+","+a.getCreatedAt()));
        return list.size();
    }

    private int exportLogs(PrintWriter pw) {
        pw.println("id,accion,detalles,usuario,timestamp");
        List<SystemLog> list = controller.getLogDao().findAll();
        list.forEach(l -> pw.println(l.getId()+","+csv(l.getAction())+","
            +csv(l.getDetails())+","+csv(l.getPerformedBy())+","+l.getTimestamp()));
        return list.size();
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel     pnlHeader;
    private javax.swing.JPanel     pnlCards;
    private javax.swing.JPanel     pnlStatus;
    private javax.swing.JLabel     lblTitle;
    private javax.swing.JLabel     lblStatusTitle;
    private javax.swing.JTextArea  txtStatus;
}
