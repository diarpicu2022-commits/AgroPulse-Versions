package com.agropulse.model;

import com.agropulse.model.enums.UserRole;
import java.time.LocalDateTime;

/**
 * Modelo de usuario del sistema.
 * Soporta Admin y Usuario con diferentes permisos.
 */
public class User {
    private int id;
    private String username;
    private String password;
    private String fullName;
    private String phone;          // Número WhatsApp para alertas
    private UserRole role;
    private boolean active;
    private LocalDateTime createdAt;
    private int    assignedGreenhouseId   = 0;  // 0 = todos (admin)
    private String assignedGreenhouseName = "";
    private String email = "";       // Email (para login Google)
    private String avatar = "";       // URL Avatar (para login Google)

    // --- Constructores ---

    public User() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String password, String fullName,
                String phone, UserRole role) {
        this();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int    getAssignedGreenhouseId()          { return assignedGreenhouseId; }
    public void   setAssignedGreenhouseId(int v)     { this.assignedGreenhouseId = v; }
    public String getAssignedGreenhouseName()         { return assignedGreenhouseName; }
    public void   setAssignedGreenhouseName(String v) { this.assignedGreenhouseName = v; }

    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String v) { this.avatar = v; }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    @Override
    public String toString() {
        return "Usuario: " + fullName +
               " | Rol: " + role.getDisplayName() +
               " | Teléfono: " + phone;
    }
}
