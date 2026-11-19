package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.service.GoalInvitationService;
import com.adrianaroca.smartfinance.service.BudgetAlertService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NotificationController {

    private final GoalInvitationService invitationService;
    private final BudgetAlertService budgetAlertService;

    public NotificationController(GoalInvitationService invitationService,
                                  BudgetAlertService budgetAlertService) {
        this.invitationService = invitationService;
        this.budgetAlertService = budgetAlertService;
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("invitations", invitationService.getPendingInvitations());
        model.addAttribute("goalNotifications", invitationService.getNotifications());
        model.addAttribute("budgetNotifications", budgetAlertService.getAndMarkUnread());
        return "notifications";
    }

    @PostMapping("/notifications/goals/{id}/accept")
    public String accept(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invitationService.respond(id, true);
            redirectAttributes.addFlashAttribute("successMessage", "Invitación aceptada. La meta ya está disponible para las personas participantes.");
            return "redirect:/goals";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/notifications";
        }
    }

    @PostMapping("/notifications/goals/{id}/decline")
    public String decline(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        invitationService.respond(id, false);
        redirectAttributes.addFlashAttribute("successMessage", "Invitación rechazada.");
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/goals/removal/{id}")
    public String respondToRemoval(@PathVariable Long id,
                                   @RequestParam("remove") boolean remove,
                                   RedirectAttributes redirectAttributes) {
        invitationService.respondToRemoval(id, remove);
        redirectAttributes.addFlashAttribute("successMessage", remove
                ? "La meta se ha eliminado de tu cuenta."
                : "Has conservado la meta en tu cuenta.");
        return "redirect:/notifications";
    }
}
