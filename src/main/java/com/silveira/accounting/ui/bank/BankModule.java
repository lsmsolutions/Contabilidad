package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.controllers.bank.BankAccountController;
import com.silveira.accounting.controllers.bank.BankAccountDetailController;
import com.silveira.accounting.controllers.bank.BankAccountWorkflow;
import com.silveira.accounting.controllers.bank.BankImportController;
import com.silveira.accounting.controllers.bank.BankPeriodController;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.repositories.bank.BankAccountRepository;
import com.silveira.accounting.repositories.bank.BankMonthlyClosingRepository;
import com.silveira.accounting.repositories.bank.BankStatementPeriodRepository;
import com.silveira.accounting.repositories.bank.BankTransactionRepository;
import com.silveira.accounting.services.OcrService;

public final class BankModule {
    private final BankTransactionRepository transactions;
    private final BankApplicationService application;
    private final BankAccountController accounts;
    private final BankAccountDetailController accountDetails;
    private final BankAccountWorkflow accountWorkflow;
    private final BankImportController imports;
    private final BankPeriodController periods;

    public BankModule(DatabaseManager databaseManager, OcrService ocrService) {
        transactions = new BankTransactionRepository(databaseManager);
        BankAccountRepository accountsRepository = new BankAccountRepository(databaseManager);
        BankStatementPeriodRepository statementPeriods = new BankStatementPeriodRepository(databaseManager);
        BankMonthlyClosingRepository monthlyClosings = new BankMonthlyClosingRepository(databaseManager);

        application = new BankApplicationService(
            transactions,
            accountsRepository,
            statementPeriods,
            monthlyClosings
        );
        accounts = new BankAccountController(application);
        accountDetails = new BankAccountDetailController(application);
        accountWorkflow = new BankAccountWorkflow(accounts);
        imports = new BankImportController(application, ocrService);
        periods = new BankPeriodController(application);
    }

    public BankTransactionRepository transactions() {
        return transactions;
    }

    public BankApplicationService application() {
        return application;
    }

    public BankAccountController accounts() {
        return accounts;
    }

    public BankAccountDetailController accountDetails() {
        return accountDetails;
    }

    public BankAccountWorkflow accountWorkflow() {
        return accountWorkflow;
    }

    public BankImportController imports() {
        return imports;
    }

    public BankPeriodController periods() {
        return periods;
    }
}
