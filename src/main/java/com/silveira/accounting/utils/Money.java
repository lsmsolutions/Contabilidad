package com.silveira.accounting.utils;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private Money() {
    }

    public static String format(double amount) {
        return FORMAT.format(amount);
    }

    public static double parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String normalized = raw.replace("$", "")
            .replace(",", "")
            .replace("(", "-")
            .replace(")", "")
            .replaceAll("[^0-9.\\-]", "")
            .trim();
        if (normalized.isBlank() || "-".equals(normalized) || ".".equals(normalized) || "-.".equals(normalized)) {
            return 0;
        }
        return Double.parseDouble(normalized);
    }
}
