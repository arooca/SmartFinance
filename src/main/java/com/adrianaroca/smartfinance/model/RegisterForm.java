package com.adrianaroca.smartfinance.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterForm {

    @NotBlank(
            message = "El nombre es obligatorio"
    )
    @Size(
            max = 100,
            message = "El nombre no puede superar los 100 caracteres"
    )
    private String name;

    @NotBlank(
            message = "El email es obligatorio"
    )
    @Email(
            message = "Introduce un email válido"
    )
    @Size(
            max = 254,
            message = "El email no puede superar los 254 caracteres"
    )
    private String email;

    @NotBlank(
            message = "La contraseña es obligatoria"
    )
    @Size(
            min = 8,
            max = 72,
            message = "La contraseña debe tener entre 8 y 72 caracteres"
    )
    private String password;

    public RegisterForm() {
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
}