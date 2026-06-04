package com.silveira.accounting.models;

public record FinancialAlert(long id, long statementId, String severity, String title, String message) {
}
