package com.adrianaroca.smartfinance.config;

import com.adrianaroca.smartfinance.service.GoalInvitationService;
import com.adrianaroca.smartfinance.service.BudgetAlertService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.beans.factory.ObjectProvider;

@ControllerAdvice(annotations = Controller.class)
public class NotificationModelAdvice {

    private final ObjectProvider<GoalInvitationService> invitationServiceProvider;
    private final ObjectProvider<BudgetAlertService> budgetAlertServiceProvider;

    public NotificationModelAdvice(ObjectProvider<GoalInvitationService> invitationServiceProvider,
                                   ObjectProvider<BudgetAlertService> budgetAlertServiceProvider) {
        this.invitationServiceProvider = invitationServiceProvider;
        this.budgetAlertServiceProvider = budgetAlertServiceProvider;
    }

    @ModelAttribute("pendingGoalInvitationCount")
    public long pendingGoalInvitationCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return 0;
        }
        GoalInvitationService invitationService = invitationServiceProvider.getIfAvailable();
        return invitationService == null ? 0 : invitationService.getPendingCount();
    }

    @ModelAttribute("pendingBudgetNotificationCount")
    public long pendingBudgetNotificationCount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return 0;
        }
        BudgetAlertService alertService = budgetAlertServiceProvider.getIfAvailable();
        return alertService == null ? 0 : alertService.refreshAndCountUnread();
    }
}
