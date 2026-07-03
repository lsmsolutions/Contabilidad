package com.silveira.accounting.ui.dashboard;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.dashboard.DashboardApplicationService;
import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.controllers.dashboard.DashboardController;
import com.silveira.accounting.repositories.NylRecordRepository;
import com.silveira.accounting.repositories.dashboard.DashboardDataRepository;

public class DashboardModule {
    private final DashboardController controller;

    public DashboardModule(
        CardAccountApplicationService cardAccounts,
        CardStatementApplicationService cardStatements,
        BankApplicationService bank,
        NylRecordRepository nylRecords,
        MortgageApplicationService mortgage
    ) {
        controller = new DashboardController(new DashboardApplicationService(
            new DashboardDataRepository(
                cardAccounts,
                cardStatements,
                bank.accounts(),
                bank.periods(),
                nylRecords,
                mortgage.statements()
            )
        ));
    }

    public DashboardController controller() {
        return controller;
    }
}
