package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import com.adrianaroca.smartfinance.repository.UserRepository;
import com.adrianaroca.smartfinance.service.impl.IncomeServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceImplTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private IncomeServiceImpl incomeService;

    private User currentUser;

    @BeforeEach
    void setUp() {

        incomeService =
                new IncomeServiceImpl(
                        incomeRepository,
                        currentUserProvider
                );

        currentUser =
                new User(
                        "Adrián",
                        "adrian@test.com",
                        "encoded-password"
                );

        currentUser.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "adrian@test.com",
                        null
                )
        );

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSaveNewIncomeForAuthenticatedUser() {

        Income income = new Income();

        income.setConcept("Nómina");
        income.setCategory("Salario");
        income.setAmount(new BigDecimal("1500.00"));
        income.setDate(LocalDate.of(2026, 9, 1));

        when(incomeRepository.save(income))
                .thenReturn(income);

        Income result =
                incomeService.save(income);

        assertSame(income, result);
        assertEquals(currentUser, income.getUser());

        verify(incomeRepository).save(income);
    }

    @Test
    void shouldRejectIncomeThatDoesNotBelongToAuthenticatedUser() {

        Income income = new Income();

        income.setId(99L);
        income.setConcept("Movimiento ajeno");
        income.setCategory("Otro");
        income.setAmount(new BigDecimal("100.00"));
        income.setDate(LocalDate.now());

        when(incomeRepository.findByIdAndUser(
                99L,
                currentUser
        )).thenReturn(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> incomeService.save(income)
                );

        assertEquals(
                "El ingreso no pertenece al usuario autenticado",
                exception.getMessage()
        );

        verify(incomeRepository, never())
                .save(any());
    }
}