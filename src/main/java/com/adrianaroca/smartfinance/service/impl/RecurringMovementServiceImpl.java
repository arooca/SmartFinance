package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.exception.ResourceNotFoundException;
import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.RecurringMovement;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import com.adrianaroca.smartfinance.repository.RecurringMovementRepository;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.RecurringMovementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class RecurringMovementServiceImpl
        implements RecurringMovementService {

    private final RecurringMovementRepository recurringMovementRepository;
    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrentUserProvider currentUserProvider;

    public RecurringMovementServiceImpl(
            RecurringMovementRepository recurringMovementRepository,
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            CurrentUserProvider currentUserProvider) {

        this.recurringMovementRepository =
                recurringMovementRepository;
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<RecurringMovement> getCurrentUserRecurringMovements() {

        return recurringMovementRepository
                .findAllByUserOrderByActiveDescNextExecutionDateAsc(
                        currentUserProvider.getCurrentUser()
                );
    }

    @Override
    public RecurringMovement findById(Long id) {

        RecurringMovement recurringMovement =
                recurringMovementRepository.findByIdAndUser(
                        id,
                        currentUserProvider.getCurrentUser()
                );

        if (recurringMovement == null) {
            throw new ResourceNotFoundException(
                    "Movimiento recurrente no encontrado"
            );
        }

        return recurringMovement;
    }

    @Override
    public RecurringMovement save(
            RecurringMovement recurringMovement) {

        User user =
                currentUserProvider.getCurrentUser();

        LocalDate today = LocalDate.now();

        RecurringMovement existing = null;

        if (recurringMovement.getId() == null) {

            recurringMovement.setUser(user);

        } else {

            existing =
                    recurringMovementRepository.findByIdAndUser(
                            recurringMovement.getId(),
                            user
                    );

            if (existing == null) {

                throw new ResourceNotFoundException(
                        "Movimiento recurrente no encontrado"
                );
            }

            recurringMovement.setUser(user);
        }

        recurringMovement.setType(
                recurringMovement.getType().toUpperCase()
        );

        boolean recalculateNextExecution =
                existing == null
                        || existing.getNextExecutionDate() == null
                        || existing.getNextExecutionDate().isBefore(today)
                        || !existing.getDayOfMonth().equals(
                        recurringMovement.getDayOfMonth()
                );

        if (recalculateNextExecution) {

            recurringMovement.setNextExecutionDate(
                    calculateNextExecutionDate(
                            recurringMovement.getDayOfMonth(),
                            today
                    )
            );

        } else {

            recurringMovement.setNextExecutionDate(
                    existing.getNextExecutionDate()
            );
        }

        return recurringMovementRepository.save(
                recurringMovement
        );
    }

    @Override
    @Transactional
    public void deleteById(Long id) {

        User user =
                currentUserProvider.getCurrentUser();

        RecurringMovement recurringMovement =
                recurringMovementRepository.findByIdAndUser(
                        id,
                        user
                );

        if (recurringMovement == null) {
            throw new ResourceNotFoundException(
                    "Movimiento recurrente no encontrado"
            );
        }

        recurringMovementRepository.deleteByIdAndUser(
                id,
                user
        );
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {

        RecurringMovement recurringMovement =
                findById(id);

        recurringMovement.setActive(
                !recurringMovement.isActive()
        );

        if (recurringMovement.isActive()) {

            recurringMovement.setNextExecutionDate(
                    calculateNextExecutionDate(
                            recurringMovement.getDayOfMonth(),
                            LocalDate.now()
                    )
            );
        }

        recurringMovementRepository.save(
                recurringMovement
        );
    }

    @Override
    @Scheduled(
            cron = "0 * * * * *",
            zone = "Europe/Madrid"
    )
    @Transactional
    public void processDueMovements() {

        LocalDate today = LocalDate.now();

        List<RecurringMovement> dueMovements =
                recurringMovementRepository
                        .findAllByActiveTrueAndNextExecutionDateLessThanEqual(
                                today
                        );

        for (RecurringMovement recurringMovement :
                dueMovements) {

            generateMovement(recurringMovement);
        }
    }

    private void generateMovement(
            RecurringMovement recurringMovement) {

        LocalDate executionDate =
                recurringMovement.getNextExecutionDate();

        if (executionDate == null) {

            recurringMovement.setNextExecutionDate(
                    calculateNextExecutionDate(
                            recurringMovement.getDayOfMonth(),
                            LocalDate.now()
                    )
            );

            recurringMovementRepository.save(
                    recurringMovement
            );

            return;
        }

        User user =
                recurringMovement.getUser();

        if ("INCOME".equals(
                recurringMovement.getType())) {

            Income income = new Income();

            income.setConcept(
                    recurringMovement.getConcept()
            );

            income.setAmount(
                    recurringMovement.getAmount()
            );

            income.setCategory(
                    recurringMovement.getCategory()
            );

            income.setDate(executionDate);

            income.setUser(user);

            incomeRepository.save(income);

        } else if ("EXPENSE".equals(
                recurringMovement.getType())) {

            Expense expense = new Expense();

            expense.setConcept(
                    recurringMovement.getConcept()
            );

            expense.setAmount(
                    recurringMovement.getAmount()
            );

            expense.setCategory(
                    recurringMovement.getCategory()
            );

            expense.setDate(executionDate);

            expense.setUser(user);

            expenseRepository.save(expense);
        }

        recurringMovement.setNextExecutionDate(
                calculateNextExecutionDate(
                        recurringMovement.getDayOfMonth(),
                        executionDate.plusDays(1)
                )
        );

        recurringMovementRepository.save(
                recurringMovement
        );
    }

    private LocalDate calculateNextExecutionDate(
            int dayOfMonth,
            LocalDate referenceDate) {

        YearMonth currentMonth =
                YearMonth.from(referenceDate);

        int validDay =
                Math.min(
                        dayOfMonth,
                        currentMonth.lengthOfMonth()
                );

        LocalDate candidate =
                currentMonth.atDay(validDay);

        if (!candidate.isBefore(referenceDate)) {
            return candidate;
        }

        YearMonth nextMonth =
                currentMonth.plusMonths(1);

        return nextMonth.atDay(
                Math.min(
                        dayOfMonth,
                        nextMonth.lengthOfMonth()
                )
        );
    }
}