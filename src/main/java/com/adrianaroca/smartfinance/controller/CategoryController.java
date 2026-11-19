package com.adrianaroca.smartfinance.controller;

import com.adrianaroca.smartfinance.dto.CategoryCreateRequest;
import com.adrianaroca.smartfinance.dto.CategoryResponse;
import com.adrianaroca.smartfinance.model.Category;
import com.adrianaroca.smartfinance.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/custom")
    public CategoryResponse createCustomCategory(
            @RequestBody CategoryCreateRequest request) {
        Category category = categoryService.createCustomCategory(
                request.name(),
                request.type()
        );
        return CategoryResponse.from(category);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidCategory(
            IllegalArgumentException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception.getMessage());
    }
}
