package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.Alert;
import com.agropulse.model.enums.AlertLevel;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Panel de Alertas – compatible NetBeans */
public class AlertsPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;

    public AlertsPanel(GreenhouseController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("🔔 Alertas del Sistema");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);
        btnRefresh = AppTheme.secondaryButton("🔄 Actualizar");
        btnRefresh.setPreferredSize(new Dimension(120, 32));
        btnRefresh.addActionListener(e -> refresh());
        pnlHeader.add(lblTitle,   BorderLayout.WEST);
        pnlHeader.add(btnRefresh, BorderLayout.EAST);

        // Tarjetas de resumen
        pnlSummary = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlSummary.setBackground(AppTheme.BG_MAIN);
        pnlSummary.setPreferredSize(new Dimension(0, 80));

        lblTotal    = new JLabel("0", SwingConstants.CENTER);
        lblCritical = new JLabel("0", SwingConstants.CENTER);
        lblWarning  = new JLabel("0", SwingConstants.CENTER);
        lblInfoCnt  = new JLabel("0", SwingConstants.CENTER);

        pnlSummary.add(buildStatCard("Total Alertas",     lblTotal,    AppTheme.INFO));
        pnlSummary.add(buildStatCard("🔴 Críticas",       lblCritical, AppTheme.DANGER));
        pnlSummary.add(buildStatCard("🟡 Advertencias",   lblWarning,  AppTheme.WARNING));
        pnlSummary.add(buildStatCard("🟢 Informativas",   lblInfoCnt,  AppTheme.SUCCESS));

        // Tabla
        tblModel = new DefaultTableModel(new String[]{"ID","Nivel","Mensaje","Enviada","Fecha/Hora"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblAlerts = new JTable(tblModel);
        tblAlerts.setFont(AppTheme.FONT_BODY);
        tblAlerts.setRowHeight(32);
        tblAlerts.setShowGrid(false);
        tblAlerts.getTableHeader().setFont(AppTheme.FONT_BUTTON);
        tblAlerts.getTableHeader().setBackground(AppTheme.BG_HEADER);
        tblAlerts.getTableHeader().setForeground(Color.WHITE);
        tblAlerts.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) {
                    String lvl = (String) t.getValueAt(row, 1);
                    if ("🔴 Crítico".equals(lvl))       setBackground(new Color(0xFFEBEE));
                    else if ("🟡 Advertencia".equals(lvl)) setBackground(new Color(0xFFFDE7));
                    else                                   setBackground(new Color(0xF1F8E9));
                    setForeground(AppTheme.TEXT_PRIMARY);
                }
                setBorder(new EmptyBorder(0,8,0,8));
                return this;
            }
        });

        pnlCard = new JPanel(new BorderLayout());
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(AppTheme.cardBorder());
        pnlCard.add(new JScrollPane(tblAlerts), BorderLayout.CENTER);

        pnlTop = new JPanel(new BorderLayout(0, 12));
        pnlTop.setBackground(AppTheme.BG_MAIN);
        pnlTop.add(pnlHeader,  BorderLayout.NORTH);
        pnlTop.add(pnlSummary, BorderLayout.CENTER);

        add(pnlTop,  BorderLayout.NORTH);
        add(pnlCard, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildStatCard(String title, JLabel valLbl, Color accent) {
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valLbl.setForeground(accent);
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(accent, 1, true), new EmptyBorder(10,10,10,10)));
        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(AppTheme.FONT_SMALL); lbl.setForeground(AppTheme.TEXT_SECONDARY);
        card.add(lbl, BorderLayout.NORTH); card.add(valLbl, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void refresh() {
        tblModel.setRowCount(0);
        List<Alert> list = controller.getAlertDao().findAll();
        int tot = list.size(), crit = 0, warn = 0, info = 0;
        for (Alert a : list) {
            String lvl;
            if      (a.getLevel() == AlertLevel.CRITICAL) { crit++; lvl = "🔴 Crítico"; }
            else if (a.getLevel() == AlertLevel.WARNING)  { warn++; lvl = "🟡 Advertencia"; }
            else                                          { info++; lvl = "🟢 Info"; }
            tblModel.addRow(new Object[]{a.getId(), lvl, a.getMessage(),
                a.isSent() ? "✅" : "⏳", a.getCreatedAt()});
        }
        lblTotal.setText(String.valueOf(tot));
        lblCritical.setText(String.valueOf(crit));
        lblWarning.setText(String.valueOf(warn));
        lblInfoCnt.setText(String.valueOf(info));
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel   pnlHeader;
    private javax.swing.JPanel   pnlSummary;
    private javax.swing.JPanel   pnlCard;
    private javax.swing.JPanel   pnlTop;
    private javax.swing.JLabel   lblTitle;
    private javax.swing.JLabel   lblTotal;
    private javax.swing.JLabel   lblCritical;
    private javax.swing.JLabel   lblWarning;
    private javax.swing.JLabel   lblInfoCnt;
    private javax.swing.JButton  btnRefresh;
    private javax.swing.JTable   tblAlerts;
    private javax.swing.table.DefaultTableModel tblModel;
}
