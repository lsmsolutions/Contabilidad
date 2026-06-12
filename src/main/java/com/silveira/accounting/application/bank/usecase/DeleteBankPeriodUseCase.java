package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.application.bank.service.BankTransactionApplicationService;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.repositories.bank.BankStatementPeriodRepository;

import java.util.List;

public class DeleteBankPeriodUseCase {
    private final BankTransactionApplicationService transactions;
    private final BankStatementPeriodRepository periods;

    public DeleteBankPeriodUseCase(BankTransactionApplicationService transactions, BankStatementPeriodRepository periods) {
        this.transactions = transactions;
        this.periods = periods;
    }

    public void execute(BankStatementPeriod period, List<BankTransaction> periodTransactions) {
        for (BankTransaction transaction : periodTransactions) {
            if (transaction.getId() > 0) {
                transactions.delete(transaction.getId());
            }
        }
        periods.delete(period.accountAlias(), period.sourcePdf());
    }
}
