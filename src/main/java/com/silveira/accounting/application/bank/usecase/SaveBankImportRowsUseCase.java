package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.application.bank.dto.BankImportSaveResult;
import com.silveira.accounting.application.bank.service.BankTransactionApplicationService;
import com.silveira.accounting.models.bank.BankTransaction;

import java.util.List;

public class SaveBankImportRowsUseCase {
    private final BankTransactionApplicationService transactions;

    public SaveBankImportRowsUseCase(BankTransactionApplicationService transactions) {
        this.transactions = transactions;
    }

    public BankImportSaveResult execute(List<BankTransaction> rows) {
        rows.forEach(transactions::normalizeSign);
        int inserted = transactions.saveAll(rows);
        long pending = rows.stream().filter(BankTransaction::isPendingReview).count();
        long reviewed = rows.size() - pending;
        return new BankImportSaveResult(inserted, reviewed, pending);
    }
}
