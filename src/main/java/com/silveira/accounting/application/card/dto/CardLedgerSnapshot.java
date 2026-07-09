package com.silveira.accounting.application.card.dto;

import java.util.List;

public record CardLedgerSnapshot(
    int year,
    int month,
    List<Integer> availableYears,
    List<CardLedgerRow> rows
) {
    public CardLedgerRow totals() {
        return new CardLedgerRow(
            "Total",
            "",
            "",
            rows.stream().mapToDouble(CardLedgerRow::creditLimit).sum(),
            rows.stream().mapToDouble(CardLedgerRow::usedBalance).sum(),
            rows.stream().mapToDouble(CardLedgerRow::availableCredit).sum(),
            rows.stream().mapToDouble(CardLedgerRow::interest).sum(),
            rows.stream().mapToDouble(CardLedgerRow::accumulatedInterest).sum()
        );
    }
}
