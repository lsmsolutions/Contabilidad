package com.silveira.accounting.ui.bank;

import com.silveira.accounting.models.bank.BankAccount;

public final class BankAccountTextFormatter {
    private BankAccountTextFormatter() {
    }

    public static String pageTitle(BankAccount account) {
        String title = "Banco - " + account.getAlias();
        if (account.getBankName() != null && !account.getBankName().isBlank()) {
            title += " - " + account.getBankName();
        }
        if (account.getAccountNumber() != null && !account.getAccountNumber().isBlank()) {
            title += " - Cuenta: " + account.getAccountNumber();
        }
        return title;
    }

    public static String pageTitle(String accountAlias) {
        return "Banco - " + accountAlias;
    }

    public static String accountSuffix(BankAccount account) {
        String number = account.getAccountNumber();
        if (number == null || number.isBlank()) {
            return "";
        }
        String clean = number.replaceAll("\\D", "");
        if (clean.isBlank()) {
            return " (" + number + ")";
        }
        String last = clean.length() <= 4 ? clean : clean.substring(clean.length() - 4);
        return " (termina en " + last + ")";
    }
}
