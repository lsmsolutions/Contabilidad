package com.silveira.accounting.application.bank.dto;

public record BankLedgerRow(
    String account,
    String bank,
    String ending,
    double deposits,
    double withdrawals,
    double calculatedBalance
) {
}
