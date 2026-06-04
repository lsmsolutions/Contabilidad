package com.silveira.accounting.models;

import java.util.List;

public record MortgageAnalysis(
    int daysUntilDue,
    String paymentStatus,
    double principalPercent,
    double interestPercent,
    double escrowPercent,
    double debtReductionPercent,
    double interestPrincipalRatio,
    List<MortgageAlert> alerts
) {
}
