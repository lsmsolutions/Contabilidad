package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankMonthlyClosing;
import com.silveira.accounting.models.bank.BankStatementPeriod;

public class ApplyBankMonthlyClosingFallbackUseCase {
    public BankStatementPeriod execute(BankStatementPeriod period, BankMonthlyClosing monthlyClosing) {
        double opening = Math.abs(period.openingBalance()) > 0.0001
            ? period.openingBalance()
            : monthlyClosing.openingBalance();
        double ending = Math.abs(period.statementEndingBalance()) > 0.0001
            ? period.statementEndingBalance()
            : monthlyClosing.statementEndingBalance();
        return new BankStatementPeriod(
            period.accountAlias(),
            period.sourcePdf(),
            period.periodStart(),
            period.periodEnd(),
            opening,
            ending
        );
    }
}
