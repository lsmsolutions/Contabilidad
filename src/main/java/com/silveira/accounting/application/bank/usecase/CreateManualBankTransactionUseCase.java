package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.application.bank.service.BankTransactionApplicationService;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.utils.Fingerprint;

import java.time.LocalDate;

public class CreateManualBankTransactionUseCase {
    private final BankTransactionApplicationService transactions;

    public CreateManualBankTransactionUseCase(BankTransactionApplicationService transactions) {
        this.transactions = transactions;
    }

    public BankTransaction execute(String selectedAccountAlias, LocalDate fallbackDate, BankStatementPeriod period) {
        LocalDate date = period == null ? fallbackDate : period.periodEnd();
        String alias = selectedAccountAlias == null || selectedAccountAlias.isBlank() ? "sin_cuenta" : selectedAccountAlias;
        String fingerprint = Fingerprint.of(alias + "|" + date + "|manual|" + System.nanoTime());
        BankTransaction transaction = new BankTransaction(
            0,
            date,
            "Movimiento manual",
            0,
            "Deposito",
            "Otros",
            "",
            date.getMonthValue(),
            date.getYear(),
            period == null ? "entrada_manual" : period.sourcePdf(),
            fingerprint,
            false
        );
        transaction.setAccountAlias(alias);
        transaction.setImportStatus("manual");
        transaction.setReviewRequired(true);
        transaction.setPendingReview(true);
        transaction.setReviewNotes("Entrada manual pendiente de revisar");
        transaction.setId(transactions.save(transaction));
        return transaction;
    }
}
