package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.exception.DuplicateEmailException;
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
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final RecurringMovementRepository recurringMovementRepository;
    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final GoalInvitationRepository goalInvitationRepository;
    private final CurrentUserProvider currentUserProvider;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            RecurringMovementRepository recurringMovementRepository,
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            SavingsGoalRepository savingsGoalRepository,
            GoalContributionRepository goalContributionRepository,
            GoalInvitationRepository goalInvitationRepository,
            CurrentUserProvider currentUserProvider) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.recurringMovementRepository =
                recurringMovementRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.goalContributionRepository = goalContributionRepository;
        this.goalInvitationRepository = goalInvitationRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public User save(User user) {

        user.setId(null);

        user.setEmail(
                normalizeEmail(user.getEmail())
        );

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()
                )
        );

        try {

            return userRepository.save(user);

        } catch (DataIntegrityViolationException exception) {

            throw new DuplicateEmailException(
                    "Este email ya está registrado"
            );
        }
    }

    @Override
    public List<User> findAll() {

        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {

        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(
                normalizeEmail(email)
        );
    }

    @Override
    public void delete(Long id) {

        userRepository.deleteById(id);
    }

    @Override
    public User getCurrentUser() {

        return currentUserProvider.getCurrentUser();
    }

    @Override
    public void updateProfile(
            User user,
            String name,
            String email,
            String newPassword) {

        boolean emailChanged = !normalizeEmail(user.getEmail()).equals(normalizeEmail(email));
        boolean passwordChanged = newPassword != null && !newPassword.isBlank();
        user.setName(name);
        user.setEmail(
                normalizeEmail(email)
        );

        if (passwordChanged) {
            user.setPassword(
                    passwordEncoder.encode(
                            newPassword
                    )
            );
        }
        if (emailChanged || passwordChanged) {
            user.incrementCredentialVersion();
        }

        try {

            userRepository.save(user);

        } catch (DataIntegrityViolationException exception) {

            throw new DuplicateEmailException(
                    "Este email ya está registrado"
            );
        }
    }

    @Override
    @Transactional
    public void deleteCurrentUser() {

        User user =
                currentUserProvider.getCurrentUser();

        // Remove invitations before goals/users because their inviter and invitee
        // foreign keys intentionally preserve referential integrity.
        goalInvitationRepository.deleteAllByInviterOrInvitee(user, user);

        for (SavingsGoal goal : savingsGoalRepository.findAllByMembersContaining(user)) {
            var remainingMembers = goal.getMembers().stream()
                    .filter(member -> !user.getId().equals(member.getId()))
                    .toList();
            if (remainingMembers.isEmpty()) {
                goalContributionRepository.deleteAllByGoal(goal);
                savingsGoalRepository.delete(goal);
                continue;
            }

            goalContributionRepository.deleteAllByGoalAndUser(goal, user);
            java.math.BigDecimal remainingAmount = goalContributionRepository.findAllByGoal(goal).stream()
                    .map(com.adrianaroca.smartfinance.model.GoalContribution::getAmount)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            goal.setCurrentAmount(remainingAmount);
            goal.removeMember(user.getId());
            if (goal.getUser() != null && user.getId().equals(goal.getUser().getId())) {
                goal.setUser(remainingMembers.get(0));
            }
            if (goal.getParticipant() != null && user.getId().equals(goal.getParticipant().getId())) {
                goal.setParticipant(null);
            }
            savingsGoalRepository.save(goal);
        }

        recurringMovementRepository.deleteAllByUser(user);
        incomeRepository.deleteAllByUser(user);
        expenseRepository.deleteAllByUser(user);
        budgetRepository.deleteAllByUser(user);
        categoryRepository.deleteAllByUser(user);
        userRepository.delete(user);
    }

    private String normalizeEmail(String email) {

        return email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }
}
