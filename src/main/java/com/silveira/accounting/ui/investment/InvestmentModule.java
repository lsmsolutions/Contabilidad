package com.silveira.accounting.ui.investment;

import com.silveira.accounting.application.investment.InvestmentApplicationService;
import com.silveira.accounting.controllers.investment.InvestmentController;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.parsers.investment.SchwabInvestmentStatementParser;
import com.silveira.accounting.repositories.investment.InvestmentAccountRepository;
import com.silveira.accounting.repositories.investment.InvestmentAllocationRepository;
import com.silveira.accounting.repositories.investment.InvestmentPositionRepository;
import com.silveira.accounting.repositories.investment.InvestmentStatementRepository;
import com.silveira.accounting.repositories.investment.InvestmentTransactionRepository;

public class InvestmentModule {
    private final InvestmentApplicationService application;
    private final InvestmentController controller;

    public InvestmentModule(DatabaseManager databaseManager) {
        application = new InvestmentApplicationService(
            new InvestmentAccountRepository(databaseManager),
            new InvestmentStatementRepository(databaseManager),
            new InvestmentAllocationRepository(databaseManager),
            new InvestmentPositionRepository(databaseManager),
            new InvestmentTransactionRepository(databaseManager),
            new SchwabInvestmentStatementParser()
        );
        controller = new InvestmentController(application);
    }

    public InvestmentController controller() {
        return controller;
    }

    public InvestmentApplicationService application() {
        return application;
    }
}
