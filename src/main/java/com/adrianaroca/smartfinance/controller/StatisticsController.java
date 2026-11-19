package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.service.ExpenseService;
import com.adrianaroca.smartfinance.service.IncomeService;
import com.adrianaroca.smartfinance.service.MovementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@Controller
public class StatisticsController {

    private final MovementService movementService;
    private final IncomeService incomeService;
    private final ExpenseService expenseService;

    public StatisticsController(
            MovementService movementService,
            IncomeService incomeService,
            ExpenseService expenseService) {

        this.movementService = movementService;
        this.incomeService = incomeService;
        this.expenseService = expenseService;
    }

    @GetMapping("/statistics")
    public String statistics(Model model) {

        YearMonth currentMonth =
                YearMonth.now();

        YearMonth previousMonth =
                currentMonth.minusMonths(1);

        LocalDate currentStart =
                currentMonth.atDay(1);

        LocalDate currentEnd =
                currentMonth.plusMonths(1)
                        .atDay(1);

        LocalDate previousStart =
                previousMonth.atDay(1);

        LocalDate previousEnd =
                currentMonth.atDay(1);

        BigDecimal currentIncome =
                incomeService.getTotalIncomeBetween(
                        currentStart,
                        currentEnd
                );

        BigDecimal currentExpense =
                expenseService.getTotalExpenseBetween(
                        currentStart,
                        currentEnd
                );

        BigDecimal previousIncome =
                incomeService.getTotalIncomeBetween(
                        previousStart,
                        previousEnd
                );

        BigDecimal previousExpense =
                expenseService.getTotalExpenseBetween(
                        previousStart,
                        previousEnd
                );

        BigDecimal currentBalance =
                currentIncome.subtract(currentExpense);

        BigDecimal previousBalance =
                previousIncome.subtract(previousExpense);

        double savingPercentage =
                currentIncome.compareTo(BigDecimal.ZERO) > 0
                        ? currentBalance
                        .divide(
                                currentIncome,
                                10,
                                java.math.RoundingMode.HALF_UP
                        )
                        .doubleValue() * 100
                        : 0;

        BigDecimal incomeDifference =
                currentIncome.subtract(previousIncome);

        BigDecimal expenseDifference =
                currentExpense.subtract(previousExpense);

        BigDecimal balanceDifference =
                currentBalance.subtract(previousBalance);

        Map<String, BigDecimal> expensesByCategory =
                movementService.getExpenseTotalsByCategory(
                        currentStart,
                        currentEnd
                );

        Map<String, BigDecimal> monthlyIncomeData =
                movementService.getMonthlyIncomeData();

        Map<String, BigDecimal> monthlyExpenseData =
                movementService.getMonthlyExpenseData();

        model.addAttribute(
                "currentMonth",
                currentMonth
        );

        model.addAttribute(
                "currentIncome",
                currentIncome
        );

        model.addAttribute(
                "currentExpense",
                currentExpense
        );

        model.addAttribute(
                "currentBalance",
                currentBalance
        );

        model.addAttribute(
                "savingPercentage",
                savingPercentage
        );

        model.addAttribute(
                "previousIncome",
                previousIncome
        );

        model.addAttribute(
                "previousExpense",
                previousExpense
        );

        model.addAttribute(
                "previousBalance",
                previousBalance
        );

        model.addAttribute(
                "incomeDifference",
                incomeDifference
        );

        model.addAttribute(
                "expenseDifference",
                expenseDifference
        );

        model.addAttribute(
                "balanceDifference",
                balanceDifference
        );

        model.addAttribute(
                "expensesByCategory",
                expensesByCategory
        );

        model.addAttribute(
                "monthlyLabels",
                monthlyIncomeData.keySet()
        );

        model.addAttribute(
                "monthlyIncome",
                monthlyIncomeData.values()
        );

        model.addAttribute(
                "monthlyExpense",
                monthlyExpenseData.values()
        );

        return "statistics";
    }
}