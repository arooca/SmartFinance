package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.GoalNotification;
import com.adrianaroca.smartfinance.model.GoalNotificationStatus;
import com.adrianaroca.smartfinance.model.GoalNotificationType;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GoalNotificationRepository extends JpaRepository<GoalNotification, Long> {

    List<GoalNotification> findAllByRecipientAndStatusInOrderByCreatedAtDesc(
            User recipient, Collection<GoalNotificationStatus> statuses);

    long countByRecipientAndStatusIn(User recipient, Collection<GoalNotificationStatus> statuses);

    GoalNotification findByIdAndRecipient(Long id, User recipient);

    boolean existsByGoalAndRecipientAndActorAndTypeAndStatus(
            com.adrianaroca.smartfinance.model.SavingsGoal goal,
            User recipient,
            User actor,
            GoalNotificationType type,
            GoalNotificationStatus status);
}
