package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.DashboardMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MovementService {

    List<DashboardMovement> getAllMovements();

    List<DashboardMovement> getMovementsByType(String type);

    Page<DashboardMovement> getMovements(
            String type,
            String search,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    List<DashboardMovement> getFilteredMovements(
            String type,
            String search,
            String category,
            LocalDate startDate,
            LocalDate endDate
    );

    Map<String, BigDecimal> getMonthlyIncomeData();

    Map<String, BigDecimal> getMonthlyExpenseData();

    Map<String, BigDecimal> getExpenseTotalsByCategory(
            LocalDate startDate,
            LocalDate endDate
    );
}
