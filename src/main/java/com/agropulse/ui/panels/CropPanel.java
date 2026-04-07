package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.Crop;
import com.agropulse.model.SystemLog;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Panel Gestión de Cultivos – compatible NetBeans */
public class CropPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;
    private final User user;

    public CropPanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("🌿 Gestión de Cultivos");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);

        pnlHBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlHBtns.setBackground(AppTheme.BG_MAIN);
        btnAdd = AppTheme.primaryButton("➕ Nuevo Cultivo");
        btnAdd.setPreferredSize(new Dimension(150, 32));
        btnAdd.addActionListener(e -> showAddDialog());
        pnlHBtns.add(btnAdd);

        pnlHeader.add(lblTitle,  BorderLayout.WEST);
        pnlHeader.add(pnlHBtns, BorderLayout.EAST);

        String[] cols = {"ID","Nombre","Variedad","Temp Min","Temp Max","Hum Min","Hum Max","Suelo Min","Suelo Max"};
        tblModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCrops = AppTheme.styledTable(cols, new Object[0][0]);
        tblCrops.setModel(tblModel);

        pnlCard = new JPanel(new BorderLayout());
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(AppTheme.cardBorder());
        pnlCard.add(new JScrollPane(tblCrops), BorderLayout.CENTER);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCard,   BorderLayout.CENTER);

        refresh();
    }

    private void showAddDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nuevo Cultivo", true);
        dlg.setSize(400, 580);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JTextField txName    = f(); JTextField txVariety = f();
        JTextField txTMin    = f(); JTextField txTMax = f();
        JTextField txHMin    = f(); JTextField txHMax = f();
        JTextField txSMin    = f(); JTextField txSMax = f();

        p.add(new JLabel("Nombre del cultivo:")); p.add(txName);
        p.add(new JLabel("Variedad:")); p.add(txVariety);

        JButton btnAuto = AppTheme.secondaryButton("🤖 Auto-completar con IA");
        btnAuto.setAlignmentX(LEFT_ALIGNMENT);
        final JTextField finalTxName = txName;
        final JTextField finalTxVariety = txVariety;
        btnAuto.addActionListener(e -> {
            String name = finalTxName.getText().trim();
            String variety = finalTxVariety.getText().trim();
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

        String[] lbls = {"Temp. mínima (°C):","Temp. máxima (°C):","Humedad mín. (%):","Humedad máx. (%):","Hum. suelo mín. (%):","Hum. suelo máx. (%):"};
        JTextField[] inps = {txTMin, txTMax, txHMin, txHMax, txSMin, txSMax};
        for (int i = 0; i < lbls.length; i++) {
            JLabel l = new JLabel(lbls[i]); l.setFont(AppTheme.FONT_SMALL); l.setForeground(AppTheme.TEXT_SECONDARY); l.setAlignmentX(LEFT_ALIGNMENT);
            p.add(l); p.add(Box.createVerticalStrut(3)); p.add(inps[i]); p.add(Box.createVerticalStrut(8));
        }

        JButton btnSave = AppTheme.primaryButton("💾 Guardar Cultivo");
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); btnSave.setAlignmentX(LEFT_ALIGNMENT);
        btnSave.addActionListener(e -> {
            try {
                if (txName.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "El nombre es obligatorio.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
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

    private JTextField f() {
        JTextField t = AppTheme.textField(); t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36)); t.setAlignmentX(LEFT_ALIGNMENT); return t;
    }
    private double d(JTextField t) { return Double.parseDouble(t.getText().trim()); }

    private void parseAndFill(String result, JTextField txTMin, JTextField txTMax, 
                        JTextField txHMin, JTextField txHMax, 
                        JTextField txSMin, JTextField txSMax) {
        try {
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
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                keyword + "[^0-9]*(\\d+)[^0-9]*[-a]+[^0-9]*(\\d+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
        } catch (Exception e) {}
        return null;
    }

    @Override
    public void refresh() {
        tblModel.setRowCount(0);
        controller.getCropDao().findAll().forEach(c -> tblModel.addRow(new Object[]{
            c.getId(), c.getName(), c.getVariety(), c.getMinTemp(), c.getMaxTemp(),
            c.getMinHumidity(), c.getMaxHumidity(), c.getMinSoilMoisture(), c.getMaxSoilMoisture()
        }));
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel   pnlHeader;
    private javax.swing.JPanel   pnlHBtns;
    private javax.swing.JPanel   pnlCard;
    private javax.swing.JLabel   lblTitle;
    private javax.swing.JButton  btnAdd;
    private javax.swing.JTable   tblCrops;
    private javax.swing.table.DefaultTableModel tblModel;
}
