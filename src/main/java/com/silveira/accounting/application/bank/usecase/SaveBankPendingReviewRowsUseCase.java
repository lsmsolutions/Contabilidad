package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.application.bank.service.BankTransactionApplicationService;
import com.silveira.accounting.models.bank.BankTransaction;

import java.util.List;

public class SaveBankPendingReviewRowsUseCase {
    private final BankTransactionApplicationService transactions;

    public SaveBankPendingReviewRowsUseCase(BankTransactionApplicationService transactions) {
        this.transactions = transactions;
    }

    public void execute(List<BankTransaction> rows) {
        for (BankTransaction transaction : rows) {
            if (transaction.getId() > 0) {
                transactions.update(transaction);
            }
        }
    }
}
