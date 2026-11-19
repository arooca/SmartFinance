package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.SavingsGoal;

import java.math.BigDecimal;
import java.util.List;

public interface SavingsGoalService {

    List<SavingsGoal> getGoals();

    SavingsGoal findById(Long id);

    SavingsGoal findOwnedById(Long id);

    SavingsGoal save(SavingsGoal goal);

    SavingsGoal addContribution(Long id, BigDecimal amount);

    void delete(Long id);
}
