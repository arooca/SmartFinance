package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.service.CategoryService;
import com.adrianaroca.smartfinance.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/expense")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final CategoryService categoryService;

    public ExpenseController(ExpenseService expenseService, CategoryService categoryService) {
        this.expenseService = expenseService;
        this.categoryService = categoryService;
    }

    @GetMapping("/new")
    public String showForm(Model model) {

        Expense expense = new Expense();

        expense.setDate(
                LocalDate.now()
        );

        model.addAttribute(
                "expense",
                expense
        );
        addCategories(model);

        return "expense/new";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("expense") Expense expense,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            addCategories(model);
            return "expense/new";
        }

        if (!categoryService.isAvailable(expense.getCategory(), CategoryType.EXPENSE)) {
            bindingResult.rejectValue("category", "invalid",
                    "Selecciona una categoría de gastos disponible");
            addCategories(model);
            return "expense/new";
        }

        boolean editing = expense.getId() != null;
        expenseService.save(expense);
        redirectAttributes.addFlashAttribute("successMessage",
                editing ? "Gasto actualizado correctamente." : "Gasto guardado correctamente.");

        return "redirect:/dashboard";
    }

    @GetMapping("/edit/{id}")
    public String editExpense(
            @PathVariable Long id,
            Model model) {

        Expense expense =
                expenseService.findById(id);

        model.addAttribute(
                "expense",
                expense
        );
        addCategories(model);

        return "expense/new";
    }

    @PostMapping("/delete/{id}")
    public String deleteExpense(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        expenseService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Gasto eliminado correctamente.");

        return "redirect:/movements";
    }

    private void addCategories(Model model) {
        model.addAttribute("categories",
                categoryService.getAvailableCategories(CategoryType.EXPENSE));
    }
}
