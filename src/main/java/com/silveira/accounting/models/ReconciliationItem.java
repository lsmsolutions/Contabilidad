package com.silveira.accounting.models;

public record ReconciliationItem(
    long bankId,
    long nylId,
    int year,
    int month,
    String bankDescription,
    String nylConcept,
    double bankAmount,
    double nylAmount,
    double difference,
    String status
) {
}
