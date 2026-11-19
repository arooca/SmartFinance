package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.GoalInvitation;
import com.adrianaroca.smartfinance.model.GoalNotification;
import com.adrianaroca.smartfinance.model.SavingsGoal;

import java.util.List;

public interface GoalInvitationService {

    void validateInviteEmails(Long goalId, String emails);

    void createGoalAndInvite(SavingsGoal proposedGoal, String emails);

    void inviteMembers(Long goalId, String emails);

    List<GoalInvitation> getPendingInvitations();

    List<GoalNotification> getNotifications();

    long getPendingCount();

    void respond(Long invitationId, boolean accepted);

    void respondToRemoval(Long notificationId, boolean remove);
}
