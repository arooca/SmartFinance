package com.adrianaroca.smartfinance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DashboardMovement {

    private Long id;
    private String concept;
    private String category;
    private LocalDate date;
    private BigDecimal amount;
    private String type;

    public DashboardMovement() {
    }

    public DashboardMovement(
            Long id,
            String concept,
            String category,
            LocalDate date,
            BigDecimal amount,
            String type) {

        this.id = id;
        this.concept = concept;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.type = type;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}