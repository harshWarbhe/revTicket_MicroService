package com.revticket.dashboard.entity;

import jakarta.persistence.*;

/**
 * User entity - Maps to the shared users table in MySQL (revticket_db)
 * This matches the monolithic backend's User entity for SecurityUtil access.
 */
@Entity
@Table(name = "users")
public class User {

    public enum Role {
        USER,
        ADMIN
    }

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String password;

    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
