package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.application.bank.dto.BankImportReview;
import com.silveira.accounting.application.bank.service.BankAccountApplicationService;
import com.silveira.accounting.application.bank.service.BankTransactionApplicationService;
import com.silveira.accounting.models.bank.BankTransaction;

import java.util.List;
import java.util.Set;

public class PrepareBankImportReviewUseCase {
    private final BankTransactionApplicationService transactions;
    private final BankAccountApplicationService accounts;

    public PrepareBankImportReviewUseCase(BankTransactionApplicationService transactions, BankAccountApplicationService accounts) {
        this.transactions = transactions;
        this.accounts = accounts;
    }

    public BankImportReview execute(List<BankTransaction> parsed, String selectedAccountAlias) {
        Set<String> existing = transactions.existingFingerprints(parsed);
        List<BankTransaction> newTransactions = parsed.stream()
            .filter(transaction -> !existing.contains(transaction.getFingerprint()))
            .toList();
        newTransactions.forEach(transaction -> {
            transaction.setPendingReview(true);
            transaction.setReviewRequired(true);
            transaction.setReviewNotes("Revisar contra el PDF original");
        });

        if (selectedAccountAlias != null && !selectedAccountAlias.isBlank()) {
            parsed.forEach(transaction -> transaction.setAccountAlias(selectedAccountAlias));
            newTransactions.forEach(transaction -> transaction.setAccountAlias(selectedAccountAlias));
        }

        parsed.stream()
            .map(BankTransaction::getAccountAlias)
            .filter(alias -> alias != null && !alias.isBlank())
            .distinct()
            .forEach(alias -> accounts.ensureExists(alias, "Detectada al importar PDF"));

        return new BankImportReview(newTransactions, existing.size());
    }
}
