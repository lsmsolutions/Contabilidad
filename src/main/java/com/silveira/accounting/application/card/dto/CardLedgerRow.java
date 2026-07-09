package com.silveira.accounting.application.card.dto;

public record CardLedgerRow(
    String account,
    String bank,
    String ending,
    double creditLimit,
    double usedBalance,
    double availableCredit,
    double interest,
    double accumulatedInterest
) {
}
