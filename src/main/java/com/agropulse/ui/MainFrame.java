package com.agropulse.ui;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.SystemLog;
import com.agropulse.model.User;
import com.agropulse.ui.panels.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ventana principal - AgroPulse v2.0.
 *
 * NUEVO: Selector de invernadero en el header.
 *        Al cambiar el invernadero, el panel activo se refresca
 *        para mostrar solo datos de ese invernadero.
 */
public class MainFrame extends javax.swing.JFrame {

    private final User user;
    private final GreenhouseController controller;

    private final Map<String, JComponent> panels = new LinkedHashMap<>();
    private final Map<String, String[]>   menu   = new LinkedHashMap<>();

    private GreenhouseSelectorPanel ghSelector;

    // ─── Constructor ────────────────────────────────────
    public MainFrame(User user, GreenhouseController controller) {
        this.user       = user;
        this.controller = controller;
        buildMenu();
        initComponents();
        setupEvents();
        showPanel("sensores");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    // ─── Menú por rol ────────────────────────────────────
    private void buildMenu() {
        menu.put("sensores",   new String[]{"📊", "Monitoreo Sensores"});
        menu.put("actuadores", new String[]{"🔧", "Control Actuadores"});
        menu.put("cultivos",   new String[]{"🌿", "Gestión Cultivos"});
        menu.put("simular",    new String[]{"📈", "Simular Lecturas"});
        menu.put("alertas",    new String[]{"🔔", "Alertas"});
        menu.put("ia",         new String[]{"🤖", "Consultar IA"});
        menu.put("esp32",      new String[]{"📟", "Conectar ESP32"});
        menu.put("ml",         new String[]{"🧠", "ML Predicciones"});
        menu.put("soporte",    new String[]{"🎧", "Soporte Técnico"});
        if (user.isAdmin()) {
            menu.put("invernaderos", new String[]{"🏭", "Mis Invernaderos"});
            menu.put("rangos",   new String[]{"⚙️",  "Configurar Rangos"});
            menu.put("logs",     new String[]{"📋", "Logs del Sistema"});
            menu.put("exportar", new String[]{"📥", "Exportar Datos"});
            menu.put("apis",     new String[]{"🔌", "Configurar APIs"});
            menu.put("dbonline", new String[]{"☁️",  "BD Online"});
            menu.put("config",   new String[]{"🛠️",  "Configuración"});
            menu.put("usuarios", new String[]{"👥", "Gestionar Usuarios"});
        }
    }

    // ─── initComponents ──────────────────────────────────
    private void initComponents() {
        setTitle("AgroPulse v2.0  –  " + user.getFullName()
                 + "  (" + user.getRole().getDisplayName() + ")");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setPreferredSize(new Dimension(1120, 720));

        pnlRoot = new JPanel(new BorderLayout());
        pnlRoot.setBackground(AppTheme.BG_MAIN);

        // ── Header ─────────────────────────────────────────────────
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_HEADER);
        pnlHeader.setPreferredSize(new Dimension(0, 58));
        pnlHeader.setBorder(new EmptyBorder(0, 20, 0, 20));

        lblAppName = new JLabel("🌿 AgroPulse");
        lblAppName.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        lblAppName.setForeground(Color.WHITE);

        lblCurrentSection = new JLabel("Inicio");
        lblCurrentSection.setFont(AppTheme.FONT_BODY);
        lblCurrentSection.setForeground(new Color(0xA5D6A7));

        // ── Selector de invernadero (NUEVO) ─────────────────────────
        ghSelector = new GreenhouseSelectorPanel(controller, user);
        ghSelector.setOnChangeCallback((id, name) -> {
            // Al cambiar invernadero → refrescar el panel activo
            refreshCurrentPanel();
        });

        pnlHeaderRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 15));
        pnlHeaderRight.setBackground(AppTheme.BG_HEADER);

        lblUserName = new JLabel("👤 " + user.getFullName());
        lblUserName.setFont(AppTheme.FONT_BODY);
        lblUserName.setForeground(Color.WHITE);

        lblUserRole = new JLabel("[" + user.getRole().getDisplayName() + "]");
        lblUserRole.setFont(AppTheme.FONT_SMALL);
        lblUserRole.setForeground(new Color(0xA5D6A7));

        btnLogout = new JButton("<html><span style='font-family: \"Segoe UI Emoji\"'>⏻</span> Salir</html>");
        btnLogout.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        btnLogout.setBackground(AppTheme.PRIMARY_DARK);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setPreferredSize(new Dimension(95, 30));
        btnLogout.setMargin(new Insets(2, 5, 2, 5));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        pnlHeaderRight.add(lblUserName);
        pnlHeaderRight.add(lblUserRole);
        pnlHeaderRight.add(btnLogout);

        // Centro del header: selector de invernadero + sección actual
        JPanel pnlCenter = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        pnlCenter.setOpaque(false);
        pnlCenter.add(lblCurrentSection);
        pnlCenter.add(ghSelector);

        pnlHeader.add(lblAppName,    BorderLayout.WEST);
        pnlHeader.add(pnlCenter,     BorderLayout.CENTER);
        pnlHeader.add(pnlHeaderRight, BorderLayout.EAST);

        // ── Sidebar ────────────────────────────────────────────────
        pnlSidebar = new JPanel();
        pnlSidebar.setLayout(new BoxLayout(pnlSidebar, BoxLayout.Y_AXIS));
        pnlSidebar.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlSidebar.setBorder(new EmptyBorder(10, 0, 10, 0));
        pnlSidebar.setBackground(AppTheme.BG_SIDEBAR);
        pnlSidebar.setPreferredSize(new Dimension(AppTheme.SIDEBAR_WIDTH, 0));

        if (!user.isAdmin() && user.getAssignedGreenhouseName() != null
                && !user.getAssignedGreenhouseName().isBlank()) {
            JLabel lblGH = new JLabel("🏠 " + user.getAssignedGreenhouseName());
            lblGH.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblGH.setForeground(new Color(0xA5D6A7));
            lblGH.setBorder(new EmptyBorder(0, 16, 6, 0));
            pnlSidebar.add(lblGH);
        }

        pnlRoleBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlRoleBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlRoleBadge.setBackground(AppTheme.PRIMARY_DARK);
        pnlRoleBadge.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        lblRoleBadge = new JLabel(user.isAdmin() ? "🔑 Administrador" : "👤 Usuario");
        lblRoleBadge.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        lblRoleBadge.setBorder(new EmptyBorder(2, 0, 0, 0));
        lblRoleBadge.setForeground(new Color(0xC8E6C9));
        pnlRoleBadge.add(lblRoleBadge);

        pnlSidebar.add(pnlRoleBadge);
        pnlSidebar.add(Box.createVerticalStrut(6));

        menu.forEach((key, val) -> pnlSidebar.add(buildNavBtn(val[0] + "  " + val[1], key)));
        pnlSidebar.add(Box.createVerticalGlue());

        lblVersion = new JLabel("v2.0 – Nariño, Colombia");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblVersion.setForeground(new Color(0x558B2F));
        lblVersion.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblVersion.setBorder(new EmptyBorder(0, 10, 4, 0));
        pnlSidebar.add(lblVersion);

        // ── Content (CardLayout) ───────────────────────────────────
        cardLayout = new CardLayout();
        pnlContent = new JPanel(cardLayout);
        pnlContent.setBackground(AppTheme.BG_MAIN);

        addPanel("sensores",   new SensorPanel(controller));
        addPanel("actuadores", new ActuatorPanel(controller, user));
        addPanel("cultivos",   new CropPanel(controller, user));
        addPanel("simular",    new SimulationPanel(controller, user));
        addPanel("alertas",    new AlertsPanel(controller));
        addPanel("ia",         new AIPanel(controller));
        addPanel("esp32",      new ESP32Panel(controller));
        addPanel("ml",         new MLPanel(controller));
        addPanel("soporte",    new SupportPanel(controller, user));
        if (user.isAdmin()) {
            addPanel("invernaderos", new GreenhouseManagementPanel(controller, user));
            addPanel("rangos",   new ConfigRangesPanel(controller, user));
            addPanel("logs",     new LogsPanel(controller));
            addPanel("exportar", new DataExportPanel(controller));
            addPanel("apis",     new APIConfigPanel(controller, user));
            addPanel("dbonline", new OnlineDatabasePanel(controller, user));
            addPanel("config",   new SystemConfigPanel(controller, user));
            addPanel("usuarios", new UserManagementPanel(controller, user));
        }

        pnlRoot.add(pnlHeader,  BorderLayout.NORTH);
        pnlRoot.add(pnlSidebar, BorderLayout.WEST);
        pnlRoot.add(pnlContent, BorderLayout.CENTER);

        setContentPane(pnlRoot);
        pack();

        try {
            java.net.URL url = getClass().getResource("/icon.png");
            if (url != null) setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(url));
        } catch (Exception e) {
            System.err.println("Error al cargar el icono: " + e.getMessage());
        }
    }

    private void setupEvents() {
        btnLogout.addActionListener(e -> logout());
    }

    // ─── Navegación ───────────────────────────────────────────────
    private JButton buildNavBtn(String text, String key) {
        JButton btn = new JButton(text);
        btn.setFont(AppTheme.FONT_BODY);
        btn.setForeground(new Color(0xC8E6C9));
        btn.setBackground(AppTheme.BG_SIDEBAR);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBorder(new EmptyBorder(3, 8, 0, 0));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(AppTheme.PRIMARY); btn.setForeground(Color.WHITE);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(AppTheme.BG_SIDEBAR); btn.setForeground(new Color(0xC8E6C9));
            }
        });
        btn.addActionListener(e -> showPanel(key));
        return btn;
    }

    private void addPanel(String key, JComponent panel) {
        panels.put(key, panel);
        pnlContent.add(panel, key);
    }

    public void showPanel(String key) {
        if (!panels.containsKey(key)) return;
        cardLayout.show(pnlContent, key);
        String[] meta = menu.get(key);
        if (meta != null) lblCurrentSection.setText(meta[0] + "  " + meta[1]);
        JComponent p = panels.get(key);
        if (p instanceof Refreshable refreshable) refreshable.refresh();
    }

    /** Refresca el panel que esté visible actualmente. */
    private void refreshCurrentPanel() {
        panels.forEach((key, panel) -> {
            if (panel.isVisible() && panel instanceof Refreshable r) {
                r.refresh();
            }
        });
    }

    private void logout() {
        int ok = JOptionPane.showConfirmDialog(this, "¿Cerrar sesión?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            controller.getLogDao().save(new SystemLog("LOGOUT", "Cierre de sesión", user.getUsername()));
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    /** Interfaz para paneles que se refrescan al mostrarse o al cambiar invernadero. */
    public interface Refreshable { void refresh(); }

    // ─── Variables privadas ───────────────────────────────────────
    private javax.swing.JPanel   pnlRoot;
    private javax.swing.JPanel   pnlHeader;
    private javax.swing.JPanel   pnlHeaderRight;
    private javax.swing.JPanel   pnlSidebar;
    private javax.swing.JPanel   pnlRoleBadge;
    private javax.swing.JPanel   pnlContent;
    private javax.swing.JLabel   lblAppName;
    private javax.swing.JLabel   lblCurrentSection;
    private javax.swing.JLabel   lblUserName;
    private javax.swing.JLabel   lblUserRole;
    private javax.swing.JLabel   lblRoleBadge;
    private javax.swing.JLabel   lblVersion;
    private javax.swing.JButton  btnLogout;
    private java.awt.CardLayout  cardLayout;
}
