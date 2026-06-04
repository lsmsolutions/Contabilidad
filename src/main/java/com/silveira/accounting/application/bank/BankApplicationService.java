package com.silveira.accounting.application.bank;

import com.silveira.accounting.application.bank.service.BankAccountApplicationService;
import com.silveira.accounting.application.bank.service.BankImportApplicationService;
import com.silveira.accounting.application.bank.service.BankPeriodApplicationService;
import com.silveira.accounting.application.bank.service.BankTransactionApplicationService;
import com.silveira.accounting.repositories.BankAccountRepository;
import com.silveira.accounting.repositories.BankMonthlyClosingRepository;
import com.silveira.accounting.repositories.BankStatementPeriodRepository;
import com.silveira.accounting.repositories.BankTransactionRepository;

public class BankApplicationService {
    private final BankAccountApplicationService accounts;
    private final BankTransactionApplicationService transactions;
    private final BankPeriodApplicationService periods;
    private final BankImportApplicationService imports;

    public BankApplicationService(
        BankTransactionRepository transactions,
        BankAccountRepository accounts,
        BankStatementPeriodRepository periods,
        BankMonthlyClosingRepository closings
    ) {
        this.accounts = new BankAccountApplicationService(accounts);
        this.transactions = new BankTransactionApplicationService(transactions);
        this.periods = new BankPeriodApplicationService(periods, closings, transactions);
        this.imports = new BankImportApplicationService(this.transactions, this.accounts, periods);
    }

    public BankAccountApplicationService accounts() {
        return accounts;
    }

    public BankTransactionApplicationService transactions() {
        return transactions;
    }

    public BankPeriodApplicationService periods() {
        return periods;
    }

    public BankImportApplicationService imports() {
        return imports;
    }
}
