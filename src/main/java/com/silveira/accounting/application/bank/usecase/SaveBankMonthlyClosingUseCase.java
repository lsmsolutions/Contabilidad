package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankMonthlyClosing;
import com.silveira.accounting.repositories.bank.BankMonthlyClosingRepository;

public class SaveBankMonthlyClosingUseCase {
    private final BankMonthlyClosingRepository closings;

    public SaveBankMonthlyClosingUseCase(BankMonthlyClosingRepository closings) {
        this.closings = closings;
    }

    public void execute(String accountAlias, int year, int month, double openingBalance, double statementEndingBalance) {
        closings.save(new BankMonthlyClosing(
            accountAlias,
            year,
            month,
            openingBalance,
            statementEndingBalance
        ));
    }
}
