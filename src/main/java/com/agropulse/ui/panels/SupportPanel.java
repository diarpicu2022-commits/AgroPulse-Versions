package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.dao.GreenhouseDao;
import com.agropulse.dao.SupportTicketDao;
import com.agropulse.model.Greenhouse;
import com.agropulse.model.SupportTicket;
import com.agropulse.model.SupportTicket.Priority;
import com.agropulse.model.SupportTicket.Status;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * Panel de Soporte Técnico.
 *
 * Vista USUARIO:  solo puede CREAR tickets y VER sus tickets + respuesta del admin.
 *                 NO puede responder ni cambiar estado (solo el admin puede).
 *
 * Vista ADMIN:    ve todos los tickets, RESPONDE, cambia estado y prioridad.
 */
public class SupportPanel extends JPanel {

    private final GreenhouseController controller;
    private final User                 user;
    private final SupportTicketDao     ticketDao;
    private final GreenhouseDao        greenhouseDao;

    private JTable             tbl;
    private DefaultTableModel  tblModel;
    private List<SupportTicket> currentTickets;

    private JTextArea  txtDetail;
    private JTextArea  txtResponse;   // Solo visible para ADMIN
    private JComboBox<String> cmbStatus;
    private JComboBox<String> cmbPriority;
    private JButton    btnRespond;    // Solo visible para ADMIN
    private JButton    btnNew;        // Solo visible para USUARIO

    public SupportPanel(GreenhouseController controller, User user) {
        this.controller    = controller;
        this.user          = user;
        this.ticketDao     = new SupportTicketDao();
        this.greenhouseDao = new GreenhouseDao();
        initComponents();
        refresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // ── Título ──────────────────────────────────────────────────
        JLabel title = new JLabel(user.isAdmin()
                ? "🎧 Soporte Técnico — Panel Administrador"
                : "🎧 Soporte Técnico");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.PRIMARY_DARK);

        JButton btnRefresh = AppTheme.secondaryButton("🔄 Actualizar");
        btnRefresh.addActionListener(e -> refresh());

        JPanel pnlHead = new JPanel(new BorderLayout());
        pnlHead.setBackground(AppTheme.BG_MAIN);
        pnlHead.add(title,      BorderLayout.WEST);
        pnlHead.add(btnRefresh, BorderLayout.EAST);

        // ── Columnas de la tabla ─────────────────────────────────────
        String[] cols = user.isAdmin()
                ? new String[]{"#", "Usuario", "Invernadero", "Asunto", "Estado", "Prioridad", "Fecha"}
                : new String[]{"#", "Asunto", "Estado", "Prioridad", "Fecha"};

        tblModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tbl = new JTable(tblModel);
        tbl.setRowHeight(30);
        tbl.setFont(AppTheme.FONT_BODY);
        tbl.getTableHeader().setFont(AppTheme.FONT_SUBTITLE);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Renderer para colorear estados
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean focus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t,v,sel,focus,r,c);
                if (!sel) {
                    int estadoCol = user.isAdmin() ? 4 : 2;
                    String estado = (String) tblModel.getValueAt(r, estadoCol);
                    if (estado != null) {
                        if (estado.contains("Abierto"))    comp.setBackground(new Color(0xFFF9C4));
                        else if (estado.contains("proceso")) comp.setBackground(new Color(0xBBDEFB));
                        else if (estado.contains("Resuelto")) comp.setBackground(new Color(0xC8E6C9));
                        else comp.setBackground(new Color(0xF5F5F5));
                    } else comp.setBackground(Color.WHITE);
                    comp.setForeground(AppTheme.TEXT_PRIMARY);
                }
                setBorder(new EmptyBorder(0,6,0,6));
                return comp;
            }
        });

        tbl.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });

        // ── Panel detalle ────────────────────────────────────────────
        JPanel pnlDetail = buildDetailPanel();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(tbl), pnlDetail);
        split.setDividerLocation(260);
        split.setResizeWeight(0.5);

        // ── Botón nuevo ticket (SOLO usuarios no-admin) ──────────────
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlBottom.setBackground(AppTheme.BG_MAIN);
        if (!user.isAdmin()) {
            btnNew = AppTheme.primaryButton("➕ Nuevo Ticket");
            btnNew.addActionListener(e -> openNewTicketDialog());
            pnlBottom.add(btnNew);

            // Info de contacto del soporte técnico
            JLabel lblInfo = new JLabel(
                "<html><span style='color:#555;font-size:11px'>"
                + "📞 Soporte: soporte@agropulse.co &nbsp;|&nbsp; "
                + "🕑 Horario: Lun–Vie 8:00–17:00</span></html>");
            pnlBottom.add(Box.createHorizontalStrut(16));
            pnlBottom.add(lblInfo);
        }

        add(pnlHead,   BorderLayout.NORTH);
        add(split,     BorderLayout.CENTER);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private JPanel buildDetailPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(AppTheme.cardBorder());

        JLabel lbl = new JLabel("  📋 Detalle del Ticket");
        lbl.setFont(AppTheme.FONT_SUBTITLE);
        lbl.setForeground(AppTheme.PRIMARY_DARK);

        // Área descripción (solo lectura siempre)
        txtDetail = new JTextArea(4, 40);
        txtDetail.setEditable(false);
        txtDetail.setLineWrap(true);
        txtDetail.setWrapStyleWord(true);
        txtDetail.setFont(AppTheme.FONT_BODY);
        txtDetail.setBackground(new Color(0xFAFAFA));
        txtDetail.setBorder(new EmptyBorder(6,8,6,8));

        JPanel center;

        if (user.isAdmin()) {
            // ── Vista ADMIN: puede responder y cambiar estado ────────
            txtResponse = new JTextArea(3, 40);
            txtResponse.setLineWrap(true);
            txtResponse.setWrapStyleWord(true);
            txtResponse.setFont(AppTheme.FONT_BODY);
            txtResponse.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_MEDIUM));

            JPanel pnlControls = new JPanel(new GridLayout(2, 2, 8, 8));
            pnlControls.setOpaque(false);
            cmbStatus = new JComboBox<>(new String[]{
                "🟡 Abierto", "🔵 En proceso", "🟢 Resuelto", "⚫ Cerrado"});
            cmbPriority = new JComboBox<>(new String[]{
                "🔽 Baja", "▶️ Media", "🔼 Alta", "🚨 Crítica"});
            pnlControls.add(new JLabel("Estado:"));   pnlControls.add(cmbStatus);
            pnlControls.add(new JLabel("Prioridad:")); pnlControls.add(cmbPriority);

            btnRespond = AppTheme.primaryButton("💬 Enviar Respuesta");
            btnRespond.addActionListener(e -> sendAdminResponse());

            JPanel pnlResp = new JPanel(new BorderLayout(4,4));
            pnlResp.setOpaque(false);
            pnlResp.add(new JLabel("Respuesta del administrador:"), BorderLayout.NORTH);
            pnlResp.add(new JScrollPane(txtResponse), BorderLayout.CENTER);
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btns.setOpaque(false);
            btns.add(pnlControls);
            btns.add(btnRespond);
            pnlResp.add(btns, BorderLayout.SOUTH);

            JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(txtDetail), pnlResp);
            sp.setDividerLocation(0.5);
            sp.setResizeWeight(0.5);
            center = new JPanel(new BorderLayout());
            center.setOpaque(false);
            center.add(sp, BorderLayout.CENTER);

        } else {
            // ── Vista USUARIO: solo lectura ──────────────────────────
            // Muestra el detalle del ticket y la respuesta del admin (si existe)
            // pero NO tiene campo para responder
            JLabel lblHint = new JLabel(
                "<html><i style='color:#888;font-size:11px'>"
                + "Solo el administrador puede responder tickets. "
                + "Verás la respuesta aquí cuando esté disponible.</i></html>");
            lblHint.setBorder(new EmptyBorder(4, 8, 4, 8));

            center = new JPanel(new BorderLayout(4,4));
            center.setOpaque(false);
            center.add(new JScrollPane(txtDetail), BorderLayout.CENTER);
            center.add(lblHint, BorderLayout.SOUTH);
        }

        p.add(lbl,    BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    // ─── Actualizar lista ────────────────────────────────────────────

    public void refresh() {
        currentTickets = user.isAdmin()
                ? ticketDao.findAll()
                : ticketDao.findByUser(user.getId());
        tblModel.setRowCount(0);
        for (SupportTicket t : currentTickets) {
            if (user.isAdmin()) {
                tblModel.addRow(new Object[]{
                    "#" + t.getId(), t.getUserName(),
                    t.getGreenhouseName() != null ? t.getGreenhouseName() : "—",
                    t.getSubject(), t.getStatusDisplay(), t.getPriorityDisplay(),
                    t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : "—"
                });
            } else {
                tblModel.addRow(new Object[]{
                    "#" + t.getId(), t.getSubject(),
                    t.getStatusDisplay(), t.getPriorityDisplay(),
                    t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : "—"
                });
            }
        }
        txtDetail.setText("Selecciona un ticket para ver el detalle.");
        if (txtResponse != null) txtResponse.setText("");
    }

    private void showDetail() {
        int row = tbl.getSelectedRow();
        if (row < 0 || currentTickets == null || row >= currentTickets.size()) return;
        SupportTicket t = currentTickets.get(row);

        String adminResp = (t.getAdminResponse() != null && !t.getAdminResponse().isBlank())
                ? t.getAdminResponse()
                : "(Sin respuesta del administrador aún)";

        String detail = "📌 Asunto: " + t.getSubject() + "\n"
                + "👤 Usuario: " + (t.getUserName() != null ? t.getUserName() : "—") + "\n"
                + "🏠 Invernadero: " + (t.getGreenhouseName() != null ? t.getGreenhouseName() : "General") + "\n"
                + "📅 Fecha: " + (t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : "—") + "\n\n"
                + "📝 Descripción:\n" + t.getDescription() + "\n\n"
                + "─────────────────────────────────\n"
                + "💬 Respuesta del administrador:\n" + adminResp;

        txtDetail.setText(detail);
        txtDetail.setCaretPosition(0);

        if (user.isAdmin() && cmbStatus != null) {
            cmbStatus.setSelectedIndex(t.getStatus().ordinal());
            cmbPriority.setSelectedIndex(t.getPriority().ordinal());
            if (txtResponse != null && t.getAdminResponse() != null)
                txtResponse.setText(t.getAdminResponse());
        }
    }

    /** Solo ADMIN puede enviar respuesta. */
    private void sendAdminResponse() {
        if (!user.isAdmin()) return;   // Doble protección

        int row = tbl.getSelectedRow();
        if (row < 0 || currentTickets == null || row >= currentTickets.size()) {
            JOptionPane.showMessageDialog(this, "Selecciona un ticket primero.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SupportTicket t = currentTickets.get(row);
        String resp = txtResponse.getText().trim();
        if (resp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe una respuesta primero.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        t.setAdminResponse(resp);
        t.setStatus(Status.values()[cmbStatus.getSelectedIndex()]);
        t.setPriority(Priority.values()[cmbPriority.getSelectedIndex()]);
        ticketDao.update(t);
        JOptionPane.showMessageDialog(this, "✅ Respuesta enviada al usuario.", "OK", JOptionPane.INFORMATION_MESSAGE);
        refresh();
    }

    /** Diálogo para crear un nuevo ticket (solo USUARIO). */
    private void openNewTicketDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nuevo Ticket de Soporte", true);
        dlg.setSize(520, 440);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));
        form.setBackground(Color.WHITE);

        // Título del diálogo
        JLabel dlgTitle = new JLabel("📨 Abrir Ticket de Soporte Técnico");
        dlgTitle.setFont(AppTheme.FONT_TITLE);
        dlgTitle.setForeground(AppTheme.PRIMARY_DARK);
        dlgTitle.setAlignmentX(LEFT_ALIGNMENT);
        form.add(dlgTitle);
        form.add(Box.createVerticalStrut(12));

        // Invernadero
        List<Greenhouse> greenhouses = greenhouseDao.findByUser(user.getId());
        String[] ghNames = greenhouses.stream().map(Greenhouse::toString).toArray(String[]::new);
        JComboBox<String> cmbGH = new JComboBox<>(ghNames.length > 0 ? ghNames : new String[]{"General"});

        // Prioridad
        JComboBox<String> cmbPri = new JComboBox<>(new String[]{"🔽 Baja", "▶️ Media", "🔼 Alta", "🚨 Crítica"});
        cmbPri.setSelectedIndex(1);

        // Asunto
        JTextField txtSubject = new JTextField();
        txtSubject.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        // Descripción
        JTextArea txtDesc = new JTextArea(5, 30);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);

        addFormRow(form, "Invernadero:", cmbGH);
        addFormRow(form, "Prioridad:", cmbPri);
        addFormRow(form, "Asunto del problema:", txtSubject);
        form.add(new JLabel("Descripción detallada:"));
        form.add(Box.createVerticalStrut(4));
        JScrollPane scrollDesc = new JScrollPane(txtDesc);
        scrollDesc.setAlignmentX(LEFT_ALIGNMENT);
        form.add(scrollDesc);
        form.add(Box.createVerticalStrut(8));

        // Info de soporte
        JLabel lblContacto = new JLabel(
            "<html><i style='color:#888'>📞 También puedes contactar: soporte@agropulse.co</i></html>");
        lblContacto.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblContacto);

        JButton btnSend = AppTheme.primaryButton("📨 Enviar Ticket al Administrador");
        btnSend.addActionListener(e -> {
            String subj = txtSubject.getText().trim();
            String desc = txtDesc.getText().trim();
            if (subj.isEmpty() || desc.isEmpty()) {
                JOptionPane.showMessageDialog(dlg,
                    "Completa el asunto y la descripción.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int ghId = greenhouses.isEmpty() ? 0 : greenhouses.get(cmbGH.getSelectedIndex()).getId();
            SupportTicket t = new SupportTicket(user.getId(), ghId, subj, desc);
            t.setPriority(Priority.values()[cmbPri.getSelectedIndex()]);
            ticketDao.save(t);
            dlg.dispose();
            refresh();
            JOptionPane.showMessageDialog(this,
                "✅ Ticket enviado al administrador.\nResponderán lo antes posible.",
                "Ticket Creado", JOptionPane.INFORMATION_MESSAGE);
        });

        dlg.add(new JScrollPane(form), BorderLayout.CENTER);
        dlg.add(btnSend, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void addFormRow(JPanel p, String label, JComponent field) {
        p.add(new JLabel(label));
        p.add(Box.createVerticalStrut(3));
        field.setAlignmentX(LEFT_ALIGNMENT);
        if (!(field instanceof JScrollPane)) field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.add(field);
        p.add(Box.createVerticalStrut(8));
    }
}
