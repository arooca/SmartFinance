package com.adrianaroca.smartfinance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "incomes")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El concepto es obligatorio")
    @Size(
            max = 100,
            message = "El concepto no puede superar los 100 caracteres"
    )
    private String concept;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor que 0")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "La cantidad debe tener como máximo 10 enteros y 2 decimales"
    )
    private BigDecimal amount;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(
            max = 100,
            message = "La categoría no puede superar los 100 caracteres"
    )
    private String category;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @Size(
            max = 500,
            message = "La descripción no puede superar los 500 caracteres"
    )
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Income() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}