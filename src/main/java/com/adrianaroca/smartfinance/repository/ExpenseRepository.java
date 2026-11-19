package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.user = :user
    """)
    BigDecimal getTotalExpense(
            @Param("user") User user
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.user = :user
        AND e.date >= :startDate
        AND e.date < :endDate
    """)
    BigDecimal getTotalExpenseBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT MONTH(e.date), COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.user = :user
          AND e.date >= :startDate
          AND e.date < :endDate
        GROUP BY MONTH(e.date)
        ORDER BY MONTH(e.date)
    """)
    List<Object[]> getMonthlyTotals(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT e.category, COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.user = :user
          AND e.date >= :startDate
          AND e.date < :endDate
        GROUP BY e.category
    """)
    List<Object[]> getTotalsByCategoryBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Expense> findTop5ByUserOrderByDateDesc(User user);

    List<Expense> findAllByUser(User user);

    boolean existsByUserAndCategoryIgnoreCase(User user, String category);

    boolean existsByUserAndDateAndConceptIgnoreCaseAndAmount(
            User user, LocalDate date, String concept, BigDecimal amount);

    Expense findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    void deleteAllByUser(User user);
}
