package com.adrianaroca.smartfinance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_movements")
public class RecurringMovement {

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

    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(
            regexp = "INCOME|EXPENSE",
            message = "El tipo de movimiento no es válido"
    )
    private String type;

    @NotNull(message = "El día es obligatorio")
    @Min(
            value = 1,
            message = "El día debe estar entre 1 y 31"
    )
    @Max(
            value = 31,
            message = "El día debe estar entre 1 y 31"
    )
    private Integer dayOfMonth;

    private boolean active = true;

    private LocalDate nextExecutionDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public RecurringMovement() {
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(LocalDate nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}