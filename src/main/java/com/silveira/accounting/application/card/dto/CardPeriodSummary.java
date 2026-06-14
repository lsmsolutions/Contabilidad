package com.silveira.accounting.application.card.dto;

import com.silveira.accounting.models.CreditCardStatement;
import java.util.List;

public record CardPeriodSummary(
    int year,
    int month,
    String title,
    List<CreditCardStatement> statements,
    CreditCardStatement openingStatement,
    CreditCardStatement closingStatement,
    double payments,
    double purchases,
    double interest
) {
}
