package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {

    @Query("""
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Income i
        WHERE i.user = :user
    """)
    BigDecimal getTotalIncome(
            @Param("user") User user
    );

    @Query("""
        SELECT COALESCE(SUM(i.amount), 0)
        FROM Income i
        WHERE i.user = :user
        AND i.date >= :startDate
        AND i.date < :endDate
    """)
    BigDecimal getTotalIncomeBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT MONTH(i.date), COALESCE(SUM(i.amount), 0)
        FROM Income i
        WHERE i.user = :user
          AND i.date >= :startDate
          AND i.date < :endDate
        GROUP BY MONTH(i.date)
        ORDER BY MONTH(i.date)
    """)
    List<Object[]> getMonthlyTotals(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    List<Income> findTop5ByUserOrderByDateDesc(User user);

    List<Income> findAllByUser(User user);

    boolean existsByUserAndCategoryIgnoreCase(User user, String category);

    boolean existsByUserAndDateAndConceptIgnoreCaseAndAmount(
            User user, LocalDate date, String concept, BigDecimal amount);

    Income findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    void deleteAllByUser(User user);
}
