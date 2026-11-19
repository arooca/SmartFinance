package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.Budget;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findAllByUserAndMonthOrderByCategoryAsc(
            User user,
            LocalDate month
    );

    Budget findByIdAndUser(
            Long id,
            User user
    );

    void deleteByIdAndUser(
            Long id,
            User user
    );

    void deleteAllByUser(
            User user
    );

    boolean existsByUserAndCategoryIgnoreCaseAndMonth(
            User user,
            String category,
            LocalDate month
    );

    boolean existsByUserAndCategoryIgnoreCase(User user, String category);
}
