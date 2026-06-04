package com.silveira.accounting.models;

public record MortgageAlert(
    long id,
    long statementId,
    String severity,
    String title,
    String message
) {
}
