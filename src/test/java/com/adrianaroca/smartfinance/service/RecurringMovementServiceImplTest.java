package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.RecurringMovement;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import com.adrianaroca.smartfinance.repository.RecurringMovementRepository;
import com.adrianaroca.smartfinance.repository.UserRepository;
import com.adrianaroca.smartfinance.service.impl.RecurringMovementServiceImpl;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringMovementServiceImplTest {

    @Mock
    private RecurringMovementRepository recurringMovementRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private RecurringMovementServiceImpl recurringService;

    private User currentUser;

    @BeforeEach
    void setUp() {

        recurringService =
                new RecurringMovementServiceImpl(
                        recurringMovementRepository,
                        incomeRepository,
                        expenseRepository,
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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAssignAuthenticatedUserToNewRecurringMovement() {

        when(currentUserProvider.getCurrentUser())
                .thenReturn(currentUser);

        RecurringMovement recurringMovement =
                new RecurringMovement();

        recurringMovement.setConcept("Netflix");
        recurringMovement.setAmount(
                new BigDecimal("15.99")
        );
        recurringMovement.setCategory("Ocio");
        recurringMovement.setType("EXPENSE");
        recurringMovement.setDayOfMonth(15);
        recurringMovement.setActive(true);

        when(recurringMovementRepository.save(recurringMovement))
                .thenReturn(recurringMovement);

        RecurringMovement result =
                recurringService.save(recurringMovement);

        assertSame(recurringMovement, result);
        assertEquals(
                currentUser,
                recurringMovement.getUser()
        );
        assertEquals(
                "EXPENSE",
                recurringMovement.getType()
        );
        assertNotNull(
                recurringMovement.getNextExecutionDate()
        );

        verify(recurringMovementRepository)
                .save(recurringMovement);
    }

    @Test
    void shouldGenerateDueIncome() {

        RecurringMovement recurringMovement =
                new RecurringMovement();

        recurringMovement.setId(1L);
        recurringMovement.setUser(currentUser);
        recurringMovement.setConcept("Nómina");
        recurringMovement.setAmount(
                new BigDecimal("1500.00")
        );
        recurringMovement.setCategory("Salario");
        recurringMovement.setType("INCOME");
        recurringMovement.setDayOfMonth(1);
        recurringMovement.setActive(true);
        recurringMovement.setNextExecutionDate(
                LocalDate.now()
        );

        when(
                recurringMovementRepository
                        .findAllByActiveTrueAndNextExecutionDateLessThanEqual(
                                LocalDate.now()
                        )
        ).thenReturn(List.of(recurringMovement));

        recurringService.processDueMovements();

        verify(incomeRepository)
                .save(any());

        verify(recurringMovementRepository, atLeastOnce())
                .save(recurringMovement);

        assertNotEquals(
                LocalDate.now(),
                recurringMovement.getNextExecutionDate()
        );
    }
}