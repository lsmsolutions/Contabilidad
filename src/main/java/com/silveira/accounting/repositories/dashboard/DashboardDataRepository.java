package com.silveira.accounting.repositories.dashboard;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.application.bank.service.BankAccountApplicationService;
import com.silveira.accounting.application.bank.service.BankPeriodApplicationService;
import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.dashboard.DashboardGateway;
import com.silveira.accounting.application.investment.InvestmentApplicationService;
import com.silveira.accounting.application.mortgage.service.MortgageStatementApplicationService;
import com.silveira.accounting.application.vehiclelease.VehicleLeaseApplicationService;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.repositories.NylRecordRepository;
import java.util.List;

public class DashboardDataRepository implements DashboardGateway {
    private final CardAccountApplicationService cardAccounts;
    private final CardStatementApplicationService cardStatements;
    private final BankAccountApplicationService bankAccounts;
    private final BankPeriodApplicationService bankPeriods;
    private final NylRecordRepository nylRecords;
    private final MortgageStatementApplicationService mortgageStatements;
    private final InvestmentApplicationService investments;
    private final VehicleLeaseApplicationService vehicleLeases;

    public DashboardDataRepository(
        CardAccountApplicationService cardAccounts,
        CardStatementApplicationService cardStatements,
        BankAccountApplicationService bankAccounts,
        BankPeriodApplicationService bankPeriods,
        NylRecordRepository nylRecords,
        MortgageStatementApplicationService mortgageStatements,
        InvestmentApplicationService investments,
        VehicleLeaseApplicationService vehicleLeases
    ) {
        this.cardAccounts = cardAccounts;
        this.cardStatements = cardStatements;
        this.bankAccounts = bankAccounts;
        this.bankPeriods = bankPeriods;
        this.nylRecords = nylRecords;
        this.mortgageStatements = mortgageStatements;
        this.investments = investments;
        this.vehicleLeases = vehicleLeases;
    }

    @Override
    public List<CreditCardAccount> cardAccounts() {
        return cardAccounts.findAll();
    }

    @Override
    public List<CreditCardStatement> cardStatements(String alias) {
        return cardStatements.findByAccount(alias, null, null);
    }

    @Override
    public List<BankAccount> bankAccounts() {
        return bankAccounts.list();
    }

    @Override
    public List<BankPeriodSummary> bankPeriodSummaries(String alias) {
        return bankPeriods.summaries(alias);
    }

    @Override
    public List<NylRecord> nylRecords() {
        return nylRecords.find(null, null, null, null);
    }

    @Override
    public List<String> mortgageAliases() {
        return mortgageStatements.findLoanAliases();
    }

    @Override
    public List<MortgageStatement> mortgageStatements(String alias) {
        return mortgageStatements.findByLoan(alias, null, null);
    }

    @Override
    public List<InvestmentAccount> investmentAccounts() {
        return investments.accounts();
    }

    @Override
    public List<InvestmentStatement> investmentStatements(String alias) {
        return investments.statements(alias);
    }

    @Override
    public List<VehicleLeaseAccount> vehicleLeaseAccounts() {
        return vehicleLeases.accounts();
    }

    @Override
    public List<VehicleLeaseStatement> vehicleLeaseStatements(String alias) {
        return vehicleLeases.statements(alias);
    }
}
