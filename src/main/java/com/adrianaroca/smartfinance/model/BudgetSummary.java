package com.adrianaroca.smartfinance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BudgetSummary {

    private final Budget budget;
    private final BigDecimal spent;
    private final double percentage;
    private final BigDecimal remaining;
    private final BigDecimal warningAmount;

    public BudgetSummary(Budget budget, BigDecimal spent) {

        this.budget = budget;
        this.spent = spent;
        this.remaining = budget.getAmount().subtract(spent).max(BigDecimal.ZERO);
        this.warningAmount = budget.getAmount()
                .multiply(BigDecimal.valueOf(budget.getWarningPercentage()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING);

        this.percentage = budget.getAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent
                .divide(budget.getAmount(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0;
    }

    public Budget getBudget() {
        return budget;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public double getPercentage() {
        return percentage;
    }

    public BigDecimal getRemaining() { return remaining; }

    public BigDecimal getWarningAmount() { return warningAmount; }

    public boolean isWarningReached() { return spent.compareTo(warningAmount) >= 0; }

    public boolean isExceeded() { return spent.compareTo(budget.getAmount()) >= 0; }
}
