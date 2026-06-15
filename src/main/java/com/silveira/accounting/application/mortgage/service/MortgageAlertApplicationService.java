package com.silveira.accounting.application.mortgage.service;

import com.silveira.accounting.models.MortgageAlert;
import com.silveira.accounting.repositories.mortgage.MortgageAlertRepository;
import java.util.List;

public class MortgageAlertApplicationService {
    private final MortgageAlertRepository repository;

    public MortgageAlertApplicationService(MortgageAlertRepository repository) {
        this.repository = repository;
    }

    public void saveAll(long statementId, List<MortgageAlert> alerts) {
        repository.saveAll(statementId, alerts);
    }
}
