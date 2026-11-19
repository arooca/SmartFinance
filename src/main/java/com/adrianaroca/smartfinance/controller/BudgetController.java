package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.Budget;
import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.service.CategoryService;
import com.adrianaroca.smartfinance.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;

@Controller
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;

    public BudgetController(BudgetService budgetService, CategoryService categoryService) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String budgets(
            @RequestParam(required = false) String month,
            Model model) {

        YearMonth selectedMonth;

        if (month == null || month.isBlank()) {

            selectedMonth = YearMonth.now();

        } else {

            try {

                selectedMonth =
                        YearMonth.parse(month);

            } catch (Exception e) {

                selectedMonth =
                        YearMonth.now();
            }
        }

        model.addAttribute(
                "budgets",
                budgetService.getBudgetsWithProgress(
                        selectedMonth
                )
        );

        model.addAttribute(
                "currentMonth",
                selectedMonth
        );

        return "budgets";
    }

    @GetMapping("/new")
    public String newBudget(Model model) {

        Budget budget = new Budget();

        budget.setMonth(
                YearMonth.now().atDay(1)
        );

        model.addAttribute(
                "budget",
                budget
        );
        addCategories(model);

        model.addAttribute(
                "editing",
                false
        );

        return "budget/new";
    }

    @GetMapping("/edit/{id}")
    public String editBudget(
            @PathVariable Long id,
            Model model) {

        Budget budget =
                budgetService.findById(id);

        model.addAttribute(
                "budget",
                budget
        );
        addCategories(model);

        model.addAttribute(
                "editing",
                true
        );

        return "budget/new";
    }

    @PostMapping("/save")
    public String saveBudget(
            @Valid @ModelAttribute("budget") Budget budget,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {

            model.addAttribute(
                    "editing",
                    budget.getId() != null
            );
            addCategories(model);

            return "budget/new";
        }

        if (!categoryService.isAvailable(budget.getCategory(), CategoryType.EXPENSE)) {
            result.rejectValue("category", "invalid",
                    "Selecciona una categoría de gastos disponible");
            model.addAttribute("editing", budget.getId() != null);
            addCategories(model);
            return "budget/new";
        }

        budget.setMonth(
                budget.getMonth().withDayOfMonth(1)
        );

        boolean duplicate =
                budget.getId() == null

                        ? budgetService
                        .existsForMonthAndCategory(
                                budget.getCategory(),
                                budget.getMonth()
                        )

                        : budgetService
                        .existsForMonthAndCategoryExcludingId(
                                budget.getCategory(),
                                budget.getMonth(),
                                budget.getId()
                        );

        if (duplicate) {

            result.rejectValue(
                    "category",
                    "duplicate",
                    "Ya existe un presupuesto para esa categoría y mes"
            );

            model.addAttribute(
                    "editing",
                    budget.getId() != null
            );
            addCategories(model);

            return "budget/new";
        }

        boolean editing = budget.getId() != null;
        budgetService.save(budget);
        redirectAttributes.addFlashAttribute("successMessage",
                editing ? "Presupuesto actualizado correctamente." : "Presupuesto creado correctamente.");

        return "redirect:/budgets?month="
                + budget.getMonth().toString()
                .substring(0, 7);
    }

    @PostMapping("/delete/{id}")
    public String deleteBudget(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        budgetService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Presupuesto eliminado correctamente.");

        return "redirect:/budgets";
    }

    private void addCategories(Model model) {
        model.addAttribute("categories",
                categoryService.getAvailableCategories(CategoryType.EXPENSE));
    }
}
