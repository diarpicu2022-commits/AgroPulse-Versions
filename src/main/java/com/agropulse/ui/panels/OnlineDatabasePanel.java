package com.agropulse.ui.panels;

import com.agropulse.config.AppConfig;
import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.SystemLog;
import com.agropulse.model.User;
import com.agropulse.pattern.singleton.DatabaseConnection;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel de configuración de base de datos online (Supabase / PostgreSQL).
 *
 * Instrucciones Supabase (gratis):
 *  1. Crear cuenta en https://supabase.com
 *  2. New Project → elegir región (us-east-1 o similar)
 *  3. Settings → Database → Connection String → seleccionar "JDBC"
 *  4. Copiar y pegar la cadena aquí (reemplazar [YOUR-PASSWORD])
 *
 * Ejemplo de cadena JDBC Supabase:
 *   jdbc:postgresql://db.abcdefgh.supabase.co:5432/postgres?user=postgres&password=MiClave123
 */
public class OnlineDatabasePanel extends JPanel {

    private final GreenhouseController controller;
    private final User user;
    private final AppConfig cfg;
    private final DatabaseConnection dbConn;

    private JTextArea  txtJdbcUrl;
    private JToggleButton tglOnline;
    private JLabel     lblStatus;
    private JTextArea  txtHelp;

    public OnlineDatabasePanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        this.cfg        = controller.getConfig();
        this.dbConn     = DatabaseConnection.getInstance();
        initComponents();
        refresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Título
        JLabel title = new JLabel("☁️ Base de Datos Online (Supabase / PostgreSQL)");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.PRIMARY_DARK);

        // Info
        JPanel pnlInfo = new JPanel(new BorderLayout());
        pnlInfo.setBackground(new Color(0xE3F2FD));
        pnlInfo.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel info = new JLabel("<html>Los datos se guardan en SQLite local <b>siempre</b>. " +
                "Si configuras una BD online, también se sincronizan en la nube en tiempo real.</html>");
        info.setFont(AppTheme.FONT_SMALL);
        info.setForeground(new Color(0x1565C0));
        pnlInfo.add(info);

        JPanel pnlHeader = new JPanel(new BorderLayout(0, 8));
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        pnlHeader.add(title,   BorderLayout.NORTH);
        pnlHeader.add(pnlInfo, BorderLayout.SOUTH);

        // Tarjeta de configuración
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(AppTheme.cardBorder());

        JLabel lblConn = new JLabel("🔗 Cadena de conexión JDBC");
        lblConn.setFont(AppTheme.FONT_SUBTITLE);
        lblConn.setForeground(AppTheme.PRIMARY_DARK);
        lblConn.setAlignmentX(LEFT_ALIGNMENT);

        txtJdbcUrl = new JTextArea(3, 40);
        txtJdbcUrl.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtJdbcUrl.setLineWrap(true);
        txtJdbcUrl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER_MEDIUM),
            new EmptyBorder(6, 8, 6, 8)));

        JLabel example = new JLabel("<html><small>" +
            "Ejemplo Supabase:<br>" +
            "<b>jdbc:postgresql://db.xxxxxxxx.supabase.co:5432/postgres?user=postgres&password=TuClave</b>" +
            "</small></html>");
        example.setFont(AppTheme.FONT_SMALL);
        example.setForeground(AppTheme.TEXT_SECONDARY);
        example.setAlignmentX(LEFT_ALIGNMENT);

        tglOnline = new JToggleButton("⛔ BD Online Inactiva");
        tglOnline.setFont(AppTheme.FONT_BUTTON);
        tglOnline.setFocusPainted(false);
        tglOnline.setBorderPainted(false);
        tglOnline.setBackground(AppTheme.BORDER_MEDIUM);
        tglOnline.setForeground(Color.WHITE);
        tglOnline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tglOnline.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnSave = AppTheme.primaryButton("💾 Guardar y Conectar");
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnSave.setAlignmentX(LEFT_ALIGNMENT);
        btnSave.addActionListener(e -> saveAndConnect());

        JButton btnTest = AppTheme.primaryButton("🔍 Solo Probar Conexión");
        btnTest.setBackground(AppTheme.INFO);
        btnTest.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnTest.setAlignmentX(LEFT_ALIGNMENT);
        btnTest.addActionListener(e -> testConnection());

        lblStatus = new JLabel("⭕ Sin conexión online configurada");
        lblStatus.setFont(AppTheme.FONT_SMALL);
        lblStatus.setForeground(AppTheme.TEXT_SECONDARY);
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);

        card.add(lblConn);
        card.add(Box.createVerticalStrut(10));
        card.add(new JScrollPane(txtJdbcUrl));
        card.add(Box.createVerticalStrut(8));
        card.add(example);
        card.add(Box.createVerticalStrut(12));
        card.add(tglOnline);
        card.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnRow.setAlignmentX(LEFT_ALIGNMENT);
        btnRow.add(btnSave);
        btnRow.add(btnTest);
        card.add(btnRow);
        card.add(Box.createVerticalStrut(10));
        card.add(lblStatus);

        // Panel de ayuda
        JPanel pnlHelp = new JPanel(new BorderLayout());
        pnlHelp.setBackground(Color.WHITE);
        pnlHelp.setBorder(AppTheme.cardBorder());

        JLabel lblHelp = new JLabel("  📖 Cómo configurar Supabase (gratis)");
        lblHelp.setFont(AppTheme.FONT_SUBTITLE);
        lblHelp.setForeground(AppTheme.PRIMARY_DARK);

        txtHelp = new JTextArea();
        txtHelp.setEditable(false);
        txtHelp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtHelp.setBackground(new Color(0xFAFAFA));
        txtHelp.setBorder(new EmptyBorder(8, 12, 8, 12));
        txtHelp.setText(
            "PASOS PARA CONFIGURAR SUPABASE (gratis):\n\n" +
            "1. Ve a https://supabase.com y crea una cuenta\n" +
            "2. Crea un nuevo proyecto (elige un nombre y contraseña)\n" +
            "3. Espera ~2 minutos a que el proyecto esté listo\n" +
            "4. Ve a: Settings → Database → Connection String\n" +
            "5. Selecciona la pestaña 'JDBC'\n" +
            "6. Copia la cadena y reemplaza [YOUR-PASSWORD]\n" +
            "7. Pega aquí y presiona 'Guardar y Conectar'\n\n" +
            "PLAN GRATUITO SUPABASE INCLUYE:\n" +
            "  • 500 MB de base de datos PostgreSQL\n" +
            "  • 2 proyectos activos\n" +
            "  • API REST automática\n" +
            "  • Dashboard visual de datos\n\n" +
            "OTROS PROVEEDORES COMPATIBLES:\n" +
            "  • Railway    → jdbc:postgresql://...\n" +
            "  • Neon       → jdbc:postgresql://...\n" +
            "  • ElephantSQL→ jdbc:postgresql://...\n" +
            "  • Aiven      → jdbc:postgresql://...\n\n" +
            "NOTA: La contraseña de la BD se guarda cifrada en config local."
        );

        pnlHelp.add(lblHelp, BorderLayout.NORTH);
        pnlHelp.add(new JScrollPane(txtHelp), BorderLayout.CENTER);

        JPanel pnlCards = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlCards.setBackground(AppTheme.BG_MAIN);
        pnlCards.add(card);
        pnlCards.add(pnlHelp);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCards,  BorderLayout.CENTER);
    }

    private void saveAndConnect() {
        String url     = txtJdbcUrl.getText().trim();
        boolean enable = tglOnline.isSelected();

        if (enable && url.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Ingresa la cadena JDBC antes de activar.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        lblStatus.setText("⏳ Conectando...");

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                // Guardar en config (cifrado automático por AppConfig)
                cfg.set("online_db_url",     url);
                cfg.set("online_db_enabled", String.valueOf(enable));
                return dbConn.configureOnline(url, enable);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    boolean ok = get();
                    if (ok) {
                        lblStatus.setText("✅ Conectado a BD online correctamente.");
                        tglOnline.setText("✅ BD Online Activa");
                        tglOnline.setBackground(AppTheme.SUCCESS);
                        controller.getLogDao().save(new SystemLog(
                            "DB_ONLINE", "BD online configurada y conectada.", user.getUsername()));
                        JOptionPane.showMessageDialog(OnlineDatabasePanel.this,
                            "✅ Conexión exitosa.\nLos datos ahora se sincronizan en la nube.",
                            "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        lblStatus.setText("❌ No se pudo conectar. Verifica la cadena JDBC y la contraseña.");
                        tglOnline.setSelected(false);
                        tglOnline.setText("⛔ BD Online Inactiva");
                        tglOnline.setBackground(AppTheme.BORDER_MEDIUM);
                    }
                } catch (Exception ex) {
                    lblStatus.setText("❌ Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void testConnection() {
        String url = txtJdbcUrl.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingresa la cadena JDBC primero.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        lblStatus.setText("⏳ Probando conexión...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return dbConn.configureOnline(url, true);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    boolean ok = get();
                    // Solo probar, no guardar
                    dbConn.configureOnline("", false);
                    if (ok) {
                        lblStatus.setText("✅ Prueba exitosa — conexión funciona.");
                        JOptionPane.showMessageDialog(OnlineDatabasePanel.this,
                            "✅ La conexión funciona correctamente.\nPresiona 'Guardar y Conectar' para activarla.",
                            "Prueba exitosa", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        lblStatus.setText("❌ Prueba fallida. Verifica URL y contraseña.");
                    }
                } catch (Exception ex) { lblStatus.setText("❌ " + ex.getMessage()); }
            }
        }.execute();
    }

    public void refresh() {
        String url     = cfg.get("online_db_url");
        boolean enable = "true".equals(cfg.get("online_db_enabled"));
        if (txtJdbcUrl != null) txtJdbcUrl.setText(url);
        if (tglOnline != null) {
            tglOnline.setSelected(enable);
            tglOnline.setText(enable ? "✅ BD Online Activa" : "⛔ BD Online Inactiva");
            tglOnline.setBackground(enable ? AppTheme.SUCCESS : AppTheme.BORDER_MEDIUM);
        }
        if (lblStatus != null) {
            lblStatus.setText(dbConn.isOnlineAvailable()
                ? "✅ Conectado a BD online."
                : "⭕ BD online no conectada.");
        }
    }
}
