package com.silveira.accounting.application.vehiclelease.dto;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import java.util.List;

public record VehicleLeaseLedgerSnapshot(
    String selectedAccount,
    int year,
    List<VehicleLeaseAccount> accounts,
    List<Integer> availableYears,
    List<VehicleLeaseLedgerRow> rows
) {
    public double totalDue() {
        return rows.stream().mapToDouble(VehicleLeaseLedgerRow::totalDue).sum();
    }

    public int latestPaymentsMade() {
        return rows.stream()
            .filter(VehicleLeaseLedgerRow::hasStatement)
            .reduce((previous, current) -> current)
            .map(VehicleLeaseLedgerRow::paymentsMade)
            .orElse(0);
    }

    public int latestPaymentsRemaining() {
        return rows.stream()
            .filter(VehicleLeaseLedgerRow::hasStatement)
            .reduce((previous, current) -> current)
            .map(VehicleLeaseLedgerRow::paymentsRemaining)
            .orElse(0);
    }
}
