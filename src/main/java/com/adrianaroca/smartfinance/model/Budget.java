package com.adrianaroca.smartfinance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "budgets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_budgets_user_category_month",
                        columnNames = {
                                "user_id",
                                "category",
                                "month"
                        }
                )
        }
)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La categoría es obligatoria")
    @Size(
            max = 100,
            message = "La categoría no puede superar los 100 caracteres"
    )
    private String category;

    @NotNull(message = "El límite es obligatorio")
    @Positive(message = "El límite debe ser mayor que 0")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "El límite debe tener como máximo 10 enteros y 2 decimales"
    )
    private BigDecimal amount;

    @NotNull(message = "El mes es obligatorio")
    private LocalDate month;

    @NotNull(message = "El porcentaje de aviso es obligatorio")
    @Min(value = 50, message = "El aviso debe estar entre el 50 % y el 99 %")
    @Max(value = 99, message = "El aviso debe estar entre el 50 % y el 99 %")
    @Column(name = "warning_percentage", nullable = false)
    private Integer warningPercentage = 80;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Budget() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getMonth() {
        return month;
    }

    public void setMonth(LocalDate month) {
        this.month = month;
    }

    public Integer getWarningPercentage() { return warningPercentage; }

    public void setWarningPercentage(Integer warningPercentage) { this.warningPercentage = warningPercentage; }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
