package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.service.CategoryService;
import com.adrianaroca.smartfinance.service.IncomeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/income")
public class IncomeController {

    private final IncomeService incomeService;
    private final CategoryService categoryService;

    public IncomeController(IncomeService incomeService, CategoryService categoryService) {
        this.incomeService = incomeService;
        this.categoryService = categoryService;
    }

    @GetMapping("/new")
    public String showForm(Model model) {

        Income income = new Income();

        income.setDate(
                LocalDate.now()
        );

        model.addAttribute(
                "income",
                income
        );
        addCategories(model);

        return "income/new";
    }

    @PostMapping("/save")
    public String saveIncome(
            @Valid @ModelAttribute("income") Income income,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            addCategories(model);
            return "income/new";
        }

        if (!categoryService.isAvailable(income.getCategory(), CategoryType.INCOME)) {
            bindingResult.rejectValue("category", "invalid",
                    "Selecciona una categoría de ingresos disponible");
            addCategories(model);
            return "income/new";
        }

        boolean editing = income.getId() != null;
        incomeService.save(income);
        redirectAttributes.addFlashAttribute("successMessage",
                editing ? "Ingreso actualizado correctamente." : "Ingreso guardado correctamente.");

        return "redirect:/dashboard";
    }

    @GetMapping("/edit/{id}")
    public String editIncome(
            @PathVariable Long id,
            Model model) {

        Income income =
                incomeService.findById(id);

        model.addAttribute(
                "income",
                income
        );
        addCategories(model);

        return "income/new";
    }

    @PostMapping("/delete/{id}")
    public String deleteIncome(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        incomeService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Ingreso eliminado correctamente.");

        return "redirect:/movements";
    }

    private void addCategories(Model model) {
        model.addAttribute("categories",
                categoryService.getAvailableCategories(CategoryType.INCOME));
    }
}
