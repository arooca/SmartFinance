package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.DashboardMovement;
import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.service.ExpenseService;
import com.adrianaroca.smartfinance.service.IncomeService;
import com.adrianaroca.smartfinance.service.MovementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final IncomeService incomeService;
    private final ExpenseService expenseService;
    private final MovementService movementService;

    public DashboardController(
            IncomeService incomeService,
            ExpenseService expenseService,
            MovementService movementService) {

        this.incomeService = incomeService;
        this.expenseService = expenseService;
        this.movementService = movementService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        YearMonth currentMonth = YearMonth.now();

        LocalDate currentStart = currentMonth.atDay(1);
        LocalDate currentEnd = currentMonth.plusMonths(1).atDay(1);

        BigDecimal totalIncome =
                incomeService.getTotalIncomeBetween(
                        currentStart,
                        currentEnd
                );

        BigDecimal totalExpense =
                expenseService.getTotalExpenseBetween(
                        currentStart,
                        currentEnd
                );

        BigDecimal balance =
                totalIncome.subtract(totalExpense);

        double savingPercentage = 0;

        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {

            savingPercentage =
                    balance
                            .divide(
                                    totalIncome,
                                    6,
                                    java.math.RoundingMode.HALF_UP
                            )
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
        }

        YearMonth previousMonth =
                currentMonth.minusMonths(1);

        LocalDate previousStart =
                previousMonth.atDay(1);

        LocalDate previousEnd =
                currentMonth.atDay(1);

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

        BigDecimal previousBalance =
                previousIncome.subtract(previousExpense);

        double previousSavingPercentage = 0;

        if (previousIncome.compareTo(BigDecimal.ZERO) > 0) {

            previousSavingPercentage =
                    previousBalance
                            .divide(
                                    previousIncome,
                                    6,
                                    java.math.RoundingMode.HALF_UP
                            )
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();
        }

        List<Income> latestIncomes =
                incomeService.getLatestIncomes();

        List<Expense> latestExpenses =
                expenseService.getLatestExpenses();

        List<DashboardMovement> latestMovements =
                new ArrayList<>();

        for (Income income : latestIncomes) {

            DashboardMovement movement =
                    new DashboardMovement(
                            income.getId(),
                            income.getConcept(),
                            income.getCategory(),
                            income.getDate(),
                            income.getAmount(),
                            "INCOME"
                    );

            latestMovements.add(movement);
        }

        for (Expense expense : latestExpenses) {

            DashboardMovement movement =
                    new DashboardMovement(
                            expense.getId(),
                            expense.getConcept(),
                            expense.getCategory(),
                            expense.getDate(),
                            expense.getAmount(),
                            "EXPENSE"
                    );

            latestMovements.add(movement);
        }

        latestMovements.sort((m1, m2) -> {

            LocalDate date1 = m1.getDate();
            LocalDate date2 = m2.getDate();

            if (date1 == null && date2 == null) {
                return 0;
            }

            if (date1 == null) {
                return 1;
            }

            if (date2 == null) {
                return -1;
            }

            return date2.compareTo(date1);
        });

        if (latestMovements.size() > 5) {

            latestMovements =
                    latestMovements.subList(0, 5);
        }

        Map<String, BigDecimal> monthlyIncomeData =
                movementService.getMonthlyIncomeData();

        Map<String, BigDecimal> monthlyExpenseData =
                movementService.getMonthlyExpenseData();

        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("balance", balance);
        model.addAttribute("savingPercentage", savingPercentage);

        model.addAttribute(
                "latestMovements",
                latestMovements
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

        return "dashboard";
    }
}