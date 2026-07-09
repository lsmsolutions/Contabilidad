package com.silveira.accounting.application.bank.dto;

import java.util.List;

public record BankLedgerSnapshot(
    int year,
    int month,
    List<Integer> availableYears,
    List<BankLedgerRow> accountRows,
    List<BankLedgerRow> reyRows,
    List<BankLedgerRow> accumulatedAccountRows,
    List<BankLedgerRow> accumulatedReyRows
) {
    public BankLedgerRow accountTotals() {
        return totals("Total", accountRows);
    }

    public BankLedgerRow reyTotals() {
        return totals("Total Rey", reyRows);
    }

    public BankLedgerRow accumulatedAccountTotals() {
        return totals("Total", accumulatedAccountRows);
    }

    public BankLedgerRow accumulatedReyTotals() {
        return totals("Total Rey", accumulatedReyRows);
    }

    private BankLedgerRow totals(String label, List<BankLedgerRow> rows) {
        return new BankLedgerRow(
            label,
            "",
            "",
            rows.stream().mapToDouble(BankLedgerRow::deposits).sum(),
            rows.stream().mapToDouble(BankLedgerRow::withdrawals).sum(),
            rows.stream().mapToDouble(BankLedgerRow::calculatedBalance).sum()
        );
    }
}
