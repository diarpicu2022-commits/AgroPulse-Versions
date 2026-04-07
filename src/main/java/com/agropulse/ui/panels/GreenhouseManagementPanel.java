package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.dao.GreenhouseDao;
import com.agropulse.dao.UserDao;
import com.agropulse.model.Greenhouse;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel Administración de Invernaderos.
 * Solo visible para ADMIN.
 *
 * Funciones:
 *  - Crear / editar / eliminar invernaderos
 *  - Asignar y desasignar usuarios a invernaderos
 *  - Ver qué usuarios tienen acceso a cada invernadero
 */
public class GreenhouseManagementPanel extends JPanel {

    private final GreenhouseController controller;
    private final User                 admin;
    private final GreenhouseDao        ghDao;
    private final UserDao              userDao;

    private JTable            tblGH;
    private DefaultTableModel tblGHModel;
    private JTable            tblUsers;
    private DefaultTableModel tblUsersModel;
    private List<Greenhouse>  greenhouses;
    private List<User>        allUsers;

    public GreenhouseManagementPanel(GreenhouseController controller, User admin) {
        this.controller = controller;
        this.admin      = admin;
        this.ghDao      = new GreenhouseDao();
        this.userDao    = new UserDao();
        initComponents();
        refresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Título ──────────────────────────────────────────────────
        JLabel title = new JLabel("🏭 Gestión de Invernaderos");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.PRIMARY_DARK);

        JButton btnRefresh = AppTheme.secondaryButton("🔄 Refrescar");
        btnRefresh.addActionListener(e -> refresh());

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setBackground(AppTheme.BG_MAIN);
        pnlHead.add(title,      BorderLayout.WEST);
        pnlHead.add(btnRefresh, BorderLayout.EAST);

        // ── Split horizontal: lista GH | asignación usuarios ────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildGHPanel(), buildAssignPanel());
        split.setDividerLocation(460);
        split.setResizeWeight(0.5);

        add(pnlHead, BorderLayout.NORTH);
        add(split,   BorderLayout.CENTER);
    }

    // ─── Panel izquierdo: lista de invernaderos ───────────────────────

    private JPanel buildGHPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(AppTheme.cardBorder());

        JLabel lbl = new JLabel("  🏠 Invernaderos");
        lbl.setFont(AppTheme.FONT_SUBTITLE);
        lbl.setForeground(AppTheme.PRIMARY_DARK);

        tblGHModel = new DefaultTableModel(
                new String[]{"#", "Nombre", "Ubicación", "Activo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblGH = new JTable(tblGHModel);
        tblGH.setRowHeight(30);
        tblGH.setFont(AppTheme.FONT_BODY);
        tblGH.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);
        tblGH.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblGH.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshAssignments();
        });

        // Botones
        JButton btnNew  = AppTheme.primaryButton("➕ Nuevo");
        JButton btnEdit = AppTheme.secondaryButton("✏️ Editar");
        JButton btnDel  = AppTheme.secondaryButton("🗑️ Eliminar");

        btnNew.addActionListener(e  -> openGHDialog(null));
        btnEdit.addActionListener(e -> {
            int row = tblGH.getSelectedRow();
            if (row < 0) { warn("Selecciona un invernadero."); return; }
            openGHDialog(greenhouses.get(row));
        });
        btnDel.addActionListener(e -> deleteGH());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.setBackground(Color.WHITE);
        btns.add(btnNew); btns.add(btnEdit); btns.add(btnDel);

        p.add(lbl,                      BorderLayout.NORTH);
        p.add(new JScrollPane(tblGH),   BorderLayout.CENTER);
        p.add(btns,                     BorderLayout.SOUTH);
        return p;
    }

    // ─── Panel derecho: asignación de usuarios ────────────────────────

    private JPanel buildAssignPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(AppTheme.cardBorder());

        JLabel lbl = new JLabel("  👥 Usuarios con acceso");
        lbl.setFont(AppTheme.FONT_SUBTITLE);
        lbl.setForeground(AppTheme.PRIMARY_DARK);

        tblUsersModel = new DefaultTableModel(
                new String[]{"ID", "Usuario", "Nombre completo", "Asignado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblUsers = new JTable(tblUsersModel);
        tblUsers.setRowHeight(30);
        tblUsers.setFont(AppTheme.FONT_BODY);
        tblUsers.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);

        JButton btnAssign   = AppTheme.primaryButton("✅ Asignar");
        JButton btnUnassign = AppTheme.secondaryButton("❌ Desasignar");

        btnAssign.addActionListener(e -> toggleAssignment(true));
        btnUnassign.addActionListener(e -> toggleAssignment(false));

        JLabel hint = new JLabel(
            "<html><small>Selecciona un invernadero (izq.) y un usuario (der.)<br>" +
            "luego usa los botones para asignar/desasignar acceso.</small></html>");
        hint.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.setBackground(Color.WHITE);
        btns.add(btnAssign); btns.add(btnUnassign); btns.add(hint);

        p.add(lbl,                         BorderLayout.NORTH);
        p.add(new JScrollPane(tblUsers),   BorderLayout.CENTER);
        p.add(btns,                        BorderLayout.SOUTH);
        return p;
    }

    // ─── Acciones ─────────────────────────────────────────────────────

    public void refresh() {
        greenhouses = ghDao.findAll();
        allUsers    = userDao.findAll().stream()
                .filter(u -> !u.isAdmin()).toList();

        tblGHModel.setRowCount(0);
        int i = 1;
        for (Greenhouse g : greenhouses) {
            tblGHModel.addRow(new Object[]{
                i++, g.getName(), g.getLocation(), g.isActive() ? "✅" : "⛔"
            });
        }
        refreshAssignments();
    }

    private void refreshAssignments() {
        tblUsersModel.setRowCount(0);
        int ghRow = tblGH.getSelectedRow();
        if (ghRow < 0 || greenhouses == null || ghRow >= greenhouses.size()) return;

        Greenhouse gh = greenhouses.get(ghRow);
        List<Integer> assigned = ghDao.getAssignedUserIds(gh.getId());

        for (User u : allUsers) {
            boolean isAssigned = assigned.contains(u.getId());
            tblUsersModel.addRow(new Object[]{
                u.getId(), u.getUsername(), u.getFullName(),
                isAssigned ? "✅ Sí" : "❌ No"
            });
        }
    }

    private void toggleAssignment(boolean assign) {
        int ghRow   = tblGH.getSelectedRow();
        int userRow = tblUsers.getSelectedRow();
        if (ghRow < 0) { warn("Selecciona un invernadero primero."); return; }
        if (userRow < 0) { warn("Selecciona un usuario primero."); return; }

        Greenhouse gh = greenhouses.get(ghRow);
        User       u  = allUsers.get(userRow);

        if (assign) {
            ghDao.assignUser(u.getId(), gh.getId());
            JOptionPane.showMessageDialog(this,
                "✅ " + u.getFullName() + " ahora tiene acceso a\n\"" + gh.getName() + "\".",
                "Asignado", JOptionPane.INFORMATION_MESSAGE);
        } else {
            ghDao.unassignUser(u.getId(), gh.getId());
            JOptionPane.showMessageDialog(this,
                "❌ " + u.getFullName() + " ya no tiene acceso a\n\"" + gh.getName() + "\".",
                "Desasignado", JOptionPane.INFORMATION_MESSAGE);
        }
        refreshAssignments();
    }

    private void openGHDialog(Greenhouse existing) {
        boolean isNew = existing == null;
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isNew ? "Nuevo Invernadero" : "Editar Invernadero", true);
        dlg.setSize(420, 300);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 16, 8, 16));
        form.setBackground(Color.WHITE);

        JTextField txtName = fld(isNew ? "" : existing.getName());
        JTextField txtLoc  = fld(isNew ? "" : existing.getLocation());
        JTextField txtDesc = fld(isNew ? "" : existing.getDescription());

        addRow(form, "Nombre *", txtName);
        addRow(form, "Ubicación", txtLoc);
        addRow(form, "Descripción", txtDesc);

        JButton btnSave = AppTheme.primaryButton(isNew ? "💾 Crear" : "💾 Guardar");
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { warn("El nombre es obligatorio."); return; }
            if (isNew) {
                Greenhouse g = new Greenhouse(name, txtLoc.getText().trim(),
                        txtDesc.getText().trim(), admin.getId());
                ghDao.save(g);
            } else {
                existing.setName(name);
                existing.setLocation(txtLoc.getText().trim());
                existing.setDescription(txtDesc.getText().trim());
                ghDao.update(existing);
            }
            dlg.dispose();
            refresh();
        });

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnSave, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void deleteGH() {
        int row = tblGH.getSelectedRow();
        if (row < 0) { warn("Selecciona un invernadero."); return; }
        Greenhouse gh = greenhouses.get(row);
        int r = JOptionPane.showConfirmDialog(this,
            "¿Eliminar el invernadero \"" + gh.getName() + "\"?\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            gh.setActive(false);
            ghDao.update(gh);
            refresh();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private JTextField fld(String val) {
        JTextField f = new JTextField(val);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    private void addRow(JPanel p, String lbl, JComponent field) {
        JLabel l = new JLabel(lbl);
        l.setFont(AppTheme.FONT_SMALL);
        l.setForeground(AppTheme.TEXT_SECONDARY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l); p.add(Box.createVerticalStrut(3)); p.add(field); p.add(Box.createVerticalStrut(8));
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
