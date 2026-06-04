package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.repositories.BankStatementPeriodRepository;

import java.time.LocalDate;

public class UpdateBankStatementPeriodUseCase {
    private final BankStatementPeriodRepository periods;

    public UpdateBankStatementPeriodUseCase(BankStatementPeriodRepository periods) {
        this.periods = periods;
    }

    public void execute(
        String accountAlias,
        String sourcePdf,
        LocalDate start,
        LocalDate end,
        double openingBalance,
        double statementEndingBalance
    ) {
        periods.save(new BankStatementPeriod(
            accountAlias,
            sourcePdf,
            start,
            end,
            openingBalance,
            statementEndingBalance
        ));
    }
}
