package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MovementImportServiceTest {

    @Test
    void importsQuotedConceptWithCommaQuotesAndLineBreak() {
        IncomeRepository incomeRepository = mock(IncomeRepository.class);
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        CategoryService categoryService = mock(CategoryService.class);
        User user = new User("Ada", "ada@example.com", "encoded-password");
        String csv = "date,description,amount\n"
                + "2026-01-15,\"Cafe, \"\"La Plaza\"\"\nand desayuno\",125.50\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(categoryService.getAvailableCategories(any())).thenReturn(List.of());
        when(incomeRepository.existsByUserAndDateAndConceptIgnoreCaseAndAmount(
                eq(user), any(), any(), any())).thenReturn(false);
        when(incomeRepository.save(any(Income.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovementImportService service = new MovementImportService(
                incomeRepository, expenseRepository, currentUserProvider, categoryService);

        MovementImportService.ImportResult result = service.importCsv(file);

        assertEquals(1, result.imported());
        assertEquals(0, result.duplicates());
        org.mockito.ArgumentCaptor<Income> captured = org.mockito.ArgumentCaptor.forClass(Income.class);
        verify(incomeRepository).save(captured.capture());
        assertEquals("Cafe, \"La Plaza\"\nand desayuno", captured.getValue().getConcept());
        assertEquals(new BigDecimal("125.50"), captured.getValue().getAmount());
        verify(expenseRepository, never()).save(any());
    }
}
