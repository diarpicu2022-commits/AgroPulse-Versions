package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.Sensor;
import com.agropulse.model.SystemLog;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Panel Configurar Rangos – compatible NetBeans */
public class ConfigRangesPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;
    private final User user;

    public ConfigRangesPanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        pnlTop = new JPanel(new BorderLayout(0, 10));
        pnlTop.setBackground(AppTheme.BG_MAIN);

        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("⚙️ Configurar Rangos de Sensores");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);
        btnRefresh = AppTheme.secondaryButton("🔄 Recargar");
        btnRefresh.setPreferredSize(new Dimension(120, 32));
        btnRefresh.addActionListener(e -> refresh());
        pnlHeader.add(lblTitle,   BorderLayout.WEST);
        pnlHeader.add(btnRefresh, BorderLayout.EAST);

        pnlWarning = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlWarning.setBackground(new Color(0xFFF8E1));
        pnlWarning.setBorder(new EmptyBorder(8, 12, 8, 12));
        lblWarning = new JLabel("⚠️  Los rangos definen cuándo se generan alertas.");
        lblWarning.setFont(AppTheme.FONT_SMALL);
        lblWarning.setForeground(AppTheme.WARNING);
        pnlWarning.add(lblWarning);

        pnlTop.add(pnlHeader,  BorderLayout.NORTH);
        pnlTop.add(pnlWarning, BorderLayout.SOUTH);

        pnlSensors = new JPanel();
        pnlSensors.setLayout(new BoxLayout(pnlSensors, BoxLayout.Y_AXIS));
        pnlSensors.setBackground(AppTheme.BG_MAIN);

        scrSensors = new JScrollPane(pnlSensors);
        scrSensors.setBorder(null);
        scrSensors.getViewport().setBackground(AppTheme.BG_MAIN);

        add(pnlTop,    BorderLayout.NORTH);
        add(scrSensors, BorderLayout.CENTER);

        refresh();
    }

    @Override
    public void refresh() {
        pnlSensors.removeAll();
        for (Sensor s : controller.getSensors()) {
            pnlSensors.add(buildSensorCard(s));
            pnlSensors.add(Box.createVerticalStrut(12));
        }
        pnlSensors.revalidate();
        pnlSensors.repaint();
    }

    private JPanel buildSensorCard(Sensor sensor) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(AppTheme.cardBorder());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        card.setAlignmentX(LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.anchor = GridBagConstraints.WEST;

        JLabel lblName = new JLabel("🌡 " + sensor.getName() + "  (" + sensor.getType().getUnit() + ")");
        lblName.setFont(AppTheme.FONT_SUBTITLE); lblName.setForeground(AppTheme.PRIMARY_DARK);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4; card.add(lblName, g);

        g.gridwidth = 1; g.gridy = 1;
        g.gridx = 0; card.add(new JLabel("Mínimo:"), g);
        g.gridx = 1;
        JTextField txtMin = AppTheme.textField();
        txtMin.setText(String.format("%.1f", sensor.getMinValue()));
        txtMin.setPreferredSize(new Dimension(100, 32)); card.add(txtMin, g);

        g.gridx = 2; card.add(new JLabel("Máximo:"), g);
        g.gridx = 3;
        JTextField txtMax = AppTheme.textField();
        txtMax.setText(String.format("%.1f", sensor.getMaxValue()));
        txtMax.setPreferredSize(new Dimension(100, 32)); card.add(txtMax, g);

        g.gridy = 2; g.gridx = 0; g.gridwidth = 1;
        JButton btnAvg = AppTheme.secondaryButton("📊 Calcular promedio");
        btnAvg.setPreferredSize(new Dimension(160, 34));
        final Sensor finalSensor = sensor;
        final JTextField finalTxtMin = txtMin;
        final JTextField finalTxtMax = txtMax;
        btnAvg.addActionListener(e -> {
            btnAvg.setEnabled(false);
            btnAvg.setText("⏳ Calculando...");
            new SwingWorker<double[], Void>() {
                @Override protected double[] doInBackground() {
                    return controller.calculateAverageRange(finalSensor.getId());
                }
                @Override protected void done() {
                    btnAvg.setEnabled(true);
                    btnAvg.setText("📊 Calcular promedio");
                    try {
                        double[] avg = get();
                        if (avg != null) {
                            finalTxtMin.setText(String.format("%.1f", avg[0]));
                            finalTxtMax.setText(String.format("%.1f", avg[1]));
                            JOptionPane.showMessageDialog(card, 
                                "✅ Promedio calculado:\nMín: " + String.format("%.1f", avg[0]) + 
                                " | Máx: " + String.format("%.1f", avg[1]), 
                                "OK", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(card, 
                                "⚠️ No hay lecturas suficientes para calcular el promedio.", 
                                "Advertencia", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(card, "Error: " + ex.getMessage(), 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        card.add(btnAvg, g);

        g.gridx = 2; g.gridwidth = 2;
        JButton btnSave = AppTheme.primaryButton("💾 Guardar");
        btnSave.setPreferredSize(new Dimension(120, 34));
        btnSave.addActionListener(e -> {
            try {
                double min = Double.parseDouble(txtMin.getText().trim());
                double max = Double.parseDouble(txtMax.getText().trim());
                if (min >= max) { JOptionPane.showMessageDialog(this, "Mínimo debe ser menor que máximo.", "Error", JOptionPane.ERROR_MESSAGE); return; }
                sensor.setMinValue(min); sensor.setMaxValue(max);
                controller.getSensorDao().update(sensor);
                controller.getLogDao().save(new SystemLog("CONFIG_RANGOS",
                    sensor.getName() + " min=" + min + " max=" + max, user.getUsername()));
                JOptionPane.showMessageDialog(this, "✅ Guardado: " + sensor.getName(), "OK", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Valores numéricos inválidos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        card.add(btnSave, g);
        return card;
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel      pnlTop;
    private javax.swing.JPanel      pnlHeader;
    private javax.swing.JPanel      pnlWarning;
    private javax.swing.JPanel      pnlSensors;
    private javax.swing.JScrollPane scrSensors;
    private javax.swing.JLabel      lblTitle;
    private javax.swing.JLabel      lblWarning;
    private javax.swing.JButton     btnRefresh;
}
