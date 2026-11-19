package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.model.Category;
import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.CategoryRepository;
import com.adrianaroca.smartfinance.repository.BudgetRepository;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import com.adrianaroca.smartfinance.repository.RecurringMovementRepository;
import com.adrianaroca.smartfinance.service.CategoryService;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringMovementRepository recurringMovementRepository;
    private final BudgetRepository budgetRepository;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            CurrentUserProvider currentUserProvider,
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            RecurringMovementRepository recurringMovementRepository,
            BudgetRepository budgetRepository) {
        this.categoryRepository = categoryRepository;
        this.currentUserProvider = currentUserProvider;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.recurringMovementRepository = recurringMovementRepository;
        this.budgetRepository = budgetRepository;
    }

    @Override
    public List<Category> getAvailableCategories(CategoryType type) {
        User user = currentUserProvider.getCurrentUser();
        return categoryRepository
                .findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
                        type,
                        type,
                        user.getId()
                );
    }

    @Override
    public boolean isAvailable(String name, CategoryType type) {
        if (name == null || type == null) {
            return false;
        }
        return getAvailableCategories(type).stream()
                .anyMatch(category -> category.getName().equals(name));
    }

    @Override
    @Transactional
    public Category createCustomCategory(String name, CategoryType type) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("La categoría no puede superar los 100 caracteres");
        }
        if (type == null) {
            throw new IllegalArgumentException("El tipo de categoría no es válido");
        }

        User user = currentUserProvider.getCurrentUser();
        boolean alreadyAvailable = categoryRepository
                .findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
                        type,
                        type,
                        user.getId()
                )
                .stream()
                .anyMatch(category -> category.getName().equalsIgnoreCase(normalizedName));

        if (alreadyAvailable) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría con ese nombre para este tipo"
            );
        }

        return categoryRepository.save(
                new Category(normalizedName, type, user)
        );
    }

    @Override
    public List<Category> getCurrentUserCategories() {
        return categoryRepository.findAllByUser_IdOrderByTypeAscNameAsc(
                currentUserProvider.getCurrentUser().getId());
    }

    @Override
    @Transactional
    public void deleteCustomCategory(Long id) {
        User user = currentUserProvider.getCurrentUser();
        Category category = categoryRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoría no existe o no pertenece a tu cuenta"));
        String name = category.getName();
        boolean inUse = switch (category.getType()) {
            case INCOME -> incomeRepository.existsByUserAndCategoryIgnoreCase(user, name)
                    || recurringMovementRepository.existsByUserAndCategoryIgnoreCase(user, name);
            case EXPENSE -> expenseRepository.existsByUserAndCategoryIgnoreCase(user, name)
                    || recurringMovementRepository.existsByUserAndCategoryIgnoreCase(user, name)
                    || budgetRepository.existsByUserAndCategoryIgnoreCase(user, name);
        };
        if (inUse) {
            throw new IllegalArgumentException(
                    "No puedes eliminar una categoría que ya está en uso en movimientos, recurrentes o presupuestos.");
        }
        categoryRepository.delete(category);
    }
}
