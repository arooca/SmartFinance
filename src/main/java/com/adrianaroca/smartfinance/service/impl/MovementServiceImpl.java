package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.model.DashboardMovement;
import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.GoalContribution;
import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.GoalContributionRepository;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.MovementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class MovementServiceImpl implements MovementService {

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final GoalContributionRepository contributionRepository;
    private final CurrentUserProvider currentUserProvider;

    public MovementServiceImpl(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            GoalContributionRepository contributionRepository,
            CurrentUserProvider currentUserProvider) {

        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.contributionRepository = contributionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<DashboardMovement> getAllMovements() {

        User user =
                currentUserProvider.getCurrentUser();

        List<DashboardMovement> movements =
                new ArrayList<>();

        List<Income> incomes =
                incomeRepository.findAllByUser(user);

        for (Income income : incomes) {

            movements.add(
                    new DashboardMovement(
                            income.getId(),
                            income.getConcept(),
                            income.getCategory(),
                            income.getDate(),
                            income.getAmount(),
                            "INCOME"
                    )
            );
        }

        List<Expense> expenses =
                expenseRepository.findAllByUser(user);

        for (Expense expense : expenses) {

            movements.add(
                    new DashboardMovement(
                            expense.getId(),
                            expense.getConcept(),
                            expense.getCategory(),
                            expense.getDate(),
                            expense.getAmount(),
                            "EXPENSE"
                    )
            );
        }

        for (GoalContribution contribution : contributionRepository
                .findAllByUserOrderByContributedAtDesc(user)) {
            movements.add(new DashboardMovement(
                    contribution.getId(),
                    "Aportación a meta: " + contribution.getGoal().getName(),
                    "Ahorro apartado",
                    contribution.getContributedAt(),
                    contribution.getAmount(),
                    "SAVING"
            ));
        }

        sortMovementsByDate(movements);

        return movements;
    }

    @Override
    public List<DashboardMovement> getMovementsByType(
            String type) {

        List<DashboardMovement> movements =
                getAllMovements();

        if (type == null
                || type.equalsIgnoreCase("ALL")) {

            return movements;
        }

        movements.removeIf(movement ->
                !movement.getType()
                        .equalsIgnoreCase(type)
        );

        return movements;
    }

    @Override
    public Page<DashboardMovement> getMovements(
            String type,
            String search,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        List<DashboardMovement> movements = getFilteredMovements(
                type, search, category, startDate, endDate);

        int totalElements = movements.size();

        int start = (int) Math.min(pageable.getOffset(), totalElements);

        int end = Math.min(start + pageable.getPageSize(), totalElements);

        List<DashboardMovement> pageContent = movements.subList(start, end);

        return new PageImpl<>(pageContent, pageable, totalElements);
    }

    @Override
    public List<DashboardMovement> getFilteredMovements(
            String type,
            String search,
            String category,
            LocalDate startDate,
            LocalDate endDate) {

        List<DashboardMovement> movements = getAllMovements();

        if (type != null
                && !type.equalsIgnoreCase("ALL")) {

            movements.removeIf(movement ->
                    !movement.getType()
                            .equalsIgnoreCase(type)
            );
        }

        if (search != null
                && !search.trim().isEmpty()) {

            String searchText =
                    normalizeText(search);

            movements.removeIf(movement ->
                    movement.getConcept() == null
                            || !normalizeText(
                            movement.getConcept()
                    ).contains(searchText)
            );
        }

        if (category != null
                && !category.equalsIgnoreCase("ALL")) {

            movements.removeIf(movement ->
                    movement.getCategory() == null
                            || !movement.getCategory()
                            .equalsIgnoreCase(category)
            );
        }

        if (startDate != null) {

            movements.removeIf(movement ->
                    movement.getDate() == null
                            || movement.getDate().isBefore(startDate)
            );
        }

        if (endDate != null) {

            movements.removeIf(movement ->
                    movement.getDate() == null
                            || movement.getDate().isAfter(endDate)
            );
        }

        return movements;
    }

    @Override
    public Map<String, BigDecimal>
    getMonthlyIncomeData() {

        User user =
                currentUserProvider.getCurrentUser();

        LocalDate startDate =
                YearMonth.now()
                        .minusMonths(5)
                        .atDay(1);

        LocalDate endDate =
                YearMonth.now()
                        .plusMonths(1)
                        .atDay(1);

        List<Object[]> results =
                incomeRepository.getMonthlyTotals(
                        user,
                        startDate,
                        endDate
                );

        return buildMonthlyData(results);
    }

    @Override
    public Map<String, BigDecimal>
    getMonthlyExpenseData() {

        User user =
                currentUserProvider.getCurrentUser();

        LocalDate startDate =
                YearMonth.now()
                        .minusMonths(5)
                        .atDay(1);

        LocalDate endDate =
                YearMonth.now()
                        .plusMonths(1)
                        .atDay(1);

        List<Object[]> results =
                expenseRepository.getMonthlyTotals(
                        user,
                        startDate,
                        endDate
                );

        return buildMonthlyData(results);
    }

    @Override
    public Map<String, BigDecimal>
    getExpenseTotalsByCategory(
            LocalDate startDate,
            LocalDate endDate) {

        User user =
                currentUserProvider.getCurrentUser();

        Map<String, BigDecimal> totals =
                new LinkedHashMap<>();

        List<Object[]> results =
                expenseRepository
                        .getTotalsByCategoryBetween(
                                user,
                                startDate,
                                endDate
                        );

        for (Object[] result : results) {

            String category =
                    result[0] != null
                            ? result[0].toString()
                            : "Sin categoría";

            BigDecimal amount =
                    result[1] != null
                            ? (BigDecimal) result[1]
                            : BigDecimal.ZERO;

            totals.put(category, amount);
        }

        return totals;
    }

    private Map<String, BigDecimal> buildMonthlyData(
            List<Object[]> results) {

        Map<Integer, BigDecimal> totalsByMonth =
                new LinkedHashMap<>();

        for (Object[] result : results) {

            Integer month =
                    ((Number) result[0]).intValue();

            BigDecimal amount =
                    result[1] != null
                            ? (BigDecimal) result[1]
                            : BigDecimal.ZERO;

            totalsByMonth.put(month, amount);
        }

        Map<String, BigDecimal> monthlyData =
                new LinkedHashMap<>();

        YearMonth currentMonth =
                YearMonth.now();

        for (int i = 5; i >= 0; i--) {

            YearMonth month =
                    currentMonth.minusMonths(i);

            String monthName =
                    month.getMonth()
                            .getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.of("es", "ES")
                            );

            monthName =
                    monthName.substring(0, 1)
                            .toUpperCase()
                            + monthName.substring(1);

            monthlyData.put(
                    monthName,
                    totalsByMonth.getOrDefault(
                            month.getMonthValue(),
                            BigDecimal.ZERO
                    )
            );
        }

        return monthlyData;
    }

    private String normalizeText(String text) {

        return Normalizer.normalize(
                        text,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }

    private void sortMovementsByDate(
            List<DashboardMovement> movements) {

        movements.sort((movement1, movement2) -> {

            LocalDate date1 =
                    movement1.getDate();

            LocalDate date2 =
                    movement2.getDate();

            if (date1 == null
                    && date2 == null) {

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
    }
}
