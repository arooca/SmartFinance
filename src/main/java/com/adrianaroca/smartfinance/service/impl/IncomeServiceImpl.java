package com.adrianaroca.smartfinance.service.impl;

import com.adrianaroca.smartfinance.exception.ResourceNotFoundException;
import com.adrianaroca.smartfinance.model.Income;
import com.adrianaroca.smartfinance.model.User;
import com.adrianaroca.smartfinance.repository.IncomeRepository;
import com.adrianaroca.smartfinance.service.CurrentUserProvider;
import com.adrianaroca.smartfinance.service.IncomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;
    private final CurrentUserProvider currentUserProvider;

    public IncomeServiceImpl(
            IncomeRepository incomeRepository,
            CurrentUserProvider currentUserProvider) {

        this.incomeRepository = incomeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<Income> findAll() {

        User user =
                currentUserProvider.getCurrentUser();

        return incomeRepository.findAllByUser(user);
    }

    @Override
    public Income save(Income income) {

        User user =
                currentUserProvider.getCurrentUser();

        if (income.getId() == null) {

            income.setUser(user);

            return incomeRepository.save(income);
        }

        Income existingIncome =
                incomeRepository.findByIdAndUser(
                        income.getId(),
                        user
                );

        if (existingIncome == null) {
            throw new IllegalArgumentException(
                    "El ingreso no pertenece al usuario autenticado"
            );
        }

        income.setUser(user);

        return incomeRepository.save(income);
    }

    @Override
    public java.math.BigDecimal getTotalIncome() {

        User user =
                currentUserProvider.getCurrentUser();

        return incomeRepository.getTotalIncome(user);
    }

    @Override
    public java.math.BigDecimal getTotalIncomeBetween(
            LocalDate startDate,
            LocalDate endDate) {

        User user =
                currentUserProvider.getCurrentUser();

        return incomeRepository.getTotalIncomeBetween(
                user,
                startDate,
                endDate
        );
    }

    @Override
    public List<Income> getLatestIncomes() {

        User user =
                currentUserProvider.getCurrentUser();

        return incomeRepository.findTop5ByUserOrderByDateDesc(user);
    }

    @Override
    public List<Income> getAllIncomes() {

        User user =
                currentUserProvider.getCurrentUser();

        return incomeRepository.findAllByUser(user);
    }

    @Override
    public Income findById(Long id) {

        User user =
                currentUserProvider.getCurrentUser();

        Income income =
                incomeRepository.findByIdAndUser(id, user);

        if (income == null) {
            throw new ResourceNotFoundException(
                    "Ingreso no encontrado"
            );
        }

        return income;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {

        User user =
                currentUserProvider.getCurrentUser();

        Income income =
                incomeRepository.findByIdAndUser(id, user);

        if (income == null) {
            throw new ResourceNotFoundException(
                    "Ingreso no encontrado"
            );
        }

        incomeRepository.deleteByIdAndUser(id, user);
    }
}