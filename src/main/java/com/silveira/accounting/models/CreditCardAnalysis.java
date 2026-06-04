package com.silveira.accounting.models;

import java.util.List;

public record CreditCardAnalysis(
    double calculatedBalance,
    double balanceDifference,
    double creditUtilizationPercent,
    double interestOnPurchasesPercent,
    long daysUntilDue,
    String paymentStatus,
    String riskLevel,
    double estimatedLateFee,
    List<FinancialAlert> alerts
) {
}
