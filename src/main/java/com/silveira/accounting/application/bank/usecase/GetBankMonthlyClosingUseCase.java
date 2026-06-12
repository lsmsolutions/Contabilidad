package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankMonthlyClosing;
import com.silveira.accounting.repositories.bank.BankMonthlyClosingRepository;

public class GetBankMonthlyClosingUseCase {
    private final BankMonthlyClosingRepository closings;

    public GetBankMonthlyClosingUseCase(BankMonthlyClosingRepository closings) {
        this.closings = closings;
    }

    public BankMonthlyClosing execute(String accountAlias, int year, int month) {
        return closings.find(accountAlias, year, month)
            .orElse(new BankMonthlyClosing(accountAlias, year, month, 0, 0));
    }
}
