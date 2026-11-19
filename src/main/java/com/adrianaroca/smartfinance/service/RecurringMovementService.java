package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.RecurringMovement;

import java.util.List;

public interface RecurringMovementService {

    List<RecurringMovement> getCurrentUserRecurringMovements();

    RecurringMovement findById(Long id);

    RecurringMovement save(RecurringMovement recurringMovement);

    void deleteById(Long id);

    void toggleActive(Long id);

    void processDueMovements();
}