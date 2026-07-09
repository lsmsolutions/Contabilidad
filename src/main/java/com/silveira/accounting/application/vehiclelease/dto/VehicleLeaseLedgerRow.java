package com.silveira.accounting.application.vehiclelease.dto;

import java.time.LocalDate;

public record VehicleLeaseLedgerRow(
    int month,
    LocalDate statementDate,
    LocalDate dueDate,
    double totalDue,
    int paymentsMade,
    int paymentsRemaining,
    boolean hasStatement
) {
}
