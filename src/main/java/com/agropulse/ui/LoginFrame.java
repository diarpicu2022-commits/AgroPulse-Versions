package com.agropulse.ui;

import com.agropulse.auth.GoogleAuthService;
import com.agropulse.auth.GoogleAuthService.GoogleUser;
import com.agropulse.controller.GreenhouseController;
import com.agropulse.dao.GreenhouseDao;
import com.agropulse.dao.UserDao;
import com.agropulse.model.Greenhouse;
import com.agropulse.model.SystemLog;
import com.agropulse.model.User;
import com.agropulse.model.enums.UserRole;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Optional;

/**
 * Ventana de inicio de sesión - AgroPulse.
 * Compatible con NetBeans GUI Builder.
 *
 * Para editar en NetBeans: clic derecho → Open → Design
 */
public class LoginFrame extends javax.swing.JFrame {

    private final UserDao userDao;
    private final GreenhouseDao greenhouseDao;
    private final GreenhouseController controller;
    private final GoogleAuthService googleAuth;

    // ─── Constructor ────────────────────────────────────
    public LoginFrame() {
        this.userDao    = new UserDao();
        this.greenhouseDao = new GreenhouseDao();
        this.controller = new GreenhouseController();
        this.googleAuth = new GoogleAuthService();
        initComponents();
        setupActions();
        setLocationRelativeTo(null);
    }

    // ────────────────────────────────────────────────────
    //  initComponents  (editable en NetBeans Design)
    // ────────────────────────────────────────────────────
    private void initComponents() {
        setTitle("AgroPulse – Inicio de Sesión");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setPreferredSize(new Dimension(420, 570));

        // ── Panel raíz ─────────────────────────────────
        pnlRoot = new JPanel(new BorderLayout());
        pnlRoot.setBackground(AppTheme.BG_MAIN);

        // ── Header verde ───────────────────────────────
        pnlHeader = new JPanel();
        pnlHeader.setBackground(AppTheme.PRIMARY);
        pnlHeader.setPreferredSize(new Dimension(420, 145));
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setBorder(new EmptyBorder(18, 20, 18, 20));

        lblLogo = new JLabel("🌿 AgroPulse");
        lblLogo.setFont(new Font("Segoe UI Emoji", Font.BOLD, 32));
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblSubtitle = new JLabel("Sistema de Monitoreo de Invernadero");
        lblSubtitle.setFont(AppTheme.FONT_SMALL);
        lblSubtitle.setForeground(new Color(0xA5D6A7));
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblUniversity = new JLabel("Universidad Cooperativa de Colombia");
        lblUniversity.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblUniversity.setForeground(new Color(0x81C784));
        lblUniversity.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlHeader.add(Box.createVerticalGlue());
        pnlHeader.add(lblLogo);
        pnlHeader.add(Box.createVerticalStrut(6));
        pnlHeader.add(lblSubtitle);
        pnlHeader.add(Box.createVerticalStrut(4));
        pnlHeader.add(lblUniversity);
        pnlHeader.add(Box.createVerticalGlue());

        // ── Formulario ─────────────────────────────────
        pnlForm = new JPanel();
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.Y_AXIS));
        pnlForm.setBackground(AppTheme.BG_MAIN);
        pnlForm.setBorder(new EmptyBorder(28, 40, 24, 40));

        lblFormTitle = new JLabel("Iniciar Sesión");
        lblFormTitle.setFont(AppTheme.FONT_TITLE);
        lblFormTitle.setForeground(AppTheme.TEXT_PRIMARY);
   lblFormTitle.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER

        lblUserLbl = makeFieldLabel("Usuario");
        txtUsername = AppTheme.textField();
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsername.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER

        lblPassLbl = makeFieldLabel("Contraseña");
        txtPassword = AppTheme.passwordField();
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtPassword.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER

        lblStatus = new JLabel(" ");
        lblStatus.setFont(AppTheme.FONT_SMALL);
        lblStatus.setForeground(AppTheme.DANGER);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER

        btnLogin = AppTheme.primaryButton("Ingresar");
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER

        // Separador "ó"
        pnlDivider = buildDivider();
        pnlDivider.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER

        btnGoogle = buildGoogleButton();
        btnGoogle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnGoogle.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER

        lblLoading = new JLabel("🌐 Abriendo Google...");
        lblLoading.setFont(AppTheme.FONT_SMALL);
        lblLoading.setForeground(AppTheme.TEXT_SECONDARY);
        lblLoading.setAlignmentX(Component.CENTER_ALIGNMENT); // Cambiado a CENTER
        lblLoading.setVisible(false);

        lblFooter = new JLabel("AgroPulse v1.0  –  Nariño, Colombia");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblFooter.setForeground(AppTheme.TEXT_MUTED);
        lblFooter.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlForm.add(lblFormTitle);
        pnlForm.add(Box.createVerticalStrut(20));
        pnlForm.add(lblUserLbl);
        pnlForm.add(Box.createVerticalStrut(4));
        pnlForm.add(txtUsername);
        pnlForm.add(Box.createVerticalStrut(12));
        pnlForm.add(lblPassLbl);
        pnlForm.add(Box.createVerticalStrut(4));
        pnlForm.add(txtPassword);
        pnlForm.add(Box.createVerticalStrut(6));
        pnlForm.add(lblStatus);
        pnlForm.add(Box.createVerticalStrut(8));
        pnlForm.add(btnLogin);
        pnlForm.add(Box.createVerticalStrut(16));
        pnlForm.add(pnlDivider);
        pnlForm.add(Box.createVerticalStrut(14));
        pnlForm.add(btnGoogle);
        pnlForm.add(Box.createVerticalStrut(6));
        pnlForm.add(lblLoading);
        pnlForm.add(Box.createVerticalGlue());
        pnlForm.add(lblFooter);

        pnlRoot.add(pnlHeader, BorderLayout.NORTH);
        pnlRoot.add(pnlForm,   BorderLayout.CENTER);

        
        
        setContentPane(pnlRoot);
        pack();
        
        try {
    // Usamos el cargador de recursos del sistema
    java.net.URL url = getClass().getResource("/icon.png"); 
    if (url != null) {
        Image icono = java.awt.Toolkit.getDefaultToolkit().getImage(url);
        this.setIconImage(icono);
    } else {
        System.out.println("Error: No se encontró el archivo icon.png en la ruta especificada.");
    }
} catch (Exception e) {
    System.err.println("Error al cargar el icono: " + e.getMessage());
}
        
        
    }

    // ────────────────────────────────────────────────────
    //  Acciones / Eventos
    // ────────────────────────────────────────────────────
    private void setupActions() {
        btnLogin.addActionListener(e -> doLogin());
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        });
        btnGoogle.addActionListener(e -> doGoogleLogin());
    }

    private void doLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            setStatus("Completa todos los campos.", false); return;
        }
        btnLogin.setEnabled(false);
        btnLogin.setText("Verificando...");

        new SwingWorker<Optional<User>, Void>() {
            @Override protected Optional<User> doInBackground() {
                return userDao.authenticate(user, pass);
            }
            @Override protected void done() {
                try {
                    Optional<User> result = get();
                    if (result.isPresent()) loginSuccess(result.get());
                    else setStatus("❌ Credenciales incorrectas.", false);
                } catch (Exception ex) {
                    setStatus("Error: " + ex.getMessage(), false);
                } finally {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Ingresar");
                }
            }
        }.execute();
    }

    private void doGoogleLogin() {
        btnGoogle.setEnabled(false);
        lblLoading.setVisible(true);

        googleAuth.authenticate(
            gu -> SwingUtilities.invokeLater(() -> {
                lblLoading.setVisible(false);
                btnGoogle.setEnabled(true);
                handleGoogleUser(gu);
            }),
            err -> SwingUtilities.invokeLater(() -> {
                lblLoading.setVisible(false);
                btnGoogle.setEnabled(true);
                setStatus("❌ " + err, false);
            })
        );
    }

    private void handleGoogleUser(GoogleUser gu) {
        Optional<User> existing = userDao.findAll().stream()
            .filter(u -> gu.email.equals(u.getUsername())).findFirst();
        if (existing.isPresent()) {
            loginSuccess(existing.get());
        } else {
            User newUser = new User(gu.email, "GOOGLE_" + gu.sub, gu.name, "", UserRole.USER);
            userDao.save(newUser);
            loginSuccess(newUser);
        }
    }

    private void loginSuccess(User user) {
        controller.getLogDao().save(new SystemLog("LOGIN", "Sesión iniciada", user.getUsername()));

        if (!user.isAdmin()) {
            // Verificar invernaderos asignados al usuario
            java.util.List<Greenhouse> assigned = greenhouseDao.findByUser(user.getId());
            if (assigned.isEmpty()) {
                // Si no tiene invernadero asignado, usar el primero disponible
                java.util.List<Greenhouse> all = greenhouseDao.findAll();
                if (!all.isEmpty()) {
                    greenhouseDao.assignUser(user.getId(), all.get(0).getId());
                    assigned = greenhouseDao.findByUser(user.getId());
                }
            }
            // Si tiene más de uno, mostrar selector
            if (assigned.size() > 1) {
                String[] opts = assigned.stream().map(Greenhouse::toString).toArray(String[]::new);
                String sel = (String) JOptionPane.showInputDialog(this,
                    "Selecciona el invernadero al que deseas acceder:",
                    "🌿 Seleccionar Invernadero", JOptionPane.PLAIN_MESSAGE,
                    null, opts, opts[0]);
                if (sel == null) { return; } // canceló
                final int idx = java.util.Arrays.asList(opts).indexOf(sel);
                user.setAssignedGreenhouseId(assigned.get(idx).getId());
                user.setAssignedGreenhouseName(assigned.get(idx).getName());
            } else if (!assigned.isEmpty()) {
                user.setAssignedGreenhouseId(assigned.get(0).getId());
                user.setAssignedGreenhouseName(assigned.get(0).getName());
            }
        }

        setVisible(false);
        dispose();
        final User finalUser = user;
        SwingUtilities.invokeLater(() -> new MainFrame(finalUser, controller).setVisible(true));
    }

    private void setStatus(String msg, boolean ok) {
        lblStatus.setText(msg);
        lblStatus.setForeground(ok ? AppTheme.SUCCESS : AppTheme.DANGER);
    }

    // ────────────────────────────────────────────────────
    //  Helpers UI
    // ────────────────────────────────────────────────────
    private JLabel makeFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.FONT_SMALL);
        l.setForeground(AppTheme.TEXT_SECONDARY);
        l.setAlignmentX(Component.CENTER_ALIGNMENT); 
        l.setHorizontalAlignment(SwingConstants.CENTER); // <-- AÑADE ESTA LÍNEA
        return l;
    }

    private JPanel buildDivider() {
        JPanel p = new JPanel(new GridBagLayout()); // Mejor layout para centrar esto
        p.setBackground(AppTheme.BG_MAIN);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        
        JSeparator l = new JSeparator(); 
        l.setForeground(AppTheme.BORDER_MEDIUM);
        
        JSeparator r = new JSeparator(); 
        r.setForeground(AppTheme.BORDER_MEDIUM);
        
        JLabel or = new JLabel("  ó  "); 
        or.setFont(AppTheme.FONT_SMALL);
        or.setForeground(AppTheme.TEXT_SECONDARY);
        or.setHorizontalAlignment(SwingConstants.CENTER); // Centramos el texto
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        p.add(l, gbc);
        
        gbc.weightx = 0.0;
        p.add(or, gbc);
        
        gbc.weightx = 1.0;
        p.add(r, gbc);
        
        return p;
    }

    private JButton buildGoogleButton() {
        JButton btn = new JButton("<html><b style='color:#4285F4'>G</b>&nbsp; Continuar con Google</html>");
        btn.setBackground(Color.WHITE);
        btn.setForeground(AppTheme.TEXT_PRIMARY);
        btn.setFont(AppTheme.FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(AppTheme.BORDER_MEDIUM, 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0xF5F5F5)); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(Color.WHITE); }
        });
        return btn;
    }

    // ────────────────────────────────────────────────────
    //  Variables (campos privados - estilo NetBeans)
    // ────────────────────────────────────────────────────
    private javax.swing.JPanel      pnlRoot;
    private javax.swing.JPanel      pnlHeader;
    private javax.swing.JPanel      pnlForm;
    private javax.swing.JPanel      pnlDivider;
    private javax.swing.JLabel      lblLogo;
    private javax.swing.JLabel      lblSubtitle;
    private javax.swing.JLabel      lblUniversity;
    private javax.swing.JLabel      lblFormTitle;
    private javax.swing.JLabel      lblUserLbl;
    private javax.swing.JLabel      lblPassLbl;
    private javax.swing.JLabel      lblStatus;
    private javax.swing.JLabel      lblLoading;
    private javax.swing.JLabel      lblFooter;
    private javax.swing.JTextField  txtUsername;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JButton     btnLogin;
    private javax.swing.JButton     btnGoogle;
}
