package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.model.DashboardMovement;
import com.adrianaroca.smartfinance.model.Category;
import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.service.CategoryService;
import com.adrianaroca.smartfinance.service.MovementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class MovementController {

    private final MovementService movementService;
    private final CategoryService categoryService;

    public MovementController(MovementService movementService, CategoryService categoryService) {
        this.movementService = movementService;
        this.categoryService = categoryService;
    }

    @GetMapping("/movements")
    public String movements(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {

        Page<DashboardMovement> movements =
                movementService.getMovements(
                        type,
                        search,
                        category,
                        startDate,
                        endDate,
                        pageable
                );

        model.addAttribute("movements", movements);
        model.addAttribute("selectedType", type);
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        List<Category> incomeCategories =
                categoryService.getAvailableCategories(CategoryType.INCOME);
        List<Category> expenseCategories =
                categoryService.getAvailableCategories(CategoryType.EXPENSE);
        model.addAttribute("incomeCategories", incomeCategories);
        model.addAttribute("expenseCategories", expenseCategories);
        List<Category> categories = switch (type) {
            case "INCOME" -> incomeCategories;
            case "EXPENSE" -> expenseCategories;
            case "SAVING" -> List.of();
            default -> java.util.stream.Stream.concat(
                            incomeCategories.stream(),
                            expenseCategories.stream())
                    .toList();
        };
        model.addAttribute("categories", categories);

        return "movements/list";
    }
}
