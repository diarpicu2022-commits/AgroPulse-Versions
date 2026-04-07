package com.agropulse.ui.panels;

import com.agropulse.config.AppConfig;
import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.SystemLog;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Panel Configuración del Sistema – compatible NetBeans */
public class SystemConfigPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;
    private final User user;
    private AppConfig cfg;

    public SystemConfigPanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        this.cfg        = controller.getConfig();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("🛠️ Configuración del Sistema");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        // ── Servicios de IA (detallado) ───────────────────────────────
        pnlServices = new JPanel(new GridLayout(1, 2, 16, 0));
        pnlServices.setBackground(AppTheme.BG_MAIN);

        // Tarjeta IA con detalle de servicios activos
        JPanel aiCard = new JPanel();
        aiCard.setLayout(new BoxLayout(aiCard, BoxLayout.Y_AXIS));
        aiCard.setBackground(Color.WHITE);
        aiCard.setBorder(AppTheme.cardBorder());

        JLabel lblAI = new JLabel("🤖 Servicios de Inteligencia Artificial");
        lblAI.setFont(AppTheme.FONT_SUBTITLE);
        lblAI.setForeground(AppTheme.PRIMARY_DARK);
        lblAI.setAlignmentX(LEFT_ALIGNMENT);
        aiCard.add(lblAI);
        aiCard.add(Box.createVerticalStrut(8));

        lblAIDetail = new JLabel();
        lblAIDetail.setFont(AppTheme.FONT_SMALL);
        lblAIDetail.setForeground(AppTheme.TEXT_SECONDARY);
        lblAIDetail.setAlignmentX(LEFT_ALIGNMENT);
        aiCard.add(lblAIDetail);
        aiCard.add(Box.createVerticalGlue());
        aiCard.add(Box.createVerticalStrut(10));

        boolean aiActive = cfg.isAIEnabled();
        tglAI = buildToggle(aiActive);
        tglAI.setAlignmentX(LEFT_ALIGNMENT);
        tglAI.addChangeListener(e -> {
            boolean on = tglAI.isSelected();
            cfg.setAIEnabled(on);
            tglAI.setText(on ? "✅ Activo" : "⛔ Inactivo");
            tglAI.setBackground(on ? AppTheme.SUCCESS : AppTheme.BORDER_MEDIUM);
            controller.getLogDao().save(new SystemLog("CONFIG",
                "AI " + (on ? "activado" : "desactivado"), user.getUsername()));
            refreshAIDetail();
        });
        aiCard.add(tglAI);

        // Tarjeta WhatsApp
        JPanel waCard = buildServiceCard("💬 WhatsApp (Green API)",
            "Envía alertas críticas al número configurado.", "whatsapp");

        pnlServices.add(aiCard);
        pnlServices.add(waCard);

        // ── Equipo de Desarrollo ──────────────────────────────────────
        pnlTeam = new JPanel();
        pnlTeam.setLayout(new BoxLayout(pnlTeam, BoxLayout.Y_AXIS));
        pnlTeam.setBackground(Color.WHITE);
        pnlTeam.setBorder(AppTheme.cardBorder());

        JLabel lblTeamTitle = new JLabel("  👥 Equipo de Desarrollo");
        lblTeamTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblTeamTitle.setForeground(AppTheme.PRIMARY_DARK);
        lblTeamTitle.setAlignmentX(LEFT_ALIGNMENT);
        pnlTeam.add(lblTeamTitle);
        pnlTeam.add(Box.createVerticalStrut(10));

        JPanel teamGrid = new JPanel(new GridLayout(0, 2, 10, 6));
        teamGrid.setBackground(Color.WHITE);
        teamGrid.setBorder(new EmptyBorder(0, 12, 8, 12));

        addTeamRow(teamGrid, "Desarrollador:", "Leider Cadena - Desarrollador Principal");
        addTeamRow(teamGrid, "Universidad:", "Universidad Cooperativa de Colombia - Nariño");
        addTeamRow(teamGrid, "Proyecto:", "Proyecto de Grado 2025");

        pnlTeam.add(teamGrid);

        // ── Info del sistema ──────────────────────────────────────────
        pnlInfo = new JPanel(new BorderLayout());
        pnlInfo.setBackground(Color.WHITE);
        pnlInfo.setBorder(AppTheme.cardBorder());

        lblInfoTitle = new JLabel("  ℹ️ Información del sistema");
        lblInfoTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblInfoTitle.setForeground(AppTheme.PRIMARY_DARK);
        lblInfoTitle.setBorder(new EmptyBorder(0, 0, 10, 0));

        pnlInfoGrid = new JPanel(new GridLayout(0, 2, 10, 8));
        pnlInfoGrid.setBackground(Color.WHITE);
        pnlInfoGrid.setBorder(new EmptyBorder(0, 8, 8, 8));

        addInfoRow("Versión:",        "AgroPulse v6.0");
        addInfoRow("Build:",          "2025.03");
        addInfoRow("Java:",           System.getProperty("java.version"));
        addInfoRow("Sistema Oper.:",  System.getProperty("os.name"));
        addInfoRow("Base de datos:",  "SQLite (local)");
        addInfoRow("Proyecto GCP:",   "agropulse-491322");
        addInfoRow("WebApp:",         "React + Vite");

        // IAs Activas row (dynamic)
        lblActiveAIs = new JLabel(getActiveAIsText());
        lblActiveAIs.setFont(AppTheme.FONT_BODY);
        lblActiveAIs.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel lblActiveKey = new JLabel("IAs Activas:");
        lblActiveKey.setFont(AppTheme.FONT_SMALL);
        lblActiveKey.setForeground(AppTheme.TEXT_SECONDARY);
        pnlInfoGrid.add(lblActiveKey);
        pnlInfoGrid.add(lblActiveAIs);

        pnlInfo.add(lblInfoTitle,  BorderLayout.NORTH);
        pnlInfo.add(pnlInfoGrid,   BorderLayout.CENTER);

        // ── Layout central con scroll ─────────────────────────────────
        JPanel pnlCenter = new JPanel();
        pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
        pnlCenter.setBackground(AppTheme.BG_MAIN);

        pnlServices.setAlignmentX(LEFT_ALIGNMENT);
        pnlServices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        pnlTeam.setAlignmentX(LEFT_ALIGNMENT);
        pnlTeam.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        pnlInfo.setAlignmentX(LEFT_ALIGNMENT);
        pnlInfo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        pnlCenter.add(pnlServices);
        pnlCenter.add(Box.createVerticalStrut(12));
        pnlCenter.add(pnlTeam);
        pnlCenter.add(Box.createVerticalStrut(12));
        pnlCenter.add(pnlInfo);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);

        refreshAIDetail();
        refresh();
    }

    private JPanel buildServiceCard(String title, String desc, String type) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(AppTheme.cardBorder());

        JLabel lblT = new JLabel(title);
        lblT.setFont(AppTheme.FONT_SUBTITLE);
        lblT.setForeground(AppTheme.PRIMARY_DARK);
        lblT.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lblD = new JLabel("<html><div style='width:200px; font-family:Segoe UI Emoji'>" + desc + "</div></html>");
        lblD.setFont(AppTheme.FONT_SMALL);
        lblD.setForeground(AppTheme.TEXT_SECONDARY);
        lblD.setAlignmentX(LEFT_ALIGNMENT);

        boolean active = "ai".equals(type) ? cfg.isAIEnabled() : cfg.isWhatsAppEnabled();
        JToggleButton tgl = buildToggle(active);
        tgl.setAlignmentX(LEFT_ALIGNMENT);
        tgl.addChangeListener(e -> {
            boolean on = tgl.isSelected();
            if ("ai".equals(type)) cfg.setAIEnabled(on);
            else                   cfg.setWhatsAppEnabled(on);
            tgl.setText(on ? "✅ Activo" : "⛔ Inactivo");
            tgl.setBackground(on ? AppTheme.SUCCESS : AppTheme.BORDER_MEDIUM);
            controller.getLogDao().save(new SystemLog("CONFIG",
                type.toUpperCase() + " " + (on ? "activado" : "desactivado"), user.getUsername()));
        });

        card.add(lblT);
        card.add(Box.createVerticalStrut(8));
        card.add(lblD);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(10));
        card.add(tgl);
        return card;
    }

    private JToggleButton buildToggle(boolean active) {
        JToggleButton tgl = new JToggleButton(active ? "✅ Activo" : "⛔ Inactivo", active);
        tgl.setFont(AppTheme.FONT_BUTTON);
        tgl.setFocusPainted(false);
        tgl.setBorderPainted(false);
        tgl.setBackground(active ? AppTheme.SUCCESS : AppTheme.BORDER_MEDIUM);
        tgl.setForeground(Color.WHITE);
        tgl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tgl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return tgl;
    }

    private void addInfoRow(String key, String val) {
        JLabel k = new JLabel(key); k.setFont(AppTheme.FONT_SMALL); k.setForeground(AppTheme.TEXT_SECONDARY);
        JLabel v = new JLabel(val); v.setFont(AppTheme.FONT_BODY);  v.setForeground(AppTheme.TEXT_PRIMARY);
        pnlInfoGrid.add(k); pnlInfoGrid.add(v);
    }

    private void addTeamRow(JPanel grid, String key, String val) {
        JLabel k = new JLabel(key); k.setFont(AppTheme.FONT_SMALL); k.setForeground(AppTheme.TEXT_SECONDARY);
        JLabel v = new JLabel(val); v.setFont(AppTheme.FONT_BODY);  v.setForeground(AppTheme.TEXT_PRIMARY);
        grid.add(k); grid.add(v);
    }

    private void refreshAIDetail() {
        StringBuilder sb = new StringBuilder("<html><div style='font-family:Segoe UI Emoji'><small>");
        int count = 0;
        if (cfg.isOpenRouterEnabled()) { sb.append("✅ OpenRouter &nbsp; "); count++; }
        else sb.append("⛔ OpenRouter &nbsp; ");
        if (cfg.isGroqEnabled()) { sb.append("✅ Groq &nbsp; "); count++; }
        else sb.append("⛔ Groq &nbsp; ");
        if (cfg.isMistralEnabled()) { sb.append("✅ Mistral &nbsp; "); count++; }
        else sb.append("⛔ Mistral &nbsp; ");
        if (cfg.isOllamaEnabled()) { sb.append("✅ Ollama &nbsp; "); count++; }
        else sb.append("⛔ Ollama &nbsp; ");
        if (cfg.isOpenAIEnabled()) { sb.append("✅ OpenAI &nbsp; "); count++; }
        else sb.append("⛔ OpenAI &nbsp; ");
        sb.append("<br><b>").append(count).append(" de 5 servicios activos</b></small></div></html>");
        if (lblAIDetail != null) lblAIDetail.setText(sb.toString());
        if (lblActiveAIs != null) lblActiveAIs.setText(getActiveAIsText());
    }

    private String getActiveAIsText() {
        int count = 0;
        if (cfg.isOpenRouterEnabled()) count++;
        if (cfg.isGroqEnabled()) count++;
        if (cfg.isMistralEnabled()) count++;
        if (cfg.isOllamaEnabled()) count++;
        if (cfg.isOpenAIEnabled()) count++;
        return count + " de 5 servicios";
    }

    @Override public void refresh() {
        cfg = controller.getConfig();
        refreshAIDetail();
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel  pnlHeader;
    private javax.swing.JPanel  pnlServices;
    private javax.swing.JPanel  pnlTeam;
    private javax.swing.JPanel  pnlInfo;
    private javax.swing.JPanel  pnlInfoGrid;
    private javax.swing.JLabel  lblTitle;
    private javax.swing.JLabel  lblInfoTitle;
    private javax.swing.JLabel  lblAIDetail;
    private javax.swing.JLabel  lblActiveAIs;
    private javax.swing.JToggleButton tglAI;
}
