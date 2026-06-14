package com.silveira.accounting.application.card.dto;

public record CardAnalysisSummary(
    double closingDebt,
    double payments,
    double purchases,
    double interest
) {
}
