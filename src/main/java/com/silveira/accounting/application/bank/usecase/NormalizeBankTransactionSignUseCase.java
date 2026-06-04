package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankTransaction;

import java.text.Normalizer;

public class NormalizeBankTransactionSignUseCase {
    public void execute(BankTransaction transaction) {
        if (transaction != null && isDepositType(transaction.getMovementType()) && transaction.getAmount() < 0) {
            transaction.setAmount(Math.abs(transaction.getAmount()));
        }
    }

    private boolean isDepositType(String value) {
        if (value == null) {
            return false;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .trim()
            .toLowerCase();
        return normalized.equals("deposito") || normalized.equals("deposit");
    }
}
