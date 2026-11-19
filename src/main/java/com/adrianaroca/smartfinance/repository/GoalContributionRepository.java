package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.GoalContribution;
import com.adrianaroca.smartfinance.model.SavingsGoal;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    List<GoalContribution> findAllByUserOrderByContributedAtDesc(User user);

    long countByGoal(SavingsGoal goal);

    List<GoalContribution> findAllByGoal(SavingsGoal goal);

    void deleteAllByGoal(SavingsGoal goal);

    List<GoalContribution> findAllByGoalAndUser(SavingsGoal goal, User user);

    void deleteAllByGoalAndUser(SavingsGoal goal, User user);
}
