package com.adrianaroca.smartfinance.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(
            max = 100,
            message = "El nombre no puede superar los 100 caracteres"
    )
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Introduce un email válido")
    @Size(
            max = 254,
            message = "El email no puede superar los 254 caracteres"
    )
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(
            min = 8,
            max = 72,
            message = "La contraseña debe tener entre 8 y 72 caracteres"
    )
    private String password;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    public User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEmailVerified() { return emailVerified; }

    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public long getCredentialVersion() { return credentialVersion; }

    public void incrementCredentialVersion() { credentialVersion++; }
}
