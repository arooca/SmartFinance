package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Category;
import com.adrianaroca.smartfinance.model.CategoryType;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.CategoryRepository;
import com.adrianaroca.smartfinance.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void listsGlobalAndCurrentUsersCategoriesForRequestedType() {
        User user = user(7L);
        List<Category> categories = List.of(
                new Category("Salario", CategoryType.INCOME, null),
                new Category("Alquiler", CategoryType.INCOME, user)
        );
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(categoryRepository
                .findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
                        CategoryType.INCOME, CategoryType.INCOME, 7L))
                .thenReturn(categories);

        assertEquals(categories,
                categoryService.getAvailableCategories(CategoryType.INCOME));

        verify(categoryRepository)
                .findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
                        CategoryType.INCOME, CategoryType.INCOME, 7L);
    }

    @Test
    void createsTrimmedPersonalCategoryForCurrentUser() {
        User user = user(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(categoryRepository
                .findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
                        CategoryType.EXPENSE, CategoryType.EXPENSE, 7L))
                .thenReturn(List.of());
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category created = categoryService.createCustomCategory(
                "  Mascotas  ", CategoryType.EXPENSE);

        assertEquals("Mascotas", created.getName());
        assertEquals(CategoryType.EXPENSE, created.getType());
        assertEquals(user, created.getUser());
    }

    @Test
    void rejectsDuplicateNameWithinAvailableCategoryType() {
        User user = user(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(categoryRepository
                .findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
                        CategoryType.INCOME, CategoryType.INCOME, 7L))
                .thenReturn(List.of(new Category("Salario", CategoryType.INCOME, null)));

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.createCustomCategory(
                        "salario", CategoryType.INCOME));
    }

    @Test
    void availabilityIsLimitedToExactNameAndCategoryType() {
        User user = user(7L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(categoryRepository
                .findByTypeAndUserIsNullOrTypeAndUser_IdOrderByNameAsc(
                        CategoryType.EXPENSE, CategoryType.EXPENSE, 7L))
                .thenReturn(List.of(new Category("Mascotas", CategoryType.EXPENSE, user)));

        assertTrue(categoryService.isAvailable("Mascotas", CategoryType.EXPENSE));
        assertFalse(categoryService.isAvailable("Mascotas", CategoryType.INCOME));
    }

    private User user(Long id) {
        User user = new User("Ada", "ada@example.com", "hashed-password");
        user.setId(id);
        return user;
    }
}
