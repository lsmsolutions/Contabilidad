package com.silveira.accounting.models.bank;

public record BankMonthlyClosing(
    String accountAlias,
    int year,
    int month,
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
