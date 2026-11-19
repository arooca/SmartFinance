package com.adrianaroca.smartfinance.repository;

import com.adrianaroca.smartfinance.model.GoalInvitation;
import com.adrianaroca.smartfinance.model.GoalInvitationStatus;
import com.adrianaroca.smartfinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface GoalInvitationRepository extends JpaRepository<GoalInvitation, Long> {

    List<GoalInvitation> findAllByInviteeAndStatusOrderByCreatedAtDesc(User invitee, GoalInvitationStatus status);

    long countByInviteeAndStatus(User invitee, GoalInvitationStatus status);

    boolean existsByGoalAndInviteeAndStatus(
            com.adrianaroca.smartfinance.model.SavingsGoal goal,
            User invitee,
            GoalInvitationStatus status);

    long countByGoalAndStatus(com.adrianaroca.smartfinance.model.SavingsGoal goal,
                              GoalInvitationStatus status);

    void deleteAllByGoalAndStatus(com.adrianaroca.smartfinance.model.SavingsGoal goal,
                                  GoalInvitationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    GoalInvitation findByIdAndInvitee(Long id, User invitee);

    void deleteAllByInviterOrInvitee(User inviter, User invitee);
}
