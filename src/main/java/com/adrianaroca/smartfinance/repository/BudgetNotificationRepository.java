package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.Budget;
import com.adrianaroca.smartfinance.model.BudgetNotification;
import com.adrianaroca.smartfinance.model.BudgetNotificationType;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BudgetNotificationRepository extends JpaRepository<BudgetNotification, Long> {

    boolean existsByBudgetAndBudgetMonthAndCategoryAndType(
            Budget budget, LocalDate budgetMonth, String category, BudgetNotificationType type);

    List<BudgetNotification> findAllByUserAndReadAtIsNullOrderByCreatedAtDesc(User user);

    long countByUserAndReadAtIsNull(User user);
}
