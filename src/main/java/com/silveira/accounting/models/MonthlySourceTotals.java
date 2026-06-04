package com.silveira.accounting.models;

public record MonthlySourceTotals(
    int year,
    int month,
    int reviewedCount,
    int pendingCount,
    double credits,
    double deductions,
    double net
) {
}
