package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.exception.DuplicateEmailException;
import com.adrianaroca.smartfinance.model.GoalContribution;
import com.adrianaroca.smartfinance.model.SavingsGoal;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.BudgetRepository;
import com.adrianaroca.smartfinance.repository.CategoryRepository;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.GoalContributionRepository;
import com.adrianaroca.smartfinance.repository.GoalInvitationRepository;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import com.adrianaroca.smartfinance.repository.RecurringMovementRepository;
import com.adrianaroca.smartfinance.repository.SavingsGoalRepository;
import com.adrianaroca.smartfinance.repository.UserRepository;
import com.adrianaroca.smartfinance.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private RecurringMovementRepository recurringMovementRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private GoalContributionRepository goalContributionRepository;

    @Mock
    private GoalInvitationRepository goalInvitationRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {

        userService =
                new UserServiceImpl(
                        userRepository,
                        passwordEncoder,
                        incomeRepository,
                        expenseRepository,
                        recurringMovementRepository,
                        budgetRepository,
                        categoryRepository,
                        savingsGoalRepository,
                        goalContributionRepository,
                        goalInvitationRepository,
                        currentUserProvider
                );
    }

    @Test
    void saveIgnoresIncomingIdAndAlwaysEncodesPassword() {

        User user = new User();

        user.setId(1L);
        user.setName("Adrián");
        user.setEmail(" ADRIAN@TEST.COM ");
        user.setPassword("{noop}12345678");

        when(passwordEncoder.encode("{noop}12345678"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        userService.save(user);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User savedUser =
                captor.getValue();

        assertNull(savedUser.getId());

        assertEquals(
                "adrian@test.com",
                savedUser.getEmail()
        );

        assertEquals(
                "encoded-password",
                savedUser.getPassword()
        );
    }

    @Test
    void saveRejectsDuplicateEmailFromDatabase() {

        User user = new User();

        user.setName("Adrián");
        user.setEmail("adrian@test.com");
        user.setPassword("password");

        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Duplicate email"
                        )
                );

        DuplicateEmailException exception =
                assertThrows(
                        DuplicateEmailException.class,
                        () -> userService.save(user)
                );

        assertEquals(
                "Este email ya está registrado",
                exception.getMessage()
        );
    }

    @Test
    void findByEmailNormalizesEmail() {

        User user =
                new User(
                        "Adrián",
                        "adrian@test.com",
                        "encoded-password"
                );

        when(userRepository.findByEmail("adrian@test.com"))
                .thenReturn(Optional.of(user));

        Optional<User> result =
                userService.findByEmail(
                        "  ADRIAN@TEST.COM "
                );

        assertEquals(
                user,
                result.orElseThrow()
        );
    }

    @Test
    void deletingAccountRemovesPersonalDataAndPreservesSharedGoalsForOtherMembers() {
        User departingUser = new User("Ada", "ada@example.com", "encoded");
        departingUser.setId(1L);
        User remainingUser = new User("Lin", "lin@example.com", "encoded");
        remainingUser.setId(2L);
        when(currentUserProvider.getCurrentUser()).thenReturn(departingUser);

        SavingsGoal personalGoal = goal("Personal", departingUser);
        SavingsGoal sharedGoal = goal("Shared", departingUser);
        sharedGoal.addMember(remainingUser);
        GoalContribution remainingContribution = new GoalContribution();
        remainingContribution.setAmount(new BigDecimal("25.00"));
        when(savingsGoalRepository.findAllByMembersContaining(departingUser))
                .thenReturn(List.of(personalGoal, sharedGoal));
        when(goalContributionRepository.findAllByGoal(sharedGoal))
                .thenReturn(List.of(remainingContribution));

        userService.deleteCurrentUser();

        verify(goalInvitationRepository).deleteAllByInviterOrInvitee(departingUser, departingUser);
        verify(goalContributionRepository).deleteAllByGoal(personalGoal);
        verify(goalContributionRepository).deleteAllByGoalAndUser(sharedGoal, departingUser);
        verify(savingsGoalRepository).delete(personalGoal);
        verify(savingsGoalRepository).save(sharedGoal);
        assertEquals(remainingUser, sharedGoal.getUser());
        assertEquals(new BigDecimal("25.00"), sharedGoal.getCurrentAmount());
        assertEquals(1, sharedGoal.getMembers().size());
        verify(recurringMovementRepository).deleteAllByUser(departingUser);
        verify(incomeRepository).deleteAllByUser(departingUser);
        verify(expenseRepository).deleteAllByUser(departingUser);
        verify(budgetRepository).deleteAllByUser(departingUser);
        verify(categoryRepository).deleteAllByUser(departingUser);
        verify(userRepository).delete(departingUser);
    }

    private SavingsGoal goal(String name, User owner) {
        SavingsGoal goal = new SavingsGoal();
        goal.setName(name);
        goal.setUser(owner);
        goal.addMember(owner);
        return goal;
    }
}
