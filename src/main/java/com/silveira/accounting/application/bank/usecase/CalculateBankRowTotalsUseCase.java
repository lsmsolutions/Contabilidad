package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.SourceTotals;

import java.util.List;

public class CalculateBankRowTotalsUseCase {
    public SourceTotals execute(List<BankTransaction> rows) {
        int reviewed = 0;
        int pending = 0;
        double deposits = 0;
        double withdrawals = 0;
        for (BankTransaction row : rows) {
            if (row.isPendingReview()) {
                pending++;
                continue;
            }
            reviewed++;
            if (row.getAmount() >= 0) {
                deposits += row.getAmount();
            } else {
                withdrawals += row.getAmount();
            }
        }
        return new SourceTotals(reviewed, pending, deposits, withdrawals, deposits + withdrawals);
    }
}
