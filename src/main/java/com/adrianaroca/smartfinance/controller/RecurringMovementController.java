package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.RecurringMovement;
import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.service.CategoryService;
import com.adrianaroca.smartfinance.service.RecurringMovementService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recurring")
public class RecurringMovementController {

    private final RecurringMovementService recurringMovementService;
    private final CategoryService categoryService;

    public RecurringMovementController(
            RecurringMovementService recurringMovementService,
            CategoryService categoryService) {

        this.recurringMovementService =
                recurringMovementService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {

        model.addAttribute(
                "recurringMovements",
                recurringMovementService
                        .getCurrentUserRecurringMovements()
        );

        return "recurring/list";
    }

    @GetMapping("/new")
    public String newRecurring(Model model) {

        RecurringMovement recurringMovement =
                new RecurringMovement();

        recurringMovement.setType("EXPENSE");
        recurringMovement.setActive(true);

        model.addAttribute(
                "recurringMovement",
                recurringMovement
        );

        model.addAttribute(
                "editing",
                false
        );
        addCategories(model, recurringMovement.getType());

        return "recurring/new";
    }

    @GetMapping("/edit/{id}")
    public String editRecurring(
            @PathVariable Long id,
            Model model) {

        RecurringMovement recurringMovement =
                recurringMovementService.findById(id);

        model.addAttribute(
                "recurringMovement",
                recurringMovement
        );

        model.addAttribute(
                "editing",
                true
        );
        addCategories(model, recurringMovement.getType());

        return "recurring/new";
    }

    @PostMapping("/save")
    public String saveRecurring(
            @Valid
            @ModelAttribute("recurringMovement")
            RecurringMovement recurringMovement,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            model.addAttribute(
                    "editing",
                    recurringMovement.getId() != null
            );
            addCategories(model, recurringMovement.getType());

            return "recurring/new";
        }

        CategoryType type = CategoryType.valueOf(recurringMovement.getType());
        if (!categoryService.isAvailable(recurringMovement.getCategory(), type)) {
            bindingResult.rejectValue("category", "invalid",
                    "Selecciona una categoría del tipo de movimiento elegido");
            model.addAttribute("editing", recurringMovement.getId() != null);
            addCategories(model, recurringMovement.getType());
            return "recurring/new";
        }

        boolean editing = recurringMovement.getId() != null;
        recurringMovementService.save(recurringMovement);
        redirectAttributes.addFlashAttribute("successMessage",
                editing ? "Movimiento recurrente actualizado correctamente."
                        : "Movimiento recurrente creado correctamente.");

        return "redirect:/recurring";
    }

    @PostMapping("/toggle/{id}")
    public String toggleRecurring(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        recurringMovementService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estado del movimiento recurrente actualizado.");

        return "redirect:/recurring";
    }

    @PostMapping("/delete/{id}")
    public String deleteRecurring(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        recurringMovementService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Movimiento recurrente eliminado correctamente.");

        return "redirect:/recurring";
    }

    private void addCategories(Model model, String movementType) {
        CategoryType type = "INCOME".equals(movementType)
                ? CategoryType.INCOME
                : CategoryType.EXPENSE;
        model.addAttribute("incomeCategories",
                categoryService.getAvailableCategories(CategoryType.INCOME));
        model.addAttribute("expenseCategories",
                categoryService.getAvailableCategories(CategoryType.EXPENSE));
        model.addAttribute("categories", categoryService.getAvailableCategories(type));
    }
}
