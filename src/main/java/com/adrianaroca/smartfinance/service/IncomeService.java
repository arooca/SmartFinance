package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.Income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IncomeService {

    List<Income> findAll();

    Income save(Income income);

    BigDecimal getTotalIncome();

    BigDecimal getTotalIncomeBetween(LocalDate startDate, LocalDate endDate);

    List<Income> getLatestIncomes();

    List<Income> getAllIncomes();

    Income findById(Long id);

    void deleteById(Long id);

}