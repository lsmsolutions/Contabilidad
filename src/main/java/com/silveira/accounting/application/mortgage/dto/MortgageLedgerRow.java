package com.silveira.accounting.application.mortgage.dto;

public record MortgageLedgerRow(
    int month,
    double debtPaid,
    double outstandingDebt,
    double interest,
    double escrow,
    boolean hasStatement
) {
}
