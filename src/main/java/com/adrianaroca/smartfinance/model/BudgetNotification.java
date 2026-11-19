package com.adrianaroca.smartfinance.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_notifications")
public class BudgetNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "budget_id")
    private Budget budget;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "budget_month", nullable = false)
    private LocalDate budgetMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetNotificationType type;

    @Column(name = "warning_percentage", nullable = false)
    private Integer warningPercentage;

    @Column(name = "spent_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal spentAmount;

    @Column(name = "budget_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getBudgetMonth() { return budgetMonth; }
    public void setBudgetMonth(LocalDate budgetMonth) { this.budgetMonth = budgetMonth; }
    public BudgetNotificationType getType() { return type; }
    public void setType(BudgetNotificationType type) { this.type = type; }
    public Integer getWarningPercentage() { return warningPercentage; }
    public void setWarningPercentage(Integer warningPercentage) { this.warningPercentage = warningPercentage; }
    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
