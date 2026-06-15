package com.silveira.accounting.application.card.dto;

public record CardActivityTotals(
    double payments,
    double purchases,
    double interest
) {}
