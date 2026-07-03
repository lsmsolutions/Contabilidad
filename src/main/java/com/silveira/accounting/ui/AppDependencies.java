package com.silveira.accounting.ui;

import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.repositories.ReviewMarkRepository;
import com.silveira.accounting.ui.bank.BankModule;
import com.silveira.accounting.ui.bank.BankWorkflow;
import com.silveira.accounting.ui.card.CardWorkflow;
import com.silveira.accounting.ui.dashboard.DashboardWorkflow;
import com.silveira.accounting.ui.internalmovement.InternalMovementWorkflow;
import com.silveira.accounting.ui.investment.InvestmentWorkflow;
import com.silveira.accounting.ui.mortgage.HouseExpenseWorkflow;
import com.silveira.accounting.ui.mortgage.MortgageWorkflow;
import com.silveira.accounting.ui.nyl.AgentLedgerWorkflow;
import com.silveira.accounting.ui.nyl.NylWorkflow;
import com.silveira.accounting.ui.vehiclelease.VehicleLeaseWorkflow;

public record AppDependencies(
    AppViewActions actions,
    BankModule bankModule,
    BankWorkflow bankWorkflow,
    InvestmentWorkflow investmentWorkflow,
    VehicleLeaseWorkflow vehicleLeaseWorkflow,
    CardAccountApplicationService creditCardAccountRepository,
    CardWorkflow cardWorkflow,
    InternalMovementWorkflow internalMovementWorkflow,
    MortgageApplicationService mortgageApplication,
    HouseExpenseWorkflow houseExpenseWorkflow,
    MortgageWorkflow mortgageWorkflow,
    NylWorkflow nylWorkflow,
    AgentLedgerWorkflow agentLedgerWorkflow,
    ReviewMarkRepository reviewMarkRepository,
    DashboardWorkflow dashboardWorkflow
) {
}
