package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.dao.GreenhouseDao;
import com.agropulse.model.Greenhouse;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel selector de invernadero reutilizable.
 * Se coloca en el header de la MainFrame para que el usuario
 * (o el admin) elija qué invernadero está viendo en cada momento.
 *
 * Al cambiar la selección:
 *  1. Se actualiza controller.setSelectedGreenhouse(id, name)
 *  2. Se llama al listener onChangeCallback para que MainFrame
 *     refresque el panel activo.
 */
public class GreenhouseSelectorPanel extends JPanel {

    public interface OnGreenhouseChange {
        void onChange(int greenhouseId, String name);
    }

    private final GreenhouseController  controller;
    private final User                  user;
    private final GreenhouseDao         ghDao;
    private final JComboBox<String>     combo;
    private final List<Greenhouse>      greenhouses = new ArrayList<>();
    private       OnGreenhouseChange    callback;

    public GreenhouseSelectorPanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        this.ghDao      = new GreenhouseDao();

        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setOpaque(false);

        JLabel lbl = new JLabel("🏠 Invernadero:");
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        lbl.setForeground(Color.WHITE);

        combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setPreferredSize(new Dimension(200, 28));
        combo.setBackground(new Color(0x1B5E20));
        combo.setForeground(Color.WHITE);

        add(lbl);
        add(combo);

        loadGreenhouses();

        combo.addActionListener(e -> {
            int idx = combo.getSelectedIndex();
            if (idx < 0 || idx >= greenhouses.size()) return;
            Greenhouse gh = greenhouses.get(idx);
            controller.setSelectedGreenhouse(gh.getId(), gh.getName());
            if (callback != null) callback.onChange(gh.getId(), gh.getName());
        });
    }

    public void setOnChangeCallback(OnGreenhouseChange cb) {
        this.callback = cb;
    }

    /** Recarga la lista de invernaderos disponibles para este usuario. */
    public void loadGreenhouses() {
        greenhouses.clear();
        combo.removeAllItems();

        List<Greenhouse> available;
        if (user.isAdmin()) {
            // El admin ve todos los invernaderos que creó
            available = ghDao.findByOwner(user.getId());
            if (available.isEmpty()) available = ghDao.findAll();
        } else {
            // El usuario solo ve los que tiene asignados
            available = ghDao.findByUser(user.getId());
        }

        if (available.isEmpty()) {
            // Fallback: invernadero principal
            Greenhouse def = new Greenhouse("Invernadero Principal", "", "", 1);
            def.setId(1);
            available.add(def);
        }

        for (Greenhouse gh : available) {
            greenhouses.add(gh);
            combo.addItem(gh.getName());
        }

        // Seleccionar el primero por defecto
        if (!greenhouses.isEmpty()) {
            Greenhouse first = greenhouses.get(0);
            combo.setSelectedIndex(0);
            controller.setSelectedGreenhouse(first.getId(), first.getName());
        }
    }

    /** Devuelve el invernadero actualmente seleccionado. */
    public Greenhouse getSelected() {
        int idx = combo.getSelectedIndex();
        if (idx >= 0 && idx < greenhouses.size()) return greenhouses.get(idx);
        return null;
    }
}
