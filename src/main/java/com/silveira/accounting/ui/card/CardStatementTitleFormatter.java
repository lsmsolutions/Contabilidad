package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CardStatementTitleFormatter {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String title(CreditCardStatement statement) {
        LocalDate start = statement.getStatementStartDate();
        LocalDate end = statement.getStatementEndDate();
        if (start != null && end != null) {
            return shortDate(start) + " - " + shortDate(end);
        }
        if (end != null) {
            return monthName(end.getMonthValue()) + " " + end.getYear();
        }
        return text(statement.getAccountAlias()).isBlank() ? "Resumen de tarjeta" : statement.getAccountAlias();
    }

    private String shortDate(LocalDate date) {
        return date.format(SHORT_DATE_FORMAT);
    }

    private String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> "Mes " + month;
        };
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
