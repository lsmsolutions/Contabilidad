package com.silveira.accounting.parsers;

import java.util.Locale;

public class ProviderDetector {
    public String detect(String description) {
        String value = description.toLowerCase(Locale.ROOT);
        if (value.contains("new york life") || value.contains("ny life") || value.contains("ledger payment")) {
            return "New York Life";
        }
        if (value.contains("trans life")) {
            return "Trans Life";
        }
        if (value.contains("national life")) {
            return "National Life";
        }
        if (value.contains("zelle")) {
            return "Zelle";
        }
        if (value.contains("irs") || value.contains("tax")) {
            return "IRS";
        }
        if (value.contains("fee") || value.contains("service charge")) {
            return "Fees";
        }
        if (value.contains("transfer") || value.contains("ach") || value.contains("wire")) {
            return "Transferencias";
        }
        return "Otros";
    }

    public String movementType(String description, double amount) {
        String value = description.toLowerCase(Locale.ROOT);
        if (value.contains("fee") || value.contains("service charge")) {
            return "Fee";
        }
        if (value.contains("card") || value.contains("purchase") || value.contains("debit")) {
            return "Pago con tarjeta";
        }
        if (amount >= 0) {
            return "Deposito";
        }
        if (value.contains("withdrawal") || value.contains("ach") || value.contains("wire") || value.contains("transfer")) {
            return "Retiro electronico";
        }
        return "Gasto";
    }
}
