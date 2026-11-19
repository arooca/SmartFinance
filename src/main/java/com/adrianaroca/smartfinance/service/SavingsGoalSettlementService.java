package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.SavingsGoal;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.GoalContributionRepository;
import com.adrianaroca.smartfinance.repository.SavingsGoalRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SavingsGoalSettlementService {

    private static final ZoneId APP_ZONE = ZoneId.of("Europe/Madrid");

    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final ExpenseRepository expenseRepository;

    public SavingsGoalSettlementService(SavingsGoalRepository goalRepository,
                                        GoalContributionRepository contributionRepository,
                                        ExpenseRepository expenseRepository) {
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.expenseRepository = expenseRepository;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Madrid")
    @Transactional
    public void settleDueGoals() {
        LocalDate today = LocalDate.now(APP_ZONE);
        List<SavingsGoal> dueGoals = goalRepository.findAllByActiveTrueAndSettledFalseAndTargetDateLessThanEqual(today);
        for (SavingsGoal goal : dueGoals) {
            Map<Long, User> contributors = new LinkedHashMap<>();
            Map<Long, BigDecimal> totalsByUser = new LinkedHashMap<>();
            for (var contribution : contributionRepository.findAllByGoal(goal)) {
                Long userId = contribution.getUser().getId();
                contributors.putIfAbsent(userId, contribution.getUser());
                totalsByUser.merge(userId, contribution.getAmount(), BigDecimal::add);
            }
            for (Map.Entry<Long, BigDecimal> entry : totalsByUser.entrySet()) {
                User contributor = contributors.get(entry.getKey());
                BigDecimal total = entry.getValue();
                if (total == null || total.signum() <= 0) {
                    continue;
                }

                Expense expense = new Expense();
                expense.setConcept(shorten("Meta de ahorro: " + goal.getName(), 100));
                expense.setAmount(total);
                expense.setCategory("Ahorro");
                expense.setDate(goal.getTargetDate());
                expense.setUser(contributor);
                expenseRepository.save(expense);
            }
            goal.setSettled(true);
            goalRepository.save(goal);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void catchUpDueGoalsAfterStartup() {
        settleDueGoals();
    }

    private String shorten(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
