package com.adrianaroca.smartfinance.dto;

import com.adrianaroca.smartfinance.model.CategoryType;

public record CategoryCreateRequest(String name, CategoryType type) {
}
