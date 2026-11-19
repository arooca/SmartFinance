package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Budget;
import com.adrianaroca.smartfinance.model.BudgetSummary;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface BudgetService {

    List<BudgetSummary> getBudgetsWithProgress(
            YearMonth month
    );

    Budget findById(Long id);

    boolean existsForMonthAndCategory(
            String category,
            LocalDate month
    );

    boolean existsForMonthAndCategoryExcludingId(
            String category,
            LocalDate month,
            Long id
    );

    Budget save(Budget budget);

    void deleteById(Long id);
}