package com.adrianaroca.smartfinance.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileForm {

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

    @Size(
            max = 72,
            message = "La contraseña no puede superar los 72 caracteres"
    )
    private String currentPassword;

    @Size(
            max = 72,
            message = "La contraseña no puede superar los 72 caracteres"
    )
    private String newPassword;

    @Size(
            max = 72,
            message = "La contraseña no puede superar los 72 caracteres"
    )
    private String confirmPassword;

    @Size(
            max = 72,
            message = "La contraseña no puede superar los 72 caracteres"
    )
    private String deletePassword;

    public ProfileForm() {
    }

    public ProfileForm(String name, String email) {
        this.name = name;
        this.email = email;
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

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getDeletePassword() {
        return deletePassword;
    }

    public void setDeletePassword(String deletePassword) {
        this.deletePassword = deletePassword;
    }
}