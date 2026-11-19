package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.model.*;
import com.adrianaroca.smartfinance.repository.BudgetNotificationRepository;
import com.adrianaroca.smartfinance.repository.BudgetRepository;
import com.adrianaroca.smartfinance.service.BudgetAlertService;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.MovementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class BudgetAlertServiceImpl implements BudgetAlertService {

    private final BudgetRepository budgetRepository;
    private final BudgetNotificationRepository notificationRepository;
    private final MovementService movementService;
    private final CurrentUserProvider currentUserProvider;

    public BudgetAlertServiceImpl(BudgetRepository budgetRepository,
                                  BudgetNotificationRepository notificationRepository,
                                  MovementService movementService,
                                  CurrentUserProvider currentUserProvider) {
        this.budgetRepository = budgetRepository;
        this.notificationRepository = notificationRepository;
        this.movementService = movementService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public long refreshAndCountUnread() {
        refreshCurrentMonth();
        return notificationRepository.countByUserAndReadAtIsNull(currentUserProvider.getCurrentUser());
    }

    @Override
    @Transactional
    public List<BudgetNotification> getAndMarkUnread() {
        refreshCurrentMonth();
        List<BudgetNotification> notifications = notificationRepository
                .findAllByUserAndReadAtIsNullOrderByCreatedAtDesc(currentUserProvider.getCurrentUser());
        LocalDateTime readAt = LocalDateTime.now();
        notifications.forEach(notification -> notification.setReadAt(readAt));
        notificationRepository.saveAll(notifications);
        return notifications;
    }

    private void refreshCurrentMonth() {
        User user = currentUserProvider.getCurrentUser();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        List<Budget> budgets = budgetRepository.findAllByUserAndMonthOrderByCategoryAsc(user, monthStart);
        if (budgets.isEmpty()) return;
        Map<String, BigDecimal> spentByCategory = movementService.getExpenseTotalsByCategory(
                monthStart, currentMonth.plusMonths(1).atDay(1));

        for (Budget budget : budgets) {
            BigDecimal spent = spentByCategory.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
            int warningPercentage = budget.getWarningPercentage() == null ? 80 : budget.getWarningPercentage();
            BigDecimal warningAmount = budget.getAmount()
                    .multiply(BigDecimal.valueOf(warningPercentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING);

            if (spent.compareTo(warningAmount) >= 0) {
                createIfMissing(budget, user, spent, BudgetNotificationType.WARNING, warningPercentage);
            }
            if (spent.compareTo(budget.getAmount()) >= 0) {
                createIfMissing(budget, user, spent, BudgetNotificationType.EXCEEDED, warningPercentage);
            }
        }
    }

    private void createIfMissing(Budget budget, User user, BigDecimal spent,
                                 BudgetNotificationType type, int warningPercentage) {
        if (notificationRepository.existsByBudgetAndBudgetMonthAndCategoryAndType(
                budget, budget.getMonth(), budget.getCategory(), type)) {
            return;
        }
        BudgetNotification notification = new BudgetNotification();
        notification.setUser(user);
        notification.setBudget(budget);
        notification.setCategory(budget.getCategory());
        notification.setBudgetMonth(budget.getMonth());
        notification.setType(type);
        notification.setWarningPercentage(warningPercentage);
        notification.setSpentAmount(spent);
        notification.setBudgetAmount(budget.getAmount());
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
