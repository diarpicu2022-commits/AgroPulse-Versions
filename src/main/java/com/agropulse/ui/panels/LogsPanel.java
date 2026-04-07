package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.SystemLog;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Panel Logs del Sistema – compatible NetBeans */
public class LogsPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;

    public LogsPanel(GreenhouseController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header
        pnlHeader = new JPanel(new BorderLayout(12, 0));
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("📋 Logs del Sistema");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);

        pnlFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlFilter.setBackground(AppTheme.BG_MAIN);

        lblFilterLbl = new JLabel("Filtrar:");
        lblFilterLbl.setFont(AppTheme.FONT_SMALL);

        txtFilter = AppTheme.textField();
        txtFilter.setPreferredSize(new Dimension(180, 30));

        btnSearch = AppTheme.secondaryButton("🔍");
        btnSearch.setPreferredSize(new Dimension(44, 30));
        btnSearch.addActionListener(e -> applyFilter());

        btnRefresh = AppTheme.secondaryButton("🔄");
        btnRefresh.setPreferredSize(new Dimension(44, 30));
        btnRefresh.addActionListener(e -> refresh());

        pnlFilter.add(lblFilterLbl);
        pnlFilter.add(txtFilter);
        pnlFilter.add(btnSearch);
        pnlFilter.add(btnRefresh);

        pnlHeader.add(lblTitle,   BorderLayout.WEST);
        pnlHeader.add(pnlFilter,  BorderLayout.EAST);

        // Tabla
        tblModel = new DefaultTableModel(new String[]{"ID","Acción","Detalles","Usuario","Fecha/Hora"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLogs = AppTheme.styledTable(new String[]{"ID","Acción","Detalles","Usuario","Fecha/Hora"}, new Object[0][0]);
        tblLogs.setModel(tblModel);
        tblLogs.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblLogs.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblLogs.getColumnModel().getColumn(2).setPreferredWidth(320);
        tblLogs.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblLogs.getColumnModel().getColumn(4).setPreferredWidth(140);

        pnlCard = new JPanel(new BorderLayout());
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(AppTheme.cardBorder());
        pnlCard.add(new JScrollPane(tblLogs), BorderLayout.CENTER);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCard,   BorderLayout.CENTER);

        refresh();
    }

    private void applyFilter() {
        String q = txtFilter.getText().trim().toLowerCase();
        tblModel.setRowCount(0);
        controller.getLogDao().findAll().forEach(log -> {
            if (q.isEmpty()
                || log.getAction().toLowerCase().contains(q)
                || log.getDetails().toLowerCase().contains(q)
                || log.getPerformedBy().toLowerCase().contains(q)) addRow(log);
        });
    }

    private void addRow(SystemLog l) {
        tblModel.addRow(new Object[]{l.getId(), l.getAction(), l.getDetails(),
            l.getPerformedBy(), l.getTimestamp()});
    }

    @Override
    public void refresh() {
        tblModel.setRowCount(0);
        controller.getLogDao().findAll().forEach(this::addRow);
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel   pnlHeader;
    private javax.swing.JPanel   pnlFilter;
    private javax.swing.JPanel   pnlCard;
    private javax.swing.JLabel   lblTitle;
    private javax.swing.JLabel   lblFilterLbl;
    private javax.swing.JTextField txtFilter;
    private javax.swing.JButton  btnSearch;
    private javax.swing.JButton  btnRefresh;
    private javax.swing.JTable   tblLogs;
    private javax.swing.table.DefaultTableModel tblModel;
}
