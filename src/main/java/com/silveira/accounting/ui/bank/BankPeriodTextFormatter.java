package com.silveira.accounting.ui.bank;

import com.silveira.accounting.models.bank.BankStatementPeriod;
import java.time.LocalDate;

public class BankPeriodTextFormatter {
    private BankPeriodTextFormatter() {
    }

    public static String title(BankStatementPeriod period) {
        if (period.periodStart().equals(period.periodEnd())) {
            return shortDate(period.periodStart());
        }
        return shortDate(period.periodStart()) + " - " + shortDate(period.periodEnd());
    }

    public static String shortDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
