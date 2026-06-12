package com.silveira.accounting.application.card.service;

import com.silveira.accounting.models.FinancialAlert;
import com.silveira.accounting.repositories.card.FinancialAlertRepository;
import java.util.List;

public class CardAlertApplicationService {
    private final FinancialAlertRepository repository;

    public CardAlertApplicationService(FinancialAlertRepository repository) {
        this.repository = repository;
    }

    public void saveAll(long statementId, List<FinancialAlert> alerts) {
        repository.saveAll(statementId, alerts);
    }
}
