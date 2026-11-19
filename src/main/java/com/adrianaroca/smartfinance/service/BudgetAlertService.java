package com.adrianaroca.smartfinance.service;

import com.adrianaroca.smartfinance.model.BudgetNotification;

import java.util.List;

public interface BudgetAlertService {

    long refreshAndCountUnread();

    List<BudgetNotification> getAndMarkUnread();
}
