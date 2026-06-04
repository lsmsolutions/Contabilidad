package com.silveira.accounting.models;

public record SourceTotals(
    int reviewedCount,
    int pendingCount,
    double income,
    double expenses,
    double net
) {
    public static SourceTotals of(int reviewedCount, int pendingCount, double income, double expenses) {
        return new SourceTotals(reviewedCount, pendingCount, income, expenses, income + expenses);
    }
}
