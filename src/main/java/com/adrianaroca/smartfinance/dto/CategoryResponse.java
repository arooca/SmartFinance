package com.adrianaroca.smartfinance.dto;

import com.adrianaroca.smartfinance.model.Category;
import com.adrianaroca.smartfinance.model.CategoryType;

public record CategoryResponse(Long id, String name, CategoryType type) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType()
        );
    }
}
