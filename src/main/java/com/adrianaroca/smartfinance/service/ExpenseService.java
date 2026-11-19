package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    List<Expense> findAll();

    Expense save(Expense expense);

    BigDecimal getTotalExpense();

    BigDecimal getTotalExpenseBetween(LocalDate startDate, LocalDate endDate);

    List<Expense> getLatestExpenses();

    List<Expense> getAllExpenses();

    Expense findById(Long id);

    void deleteById(Long id);

}