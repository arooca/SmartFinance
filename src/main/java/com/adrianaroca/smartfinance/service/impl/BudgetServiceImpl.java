package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.exception.ResourceNotFoundException;
import com.adrianaroca.smartfinance.model.Budget;
import com.adrianaroca.smartfinance.model.BudgetSummary;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.BudgetRepository;
import com.adrianaroca.smartfinance.service.BudgetService;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.MovementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class BudgetServiceImpl implements BudgetService {

    private final BudgetRepository budgetRepository;
    private final MovementService movementService;
    private final CurrentUserProvider currentUserProvider;

    public BudgetServiceImpl(
            BudgetRepository budgetRepository,
            MovementService movementService,
            CurrentUserProvider currentUserProvider) {

        this.budgetRepository = budgetRepository;
        this.movementService = movementService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<BudgetSummary> getBudgetsWithProgress(
            YearMonth selectedMonth) {

        User user =
                currentUserProvider.getCurrentUser();

        LocalDate month =
                selectedMonth.atDay(1);

        List<Budget> budgets =
                budgetRepository
                        .findAllByUserAndMonthOrderByCategoryAsc(
                                user,
                                month
                        );

        LocalDate start =
                selectedMonth.atDay(1);

        LocalDate end =
                selectedMonth.plusMonths(1)
                        .atDay(1);

        Map<String, BigDecimal> expensesByCategory =
                movementService.getExpenseTotalsByCategory(
                        start,
                        end
                );

        return budgets.stream()
                .map(budget ->
                        new BudgetSummary(
                                budget,
                                expensesByCategory.getOrDefault(
                                        budget.getCategory(),
                                        BigDecimal.ZERO
                                )
                        )
                )
                .toList();
    }

    @Override
    public Budget findById(Long id) {

        Budget budget =
                budgetRepository.findByIdAndUser(
                        id,
                        currentUserProvider.getCurrentUser()
                );

        if (budget == null) {

            throw new ResourceNotFoundException(
                    "Presupuesto no encontrado"
            );
        }

        return budget;
    }

    @Override
    public boolean existsForMonthAndCategory(
            String category,
            LocalDate month) {

        return budgetRepository
                .existsByUserAndCategoryIgnoreCaseAndMonth(
                        currentUserProvider.getCurrentUser(),
                        category,
                        month
                );
    }

    @Override
    public boolean existsForMonthAndCategoryExcludingId(
            String category,
            LocalDate month,
            Long id) {

        Budget existing =
                budgetRepository.findByIdAndUser(
                        id,
                        currentUserProvider.getCurrentUser()
                );

        if (existing == null) {
            return false;
        }

        if (!existing.getCategory()
                .equalsIgnoreCase(category)
                || !existing.getMonth()
                .equals(month)) {

            return budgetRepository
                    .existsByUserAndCategoryIgnoreCaseAndMonth(
                            currentUserProvider.getCurrentUser(),
                            category,
                            month
                    );
        }

        return false;
    }

    @Override
    public Budget save(Budget budget) {

        User user =
                currentUserProvider.getCurrentUser();

        if (budget.getId() == null) {

            budget.setUser(user);

        } else {

            Budget existing =
                    budgetRepository.findByIdAndUser(
                            budget.getId(),
                            user
                    );

            if (existing == null) {

                throw new ResourceNotFoundException(
                        "Presupuesto no encontrado"
                );
            }

            budget.setUser(user);
        }

        budget.setMonth(
                budget.getMonth()
                        .withDayOfMonth(1)
        );

        return budgetRepository.save(budget);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {

        User user =
                currentUserProvider.getCurrentUser();

        Budget budget =
                budgetRepository.findByIdAndUser(
                        id,
                        user
                );

        if (budget == null) {

            throw new ResourceNotFoundException(
                    "Presupuesto no encontrado"
            );
        }

        budgetRepository.deleteByIdAndUser(
                id,
                user
        );
    }
}