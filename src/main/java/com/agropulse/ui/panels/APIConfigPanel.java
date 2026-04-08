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

/**
 * Panel Configuración de APIs de IA.
 *
 * Proveedores activos (solo los que funcionan):
 *   ⚡ Groq         – GRAT#ISO (LLaMA-3.3-70B ultra-rápido)
 *   🐙 GitHub       – GRAT#ISO (phi-4-mini)
 *   💻 Ollama       – local, sin internet
 *   💬 WhatsApp     – Green API
 */
public class APIConfigPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;
    private final User                 user;
    private final AppConfig            cfg;

    public APIConfigPanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        this.cfg        = controller.getConfig();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        lblTitle = new JLabel("🔌 Configuración de APIs de IA");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);

        JPanel pnlInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlInfo.setBackground(new Color(0xE8F5E9));
        pnlInfo.setBorder(new EmptyBorder(6, 12, 6, 12));
        JLabel lblInfo = new JLabel(
            "⚡  Todas las IAs activas se consultan en PARALELO. " +
            "OpenRouter y Groq son GRATUITOS sin tarjeta.");
        lblInfo.setFont(AppTheme.FONT_SMALL);
        lblInfo.setForeground(new Color(0x2E7D32));
        pnlInfo.add(lblInfo);

        // ── Panel de Activar/Desactivar todas ──
        JPanel pnlBulk = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        pnlBulk.setBackground(AppTheme.BG_MAIN);

        JButton btnEnableAll = AppTheme.primaryButton("✅ Activar Todas las APIs");
        btnEnableAll.addActionListener(e -> {
            tglGroq.setSelected(true);
            tglGithub.setSelected(true);
            tglOllama.setSelected(true);
            tglWA.setSelected(true);
            ok("✅ Todas las APIs activadas. Recuerda guardar cada una.");
        });

        JButton btnDisableAll = new JButton("⛔ Desactivar Todas las APIs");
        btnDisableAll.setFont(AppTheme.FONT_BUTTON);
        btnDisableAll.setFocusPainted(false);
        btnDisableAll.setBackground(new Color(0xD32F2F));
        btnDisableAll.setForeground(Color.WHITE);
        btnDisableAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDisableAll.addActionListener(e -> {
            tglGroq.setSelected(false);
            tglGithub.setSelected(false);
            tglOllama.setSelected(false);
            tglWA.setSelected(false);
            ok("⛔ Todas las APIs desactivadas. Recuerda guardar cada una.");
        });

        pnlBulk.add(btnEnableAll);
        pnlBulk.add(btnDisableAll);

        // Fila superior: Groq | GitHub | Ollama
        JPanel pnlTop = new JPanel(new GridLayout(1, 3, 12, 0));
        pnlTop.setBackground(AppTheme.BG_MAIN);
        pnlTop.add(buildGroqCard());
        pnlTop.add(buildGithubCard());
        pnlTop.add(buildOllamaCard());

        // Fila inferior: WhatsApp
        JPanel pnlBot = new JPanel(new GridLayout(1, 1, 12, 0));
        pnlBot.setBackground(AppTheme.BG_MAIN);
        pnlBot.add(buildWhatsAppCard());

        JPanel pnlGrid = new JPanel(new GridLayout(2, 1, 0, 12));
        pnlGrid.setBackground(AppTheme.BG_MAIN);
        pnlGrid.add(pnlTop);
        pnlGrid.add(pnlBot);

        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle.setAlignmentX(LEFT_ALIGNMENT);
        pnlInfo.setAlignmentX(LEFT_ALIGNMENT);
        pnlBulk.setAlignmentX(LEFT_ALIGNMENT);
        pnlHeader.add(lblTitle);
        pnlHeader.add(Box.createVerticalStrut(8));
        pnlHeader.add(pnlInfo);
        pnlHeader.add(pnlBulk);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlGrid,   BorderLayout.CENTER);

        refresh();
    }

    // ─── Tarjetas ─────────────────────────────────────────────────

    private JPanel buildGroqCard() {
        JPanel card = card();
        txtGroqKey = pwd();
        tglGroq    = toggle("Groq", cfg.isGroqEnabled());
        hookToggle(tglGroq);
        JButton btn = saveBtn("💾 Guardar Groq", () -> {
            cfg.set("groq_api_key", new String(txtGroqKey.getPassword()));
            cfg.setGroqEnabled(tglGroq.isSelected());
            controller.refreshAIServices();
            log("CONFIG_API", "Groq guardado");
            ok("✅ Groq guardado.");
        });
        addRows(card, sectionLabel("⚡ Groq (LLaMA-3.3-70B Gratis)"),
                row("API Key (gsk_...)", txtGroqKey), tglGroq, btn);
        return card;
    }

    private JPanel buildGithubCard() {
        JPanel card = card();
        txtGithubKey = pwd();
        tglGithub    = toggle("GitHub", cfg.isGithubEnabled());
        hookToggle(tglGithub);

        JLabel hint = new JLabel(
            "<html><div style='font-family:Segoe UI Emoji'><small>✅ <b>Gratis sin tarjeta.</b><br>" +
            "1. Ir a <b>github.com</b> → Settings → Developer settings<br>" +
            "2. Personal access tokens → Fine-grained tokens<br>" +
            "3. Generar token con permisos de Models AI</small></div></html>");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(new Color(0x1B5E20));
        hint.setAlignmentX(LEFT_ALIGNMENT);

        JButton btn = saveBtn("💾 Guardar GitHub", () -> {
            cfg.set("github_api_key", new String(txtGithubKey.getPassword()));
            cfg.setGithubEnabled(tglGithub.isSelected());
            controller.refreshAIServices();
            log("CONFIG_API", "GitHub guardado");
            ok("✅ GitHub guardado. Modelos phi-4-mini activos.");
        });

        card.add(sectionLabel("🐙 GitHub Models (phi-4-mini Gratis)"));
        card.add(Box.createVerticalStrut(6));
        card.add(hint);
        card.add(Box.createVerticalStrut(8));
        card.add(row("PAT Token", txtGithubKey));
        card.add(tglGithub);
        card.add(Box.createVerticalStrut(8));
        card.add(btn);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildOllamaCard() {
        JPanel card = card();
        txtOllamaHost  = fld();
        txtOllamaModel = fld();
        tglOllama = toggle("Ollama", cfg.isOllamaEnabled());
        hookToggle(tglOllama);

        JLabel hint = new JLabel(
            "<html><div style='font-family:Segoe UI Emoji'><small>Instalación Windows:<br>" +
            "<b>irm https://ollama.com/install.ps1 | iex</b><br>" +
            "Luego: <b>ollama pull llama3</b></small></div></html>");
        hint.setFont(AppTheme.FONT_SMALL);
        hint.setForeground(AppTheme.TEXT_SECONDARY);
        hint.setAlignmentX(LEFT_ALIGNMENT);

        JButton btn = saveBtn("💾 Guardar Ollama", () -> {
            cfg.set("ollama_host",  txtOllamaHost.getText().trim());
            cfg.set("ollama_model", txtOllamaModel.getText().trim());
            cfg.setOllamaEnabled(tglOllama.isSelected());
            controller.refreshAIServices();
            log("CONFIG_API", "Ollama guardado");
            ok("✅ Ollama guardado.");
        });

        card.add(sectionLabel("💻 Ollama (Local — Sin costo)"));
        card.add(Box.createVerticalStrut(6));
        card.add(hint);
        card.add(Box.createVerticalStrut(8));
        addField(card, "Host (localhost:11434)", txtOllamaHost);
        addField(card, "Modelo (llama3 / phi3 / mistral)", txtOllamaModel);
        card.add(tglOllama);
        card.add(Box.createVerticalStrut(8));
        card.add(btn);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildWhatsAppCard() {
        JPanel card = card();
        txtGreenUrl   = fld();
        txtGreenId    = fld();
        txtGreenToken = pwd();
        txtAlertPhone = fld();
        tglWA = toggle("WhatsApp", cfg.isWhatsAppEnabled());
        hookToggle(tglWA);
        JButton btn = saveBtn("💾 Guardar WhatsApp", () -> {
            cfg.set("green_api_url",     txtGreenUrl.getText().trim());
            cfg.set("green_id_instance", txtGreenId.getText().trim());
            cfg.set("green_api_token",   new String(txtGreenToken.getPassword()));
            cfg.set("alert_phone",       txtAlertPhone.getText().trim());
            cfg.setWhatsAppEnabled(tglWA.isSelected());
            log("CONFIG_API", "WhatsApp guardado");
            ok("✅ WhatsApp guardado.");
        });
        addRows(card, sectionLabel("💬 WhatsApp (Green API)"),
                row("URL del servicio", txtGreenUrl),
                row("ID de instancia",  txtGreenId),
                row("Token API",        txtGreenToken),
                row("Teléfono alertas", txtAlertPhone),
                tglWA, btn);
        return card;
    }

    // ─── Refresh ──────────────────────────────────────────────────

    @Override
    public void refresh() {
        if (txtGroqKey  != null) txtGroqKey.setText(cfg.getGroqKey());
        if (tglGroq     != null) { tglGroq.setSelected(cfg.isGroqEnabled()); syncToggle(tglGroq); }

        if (txtGithubKey != null) txtGithubKey.setText(cfg.getGithubKey());
        if (tglGithub    != null) { tglGithub.setSelected(cfg.isGithubEnabled()); syncToggle(tglGithub); }

        if (txtOllamaHost  != null) txtOllamaHost.setText(cfg.getOllamaHost());
        if (txtOllamaModel != null) txtOllamaModel.setText(cfg.getOllamaModel());
        if (tglOllama      != null) { tglOllama.setSelected(cfg.isOllamaEnabled()); syncToggle(tglOllama); }

        if (txtGreenUrl   != null) txtGreenUrl.setText(cfg.getGreenApiUrl());
        if (txtGreenId    != null) txtGreenId.setText(cfg.getGreenIdInstance());
        if (txtGreenToken != null) txtGreenToken.setText(cfg.getGreenToken());
        if (txtAlertPhone != null) txtAlertPhone.setText(cfg.getAlertPhone());
        if (tglWA         != null) { tglWA.setSelected(cfg.isWhatsAppEnabled()); syncToggle(tglWA); }
    }

    // ─── Helpers UI ───────────────────────────────────────────────

    private JPanel card() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(AppTheme.cardBorder());
        return p;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.FONT_SUBTITLE);
        l.setForeground(AppTheme.PRIMARY_DARK);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPasswordField pwd() {
        JPasswordField f = new JPasswordField();
        f.setFont(AppTheme.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER_MEDIUM, 1, true),
            new EmptyBorder(6, 10, 6, 10)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    private JTextField fld() {
        JTextField f = AppTheme.textField();
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    private JToggleButton toggle(String label, boolean sel) {
        String txt = sel ? "✅ " + label + " Activo" : "⛔ " + label + " Inactivo";
        JToggleButton t = new JToggleButton(txt, sel);
        t.setFont(AppTheme.FONT_BUTTON);
        t.setFocusPainted(false);
        t.setBorderPainted(false);
        t.setBackground(sel ? AppTheme.SUCCESS : AppTheme.BORDER_MEDIUM);
        t.setForeground(Color.WHITE);
        t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        t.setAlignmentX(LEFT_ALIGNMENT);
        return t;
    }

    private void hookToggle(JToggleButton t) {
        // Extraer el nombre base limpiando emojis y estado
        String raw = t.getText()
            .replace("✅ ", "").replace("⛔ ", "")
            .replace(" Activo", "").replace(" Inactivo", "");
        t.addChangeListener(e -> {
            boolean on = t.isSelected();
            t.setText(on ? "✅ " + raw + " Activo" : "⛔ " + raw + " Inactivo");
            t.setBackground(on ? AppTheme.SUCCESS : AppTheme.BORDER_MEDIUM);
        });
    }

    private JButton saveBtn(String text, Runnable action) {
        JButton btn = AppTheme.primaryButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void addField(JPanel p, String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setFont(AppTheme.FONT_SMALL);
        l.setForeground(AppTheme.TEXT_SECONDARY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(field);
        p.add(Box.createVerticalStrut(8));
    }

    private JPanel row(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        addField(p, label, field);
        return p;
    }

    private void addRows(JPanel card, JLabel header, Object... items) {
        card.add(header);
        card.add(Box.createVerticalStrut(10));
        for (Object item : items) {
            if      (item instanceof JPanel)        card.add((JPanel) item);
            else if (item instanceof JToggleButton) card.add((JToggleButton) item);
            else if (item instanceof JButton) {
                card.add(Box.createVerticalStrut(8));
                card.add((JButton) item);
            }
        }
        card.add(Box.createVerticalGlue());
    }

    private void syncToggle(JToggleButton t) {
        t.setBackground(t.isSelected() ? AppTheme.SUCCESS : AppTheme.BORDER_MEDIUM);
    }

    private void log(String action, String detail) {
        controller.getLogDao().save(new SystemLog(action, detail, user.getUsername()));
    }

    private void ok(String msg) {
        JOptionPane.showMessageDialog(this, msg, "OK", JOptionPane.INFORMATION_MESSAGE);
    }

    // ─── Variables privadas ───────────────────────────────────────
    private javax.swing.JLabel         lblTitle;
    private javax.swing.JPasswordField txtGroqKey;
    private javax.swing.JPasswordField txtGithubKey;
    private javax.swing.JPasswordField txtGreenToken;
    private javax.swing.JTextField     txtOllamaHost;
    private javax.swing.JTextField     txtOllamaModel;
    private javax.swing.JTextField     txtGreenUrl;
    private javax.swing.JTextField     txtGreenId;
    private javax.swing.JTextField     txtAlertPhone;
    private javax.swing.JToggleButton  tglGroq;
    private javax.swing.JToggleButton  tglGithub;
    private javax.swing.JToggleButton  tglOllama;
    private javax.swing.JToggleButton  tglWA;
}
