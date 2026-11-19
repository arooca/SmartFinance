package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.SavingsGoal;
import com.adrianaroca.smartfinance.service.SavingsGoalService;
import com.adrianaroca.smartfinance.service.GoalInvitationService;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;
    private final GoalInvitationService invitationService;
    private final CurrentUserProvider currentUserProvider;

    public SavingsGoalController(SavingsGoalService savingsGoalService,
                                 GoalInvitationService invitationService,
                                 CurrentUserProvider currentUserProvider) {
        this.savingsGoalService = savingsGoalService;
        this.invitationService = invitationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("goals", savingsGoalService.getGoals());
        model.addAttribute("currentUserId", currentUserProvider.getCurrentUser().getId());
        return "goals/list";
    }

    @GetMapping("/new")
    public String newGoal(Model model) {
        SavingsGoal goal = new SavingsGoal();
        goal.setCurrentAmount(BigDecimal.ZERO);
        model.addAttribute("goal", goal);
        model.addAttribute("editing", false);
        model.addAttribute("targetDateValue", "");
        return "goals/form";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        SavingsGoal goal = savingsGoalService.findOwnedById(id);
        model.addAttribute("goal", goal);
        model.addAttribute("editing", true);
        model.addAttribute("targetDateValue", goal.getTargetDate().toString());
        return "goals/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("goal") SavingsGoal goal,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("editing", goal.getId() != null);
            Object rejectedDate = result.getFieldValue("targetDate");
            if (rejectedDate instanceof LocalDate date) {
                model.addAttribute("targetDateValue", date.toString());
            } else {
                model.addAttribute("targetDateValue", rejectedDate == null ? "" : rejectedDate.toString());
            }
            return "goals/form";
        }
        boolean editing = goal.getId() != null;
        String inviteEmails = goal.getInviteEmail();
        try {
            if (inviteEmails != null && !inviteEmails.isBlank()) {
                invitationService.validateInviteEmails(goal.getId(), inviteEmails);
            }
            if (!editing && inviteEmails != null && !inviteEmails.isBlank()) {
                invitationService.createGoalAndInvite(goal, inviteEmails);
                redirectAttributes.addFlashAttribute("successMessage", "Invitaciones enviadas. La meta aparecerá cuando una persona invitada la acepte.");
            } else {
                savingsGoalService.save(goal);
                if (editing && inviteEmails != null && !inviteEmails.isBlank()) {
                    invitationService.inviteMembers(goal.getId(), inviteEmails);
                    redirectAttributes.addFlashAttribute("successMessage", "Meta actualizada e invitaciones enviadas.");
                } else {
                    redirectAttributes.addFlashAttribute("successMessage",
                            editing ? "Meta de ahorro actualizada." : "Meta de ahorro creada.");
                }
            }
        } catch (IllegalArgumentException exception) {
            result.rejectValue("inviteEmail", "invalid", exception.getMessage());
            model.addAttribute("editing", editing);
            model.addAttribute("targetDateValue", goal.getTargetDate().toString());
            return "goals/form";
        }
        return "redirect:/goals";
    }

    @PostMapping("/{id}/contributions")
    public String contribute(@PathVariable Long id,
                             @RequestParam BigDecimal amount,
                             RedirectAttributes redirectAttributes) {
        try {
            savingsGoalService.addContribution(id, amount);
            redirectAttributes.addFlashAttribute("successMessage", "Aportación añadida a la meta.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/goals";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        savingsGoalService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Meta de ahorro eliminada.");
        return "redirect:/goals";
    }
}
