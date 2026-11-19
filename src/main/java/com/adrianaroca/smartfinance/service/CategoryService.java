package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Category;
import com.adrianaroca.smartfinance.model.CategoryType;

import java.util.List;

public interface CategoryService {

    List<Category> getAvailableCategories(CategoryType type);

    boolean isAvailable(String name, CategoryType type);

    Category createCustomCategory(String name, CategoryType type);

    List<Category> getCurrentUserCategories();

    void deleteCustomCategory(Long id);
}
