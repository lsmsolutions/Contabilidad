package com.silveira.accounting.models;

public record DashboardSummary(
    double bankIncome,
    double nylReceivedBank,
    double nylCredits,
    double nylDeductions,
    double nylNet,
    double bankExpenses,
    double pendingDifference
) {
}
