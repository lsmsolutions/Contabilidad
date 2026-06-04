package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankTransaction;

import java.util.List;

public class CalculateBankNylIncomeUseCase {
    private static final String NYL_PROVIDER = "New York Life";

    public double execute(List<BankTransaction> rows) {
        return rows.stream()
            .filter(row -> !row.isPendingReview())
            .filter(row -> row.getAmount() > 0)
            .filter(row -> NYL_PROVIDER.equalsIgnoreCase(row.getProvider()))
            .mapToDouble(BankTransaction::getAmount)
            .sum();
    }
}
