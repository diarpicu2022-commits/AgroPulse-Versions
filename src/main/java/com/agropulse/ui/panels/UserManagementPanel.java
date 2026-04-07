package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.dao.UserDao;
import com.agropulse.model.SystemLog;
import com.agropulse.model.User;
import com.agropulse.model.enums.UserRole;
import com.agropulse.ui.AppTheme;
import com.agropulse.ui.MainFrame.Refreshable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Panel Gestión de Usuarios – compatible NetBeans */
public class UserManagementPanel extends javax.swing.JPanel implements Refreshable {

    private final GreenhouseController controller;
    private final User currentUser;
    private final UserDao userDao;

    public UserManagementPanel(GreenhouseController controller, User currentUser) {
        this.controller  = controller;
        this.currentUser = currentUser;
        this.userDao     = new UserDao();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Header
        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("👥 Gestión de Usuarios");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);

        pnlHBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlHBtns.setBackground(AppTheme.BG_MAIN);

        btnNew = AppTheme.primaryButton("➕ Nuevo Usuario");
        btnNew.setPreferredSize(new Dimension(155, 32));
        btnNew.addActionListener(e -> showNewDialog());

        btnToggleUser = AppTheme.secondaryButton("🔄 Act/Desact.");
        btnToggleUser.setPreferredSize(new Dimension(135, 32));
        btnToggleUser.addActionListener(e -> toggleSelected());

        btnRefresh = AppTheme.secondaryButton("🔄");
        btnRefresh.setPreferredSize(new Dimension(44, 32));
        btnRefresh.addActionListener(e -> refresh());

        pnlHBtns.add(btnNew);
        pnlHBtns.add(btnToggleUser);
        pnlHBtns.add(btnRefresh);

        pnlHeader.add(lblTitle,  BorderLayout.WEST);
        pnlHeader.add(pnlHBtns, BorderLayout.EAST);

        // Tabla
        tblModel = new DefaultTableModel(
            new String[]{"ID","Usuario","Nombre Completo","Teléfono","Rol","Activo","Creado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblUsers = AppTheme.styledTable(
            new String[]{"ID","Usuario","Nombre Completo","Teléfono","Rol","Activo","Creado"}, new Object[0][0]);
        tblUsers.setModel(tblModel);
        tblUsers.getColumnModel().getColumn(0).setPreferredWidth(40);
        tblUsers.getColumnModel().getColumn(1).setPreferredWidth(120);
        tblUsers.getColumnModel().getColumn(2).setPreferredWidth(180);

        pnlCard = new JPanel(new BorderLayout());
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(AppTheme.cardBorder());
        pnlCard.add(new JScrollPane(tblUsers), BorderLayout.CENTER);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCard,   BorderLayout.CENTER);

        refresh();
    }

    private void showNewDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nuevo Usuario", true);
        dlg.setSize(380, 420);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JTextField txUser  = AppTheme.textField(); txUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));  txUser.setAlignmentX(LEFT_ALIGNMENT);
        JTextField txName  = AppTheme.textField(); txName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));  txName.setAlignmentX(LEFT_ALIGNMENT);
        JPasswordField txPass = AppTheme.passwordField(); txPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36)); txPass.setAlignmentX(LEFT_ALIGNMENT);
        JTextField txPhone = AppTheme.textField(); txPhone.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36)); txPhone.setAlignmentX(LEFT_ALIGNMENT);
        JComboBox<UserRole> cmbRole = new JComboBox<>(UserRole.values()); cmbRole.setAlignmentX(LEFT_ALIGNMENT);
        cmbRole.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        String[][] rows = {{"Usuario (login):"},{"Nombre completo:"},{"Contraseña:"},{"Teléfono (+57...):"} };
        JComponent[] inputs = {txUser, txName, txPass, txPhone};
        for (int i = 0; i < rows.length; i++) {
            JLabel l = new JLabel(rows[i][0]); l.setFont(AppTheme.FONT_SMALL); l.setForeground(AppTheme.TEXT_SECONDARY); l.setAlignmentX(LEFT_ALIGNMENT);
            p.add(l); p.add(Box.createVerticalStrut(3)); p.add(inputs[i]); p.add(Box.createVerticalStrut(10));
        }
        JLabel lRole = new JLabel("Rol:"); lRole.setFont(AppTheme.FONT_SMALL); lRole.setForeground(AppTheme.TEXT_SECONDARY); lRole.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lRole); p.add(Box.createVerticalStrut(3)); p.add(cmbRole); p.add(Box.createVerticalStrut(16));

        JButton btnSave = AppTheme.primaryButton("💾 Crear Usuario");
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38)); btnSave.setAlignmentX(LEFT_ALIGNMENT);
        btnSave.addActionListener(e -> {
            if (txUser.getText().isBlank() || txName.getText().isBlank()) {
                JOptionPane.showMessageDialog(dlg, "Usuario y nombre son obligatorios.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            User u = new User(txUser.getText().trim(), new String(txPass.getPassword()),
                txName.getText().trim(), txPhone.getText().trim(), (UserRole) cmbRole.getSelectedItem());
            userDao.save(u);
            controller.getLogDao().save(new SystemLog("USUARIO_NUEVO", "Creado: " + u.getUsername(), currentUser.getUsername()));
            refresh();
            dlg.dispose();
            JOptionPane.showMessageDialog(this, "✅ Usuario creado.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        });
        p.add(btnSave);

        dlg.setContentPane(new JScrollPane(p));
        dlg.setVisible(true);
    }

    private void toggleSelected() {
        int row = tblUsers.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this,"Selecciona un usuario.","Aviso",JOptionPane.WARNING_MESSAGE); return; }
        List<User> all = userDao.findAll();
        if (row >= all.size()) return;
        User u = all.get(row);
        if (u.getUsername().equals(currentUser.getUsername())) {
            JOptionPane.showMessageDialog(this,"No puedes desactivar tu propia cuenta.","Error",JOptionPane.ERROR_MESSAGE); return;
        }
        u.setActive(!u.isActive());
        userDao.update(u);
        refresh();
    }

    @Override
    public void refresh() {
        tblModel.setRowCount(0);
        userDao.findAll().forEach(u -> tblModel.addRow(new Object[]{
            u.getId(), u.getUsername(), u.getFullName(), u.getPhone(),
            u.getRole().getDisplayName(), u.isActive() ? "✅ Sí" : "❌ No",
            u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate() : ""
        }));
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel   pnlHeader;
    private javax.swing.JPanel   pnlHBtns;
    private javax.swing.JPanel   pnlCard;
    private javax.swing.JLabel   lblTitle;
    private javax.swing.JButton  btnNew;
    private javax.swing.JButton  btnToggleUser;
    private javax.swing.JButton  btnRefresh;
    private javax.swing.JTable   tblUsers;
    private javax.swing.table.DefaultTableModel tblModel;
}
