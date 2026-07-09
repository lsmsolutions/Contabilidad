package com.silveira.accounting.application.mortgage.dto;

import java.util.List;

public record MortgageLedgerSnapshot(
    String selectedLoan,
    int year,
    List<String> loans,
    List<Integer> availableYears,
    double initialDebt,
    List<MortgageLedgerRow> rows
) {
    public double totalDebtPaid() {
        return rows.stream().mapToDouble(MortgageLedgerRow::debtPaid).sum();
    }

    public double endingOutstandingDebt() {
        return rows.stream()
            .filter(MortgageLedgerRow::hasStatement)
            .reduce((previous, current) -> current)
            .map(MortgageLedgerRow::outstandingDebt)
            .orElse(0.0);
    }

    public double totalInterest() {
        return rows.stream().mapToDouble(MortgageLedgerRow::interest).sum();
    }

    public double totalEscrow() {
        return rows.stream().mapToDouble(MortgageLedgerRow::escrow).sum();
    }

    public double totalPaidIndicators() {
        return totalDebtPaid() + totalInterest() + totalEscrow();
    }
}
