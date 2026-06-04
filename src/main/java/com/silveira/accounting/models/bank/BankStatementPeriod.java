package com.silveira.accounting.models.bank;

import java.time.LocalDate;

public record BankStatementPeriod(
    String accountAlias,
    String sourcePdf,
    LocalDate periodStart,
    LocalDate periodEnd,
    double openingBalance,
    double statementEndingBalance
) {
    public double calculatedEndingBalance(double netMovement) {
        return openingBalance + netMovement;
    }

    public boolean hasStatementEndingBalance() {
        return Math.abs(statementEndingBalance) > 0.0001;
    }
}
