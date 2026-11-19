package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.exception.ResourceNotFoundException;
import com.adrianaroca.smartfinance.model.Expense;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.ExpenseRepository;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.ExpenseService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CurrentUserProvider currentUserProvider;

    public ExpenseServiceImpl(
            ExpenseRepository expenseRepository,
            CurrentUserProvider currentUserProvider) {

        this.expenseRepository = expenseRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<Expense> findAll() {

        User user =
                currentUserProvider.getCurrentUser();

        return expenseRepository.findAllByUser(user);
    }

    @Override
    public Expense save(Expense expense) {

        User user =
                currentUserProvider.getCurrentUser();

        if (expense.getId() == null) {

            expense.setUser(user);

            return expenseRepository.save(expense);
        }

        Expense existingExpense =
                expenseRepository.findByIdAndUser(
                        expense.getId(),
                        user
                );

        if (existingExpense == null) {
            throw new ResourceNotFoundException(
                    "Gasto no encontrado"
            );
        }

        expense.setUser(user);

        return expenseRepository.save(expense);
    }

    @Override
    public java.math.BigDecimal getTotalExpense() {

        User user =
                currentUserProvider.getCurrentUser();

        return expenseRepository.getTotalExpense(user);
    }

    @Override
    public java.math.BigDecimal getTotalExpenseBetween(
            LocalDate startDate,
            LocalDate endDate) {

        User user =
                currentUserProvider.getCurrentUser();

        return expenseRepository.getTotalExpenseBetween(
                user,
                startDate,
                endDate
        );
    }

    @Override
    public List<Expense> getLatestExpenses() {

        User user =
                currentUserProvider.getCurrentUser();

        return expenseRepository.findTop5ByUserOrderByDateDesc(user);
    }

    @Override
    public List<Expense> getAllExpenses() {

        User user =
                currentUserProvider.getCurrentUser();

        return expenseRepository.findAllByUser(user);
    }

    @Override
    public Expense findById(Long id) {

        User user =
                currentUserProvider.getCurrentUser();

        Expense expense =
                expenseRepository.findByIdAndUser(id, user);

        if (expense == null) {
            throw new ResourceNotFoundException(
                    "Gasto no encontrado"
            );
        }

        return expense;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {

        User user =
                currentUserProvider.getCurrentUser();

        Expense expense =
                expenseRepository.findByIdAndUser(id, user);

        if (expense == null) {
            throw new ResourceNotFoundException(
                    "Gasto no encontrado"
            );
        }

        expenseRepository.deleteByIdAndUser(id, user);
    }
}