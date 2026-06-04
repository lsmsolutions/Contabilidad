package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.repositories.BankStatementPeriodRepository;

import java.time.LocalDate;

public class SaveManualBankPeriodUseCase {
    private final BankStatementPeriodRepository periods;

    public SaveManualBankPeriodUseCase(BankStatementPeriodRepository periods) {
        this.periods = periods;
    }

    public void execute(String accountAlias, LocalDate start, LocalDate end, double openingBalance, double statementEndingBalance) {
        String source = "periodo_manual_" + start.toString().replace("-", "") + "_" + end.toString().replace("-", "");
        periods.save(new BankStatementPeriod(
            accountAlias,
            source,
            start,
            end,
            openingBalance,
            statementEndingBalance
        ));
    }
}
