package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.Actuator;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Panel Control de Actuadores — con indicadores de estado en verde/rojo */
public class ActuatorPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;
    private final User user;

    // Colores del indicador
    private static final Color COLOR_ON     = new Color(0x2E7D32);  // verde oscuro fondo
    private static final Color COLOR_ON_FG  = Color.WHITE;
    private static final Color COLOR_OFF    = new Color(0xC62828);  // rojo oscuro fondo
    private static final Color COLOR_OFF_FG = Color.WHITE;
    private static final Color COLOR_ROW_ON  = new Color(0xE8F5E9); // verde claro fila
    private static final Color COLOR_ROW_OFF = new Color(0xFFEBEE); // rojo claro fila

    public ActuatorPanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Header ──────────────────────────────────────────────────
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);

        lblTitle = new JLabel("🔧 Control de Actuadores");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);

        btnRefresh = AppTheme.secondaryButton("🔄 Refrescar");
        btnRefresh.setPreferredSize(new Dimension(120, 32));
        btnRefresh.addActionListener(e -> refresh());

        pnlHeader.add(lblTitle,   BorderLayout.WEST);
        pnlHeader.add(btnRefresh, BorderLayout.EAST);

        // ── Info bar ─────────────────────────────────────────────────
        pnlInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlInfo.setBackground(new Color(0xE8F5E9));
        pnlInfo.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Leyenda de colores
        JLabel legendOn  = pill("● ENCENDIDO", COLOR_ON,  COLOR_ON_FG);
        JLabel legendOff = pill("● APAGADO",   COLOR_OFF, COLOR_OFF_FG);
        lblInfo = new JLabel("  Selecciona un actuador y usa los botones para controlarlo. ");
        lblInfo.setFont(AppTheme.FONT_SMALL);
        lblInfo.setForeground(AppTheme.PRIMARY_DARK);
        pnlInfo.add(lblInfo);
        pnlInfo.add(legendOn);
        pnlInfo.add(Box.createHorizontalStrut(6));
        pnlInfo.add(legendOff);

        // ── Tabla ────────────────────────────────────────────────────
        tblModel = new DefaultTableModel(
                new String[]{"#", "Nombre", "Tipo", "Estado", "Modo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tblActuators = new JTable(tblModel);
        tblActuators.setRowHeight(36);
        tblActuators.setFont(AppTheme.FONT_BODY);
        tblActuators.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);
        tblActuators.setSelectionBackground(new Color(0xBBDEFB));
        tblActuators.setGridColor(new Color(0xE0E0E0));
        tblActuators.setShowHorizontalLines(true);
        tblActuators.setShowVerticalLines(false);

        // Anchos de columna
        tblActuators.getColumnModel().getColumn(0).setPreferredWidth(30);
        tblActuators.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblActuators.getColumnModel().getColumn(2).setPreferredWidth(140);
        tblActuators.getColumnModel().getColumn(3).setPreferredWidth(160);
        tblActuators.getColumnModel().getColumn(4).setPreferredWidth(100);

        // ── Renderer personalizado para Estado ───────────────────────
        // Columna 3 = "Estado": bolita + color de fondo de toda la fila
        tblActuators.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);

                // Determinar si el actuador está encendido según la col 3
                Object statusVal = table.getModel().getValueAt(row, 3);
                boolean isOn = statusVal != null && statusVal.toString().contains("ENCENDIDO");

                if (!isSelected) {
                    // Fondo de la fila completa
                    c.setBackground(isOn ? COLOR_ROW_ON : COLOR_ROW_OFF);
                    c.setForeground(AppTheme.TEXT_PRIMARY);

                    // Celda de estado: fondo sólido con bolita
                    if (col == 3) {
                        String txt = isOn ? "  ●  ENCENDIDO" : "  ●  APAGADO";
                        ((JLabel) c).setText(txt);
                        ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                        c.setBackground(isOn ? COLOR_ON : COLOR_OFF);
                        c.setForeground(isOn ? COLOR_ON_FG : COLOR_OFF_FG);
                        ((JLabel) c).setFont(AppTheme.FONT_BODY.deriveFont(Font.BOLD));
                    }
                } else {
                    c.setBackground(new Color(0xBBDEFB));
                    c.setForeground(AppTheme.TEXT_PRIMARY);
                    if (col == 3) {
                        String txt = isOn ? "  ●  ENCENDIDO" : "  ●  APAGADO";
                        ((JLabel) c).setText(txt);
                        ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }

                // Centrar columna # 
                if (col == 0) ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);

                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });

        // ── Botones de acción ────────────────────────────────────────
        pnlActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlActions.setBackground(Color.WHITE);
        pnlActions.setBorder(new EmptyBorder(4, 0, 4, 0));

        btnToggle = AppTheme.primaryButton("⚡ Encender / Apagar");
        btnToggle.setPreferredSize(new Dimension(190, 36));
        btnToggle.addActionListener(e -> toggleActuator());

        btnMode = AppTheme.secondaryButton("🔄 Auto / Manual");
        btnMode.setPreferredSize(new Dimension(160, 36));
        btnMode.addActionListener(e -> changeMode());

        pnlActions.add(btnToggle);
        pnlActions.add(btnMode);

        // ── Card ─────────────────────────────────────────────────────
        pnlCard = new JPanel(new BorderLayout());
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(AppTheme.cardBorder());
        pnlCard.add(pnlInfo,                      BorderLayout.NORTH);
        pnlCard.add(new JScrollPane(tblActuators), BorderLayout.CENTER);
        pnlCard.add(pnlActions,                    BorderLayout.SOUTH);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCard,   BorderLayout.CENTER);

        refresh();
    }

    // ─── Acciones ────────────────────────────────────────────────────

    private void toggleActuator() {
        int row = tblActuators.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un actuador primero.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.toggleActuator(row, user.getUsername());
        refresh();
    }

    private void changeMode() {
        int row = tblActuators.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un actuador primero.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean cur = controller.getActuators().get(row).isAutoMode();
        controller.setAutoMode(row, !cur, user.getUsername());
        refresh();
        JOptionPane.showMessageDialog(this,
            "Modo cambiado a: " + (!cur ? "Automático 🤖" : "Manual 🖐"),
            "Modo actualizado", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void refresh() {
        int selectedRow = tblActuators.getSelectedRow();
        tblModel.setRowCount(0);
        int i = 1;
        for (Actuator a : controller.getActuators()) {
            tblModel.addRow(new Object[]{
                i++,
                a.getName(),
                a.getType().getDisplayName(),
                a.isEnabled() ? "ENCENDIDO" : "APAGADO",   // El renderer añade la bolita
                a.isAutoMode() ? "🤖 Automático" : "🖐 Manual"
            });
        }
        // Restaurar selección
        if (selectedRow >= 0 && selectedRow < tblModel.getRowCount()) {
            tblActuators.setRowSelectionInterval(selectedRow, selectedRow);
        }
    }

    // ─── Helper: etiqueta tipo "pill" para la leyenda ─────────────────

    private JLabel pill(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.FONT_SMALL.deriveFont(Font.BOLD));
        l.setForeground(fg);
        l.setBackground(bg);
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bg.darker(), 1, true),
            new EmptyBorder(3, 10, 3, 10)));
        return l;
    }

    // ─── Variables privadas ───────────────────────────────────────────
    private javax.swing.JPanel   pnlHeader;
    private javax.swing.JPanel   pnlCard;
    private javax.swing.JPanel   pnlInfo;
    private javax.swing.JPanel   pnlActions;
    private javax.swing.JLabel   lblTitle;
    private javax.swing.JLabel   lblInfo;
    private javax.swing.JButton  btnRefresh;
    private javax.swing.JButton  btnToggle;
    private javax.swing.JButton  btnMode;
    private javax.swing.JTable   tblActuators;
    private javax.swing.table.DefaultTableModel tblModel;
}
