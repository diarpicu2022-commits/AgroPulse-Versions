package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.dao.GreenhouseDao;
import com.agropulse.model.Greenhouse;
import com.agropulse.model.Sensor;
import com.agropulse.model.SensorReading;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel Monitoreo de Sensores.
 * Filtra las lecturas según el invernadero seleccionado en el controller.
 * Implementa Refreshable para actualizarse cuando el panel se activa.
 */
public class SensorPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;

    public SensorPanel(GreenhouseController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Header ────────────────────────────────────────────────────
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        pnlHeader.setBorder(new EmptyBorder(0, 0, 14, 0));

        lblTitle = new JLabel("📊 Monitoreo de Sensores");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);

        pnlHeaderRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlHeaderRight.setBackground(AppTheme.BG_MAIN);

        lblCurrentGH = new JLabel("");
        lblCurrentGH.setFont(AppTheme.FONT_SMALL);
        lblCurrentGH.setForeground(AppTheme.TEXT_SECONDARY);

        lblLastUpdate = new JLabel("─");
        lblLastUpdate.setFont(AppTheme.FONT_SMALL);
        lblLastUpdate.setForeground(AppTheme.TEXT_SECONDARY);

        btnRefresh = AppTheme.secondaryButton("🔄 Actualizar");
        btnRefresh.setPreferredSize(new Dimension(130, 32));
        btnRefresh.addActionListener(e -> refresh());

        pnlHeaderRight.add(lblCurrentGH);
        pnlHeaderRight.add(lblLastUpdate);
        pnlHeaderRight.add(btnRefresh);

        pnlHeader.add(lblTitle,       BorderLayout.WEST);
        pnlHeader.add(pnlHeaderRight, BorderLayout.EAST);

        // ── Tabla sensores ────────────────────────────────────────────
        tblSensorsModel = new DefaultTableModel(
            new String[]{"ID","Nombre","Tipo","Último Valor","Unidad","Estado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSensors = AppTheme.styledTable(
            new String[]{"ID","Nombre","Tipo","Último Valor","Unidad","Estado"}, new Object[0][0]);
        tblSensors.setModel(tblSensorsModel);

        pnlCardSensors = new JPanel(new BorderLayout());
        pnlCardSensors.setBackground(Color.WHITE);
        pnlCardSensors.setBorder(AppTheme.cardBorder());
        lblCardSensorsTitle = new JLabel("  Estado Actual de Sensores");
        lblCardSensorsTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblCardSensorsTitle.setForeground(AppTheme.PRIMARY_DARK);
        lblCardSensorsTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        pnlCardSensors.add(lblCardSensorsTitle, BorderLayout.NORTH);
        pnlCardSensors.add(new JScrollPane(tblSensors), BorderLayout.CENTER);

        // ── Tabla lecturas ────────────────────────────────────────────
        tblReadingsModel = new DefaultTableModel(
            new String[]{"Sensor","Valor","Unidad","Fecha/Hora","Invernadero"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblReadings = AppTheme.styledTable(
            new String[]{"Sensor","Valor","Unidad","Fecha/Hora","Invernadero"}, new Object[0][0]);
        tblReadings.setModel(tblReadingsModel);

        pnlCardReadings = new JPanel(new BorderLayout());
        pnlCardReadings.setBackground(Color.WHITE);
        pnlCardReadings.setBorder(AppTheme.cardBorder());
        lblCardReadingsTitle = new JLabel("  Últimas 20 Lecturas");
        lblCardReadingsTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblCardReadingsTitle.setForeground(AppTheme.PRIMARY_DARK);
        lblCardReadingsTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        pnlCardReadings.add(lblCardReadingsTitle, BorderLayout.NORTH);
        pnlCardReadings.add(new JScrollPane(tblReadings), BorderLayout.CENTER);

        splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlCardSensors, pnlCardReadings);
        splitMain.setDividerLocation(280);
        splitMain.setBorder(null);

        add(pnlHeader, BorderLayout.NORTH);
        add(splitMain, BorderLayout.CENTER);

        refresh();
    }

    @Override
    public void refresh() {
        int    ghId   = controller.getSelectedGreenhouseId();
        String ghName = controller.getSelectedGreenhouseName();

        // Actualizar etiqueta del invernadero activo
        lblCurrentGH.setText("🏠 " + ghName + "  |  ");

        // ── Tabla sensores (no dependen del invernadero en v1) ────────
        tblSensorsModel.setRowCount(0);
        for (Sensor s : controller.getSensors()) {
            tblSensorsModel.addRow(new Object[]{
                s.getId(), s.getName(), s.getType().getDisplayName(),
                String.format("%.2f", s.getLastValue()), s.getType().getUnit(),
                s.isActive() ? "✅ Activo" : "❌ Inactivo"
            });
        }

        // ── Tabla lecturas filtrada por invernadero ───────────────────
        tblReadingsModel.setRowCount(0);
        List<SensorReading> list = controller.getReadingDao().findByGreenhouse(ghId);
        for (int i = 0; i < Math.min(list.size(), 20); i++) {
            SensorReading r = list.get(i);
            tblReadingsModel.addRow(new Object[]{
                r.getSensorName() != null ? r.getSensorName() : "Sensor #" + r.getSensorId(),
                String.format("%.2f", r.getValue()),
                r.getUnit(),
                r.getTimestamp(),
                ghName
            });
        }

        // Actualizar etiqueta de últimas lecturas según filtro
        lblCardReadingsTitle.setText("  Últimas 20 Lecturas" +
            (ghId == 0 ? "" : " — " + ghName));

        lblLastUpdate.setText("Actualizado: " + java.time.LocalTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel           pnlHeader;
    private javax.swing.JPanel           pnlHeaderRight;
    private javax.swing.JPanel           pnlCardSensors;
    private javax.swing.JPanel           pnlCardReadings;
    private javax.swing.JLabel           lblTitle;
    private javax.swing.JLabel           lblLastUpdate;
    private javax.swing.JLabel           lblCurrentGH;
    private javax.swing.JLabel           lblCardSensorsTitle;
    private javax.swing.JLabel           lblCardReadingsTitle;
    private javax.swing.JButton          btnRefresh;
    private javax.swing.JTable           tblSensors;
    private javax.swing.JTable           tblReadings;
    private javax.swing.JSplitPane       splitMain;
    private javax.swing.table.DefaultTableModel tblSensorsModel;
    private javax.swing.table.DefaultTableModel tblReadingsModel;
}
