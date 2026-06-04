package com.silveira.accounting.parsers;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.utils.Money;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreditCardStatementParser {
    private static final Pattern MONEY = Pattern.compile("\\$?\\(?[0-9,]+(?:\\.\\d{2})?\\)?");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter NUMERIC_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_NUMERIC_DATE = DateTimeFormatter.ofPattern("MM/dd/yy", Locale.ENGLISH);

    public ParsedCreditCardStatement parse(String text, String sourcePdf, String importStatus) {
        CreditCardStatement statement = new CreditCardStatement();
        statement.setSourcePdfPath(sourcePdf);
        statement.setImportStatus(importStatus);
        statement.setBankName(detectBank(text));
        statement.setCardName(detectCardName(text));
        statement.setAccountLastDigits(match(text, "ending\\s+in\\s+(\\d{4})", 1, ""));
        statement.setAccountAlias(buildAlias(statement));
        statement.setStatementStartDate(parseDateAny(firstNonBlank(
            match(text, "([A-Z][a-z]{2}\\s+\\d{1,2},\\s+20\\d{2})\\s+-\\s+[A-Z][a-z]{2}\\s+\\d{1,2},\\s+20\\d{2}", 1, null),
            match(text, "(?:Open to Close Date|Account Summary)\\s*:?\\s*(\\d{2}/\\d{2}/20\\d{2})\\s*-\\s*\\d{2}/\\d{2}/20\\d{2}", 1, null)
        )));
        statement.setStatementEndDate(parseDateAny(firstNonBlank(
            match(text, "[A-Z][a-z]{2}\\s+\\d{1,2},\\s+20\\d{2}\\s+-\\s+([A-Z][a-z]{2}\\s+\\d{1,2},\\s+20\\d{2})", 1, null),
            match(text, "(?:Open to Close Date|Account Summary)\\s*:?\\s*\\d{2}/\\d{2}/20\\d{2}\\s*-\\s*(\\d{2}/\\d{2}/20\\d{2})", 1, null)
        )));
        statement.setPaymentDueDate(parseDateAny(dateAfterLabel(text, "Payment Due Date")));
        statement.setNextClosingDate(parseDate(match(text, "Upcoming statement closing date:\\s*([A-Z][a-z]{2}\\s+\\d{1,2},\\s+20\\d{2})", 1, null)));
        statement.setPreviousBalance(amountAfter(text, "Previous Balance"));
        statement.setPayments(Math.abs(amountAfterAny(text, "Payments and Credits", "Payments")));
        statement.setOtherCredits(Math.abs(amountAfter(text, "Other Credits")));
        statement.setTransactions(amountAfterAny(text, "Transactions", "Purchases"));
        statement.setBalanceTransfers(amountAfter(text, "Balance Transfers"));
        statement.setCashAdvances(amountAfter(text, "Cash Advances"));
        statement.setFeesCharged(amountAfter(text, "Fees Charged"));
        statement.setInterestCharged(amountAfter(text, "Interest Charged"));
        statement.setNewBalance(paymentCouponAmount(text, 0, lastAmountAfter(text, "New Balance")));
        statement.setMinimumPaymentDue(minimumPaymentDue(text, statement.getNewBalance()));
        statement.setCreditLimit(amountAfterExactLine(text, "Credit Limit", amountAfterAny(text, "Credit Limit", "Credit Line")));
        statement.setAvailableCredit(amountAfterExactLine(text, "Available Credit (as of", amountAfterAny(text, "Available Credit", "Credit Line Available")));
        statement.setCashAdvanceLimit(amountAfterExactLine(text, "Cash Advance Credit Limit", amountAfterAny(text, "Cash Advance Credit Limit", "Cash Advance Credit Line")));
        statement.setAvailableCashAdvanceCredit(amountAfterExactLine(text, "Available Credit for Cash Advances", amountAfterAny(text, "Available Credit for Cash Advances", "Cash Advance Credit Line Available")));
        statement.setRewardsBalance(rewardsBalance(text));
        statement.setRewardsPreviousBalance(rewardAmount(text, "Previous Balance", "Balance anterior"));
        statement.setRewardsEarned(rewardAmount(text, "Earned This Period", "Ganado este periodo"));
        statement.setRewardsRedeemed(rewardAmount(text, "Redeemed This Period", "Canjeado este periodo"));
        if ("Discover".equalsIgnoreCase(statement.getBankName())) {
            applyDiscoverSummary(text, statement);
        }
        if ("Citi".equalsIgnoreCase(statement.getBankName())) {
            applyCitiSummary(text, statement);
        }
        if ("Best Buy".equalsIgnoreCase(statement.getBankName())) {
            applyBestBuySummary(text, statement);
        }
        List<CreditCardTransaction> parsedTransactions = "Best Buy".equalsIgnoreCase(statement.getBankName())
            ? parseBestBuyTransactions(text, statement.getStatementEndDate())
            : parseTransactions(text, statement.getStatementEndDate());
        if ("Discover".equalsIgnoreCase(statement.getBankName())) {
            double paymentsFromRows = parsedTransactions.stream()
                .filter(transaction -> "pago".equalsIgnoreCase(transaction.getType()))
                .mapToDouble(transaction -> Math.abs(transaction.getAmount()))
                .sum();
            if (paymentsFromRows > 0) {
                statement.setPayments(paymentsFromRows);
            }
        }
        return new ParsedCreditCardStatement(statement, parsedTransactions);
    }

    private void applyCitiSummary(String text, CreditCardStatement statement) {
        String periodStart = match(text, "Billing\\s+Period:\\s*(\\d{2}/\\d{2}/\\d{2})\\s*-\\s*\\d{2}/\\d{2}/\\d{2}", 1, null);
        String periodEnd = match(text, "Billing\\s+Period:\\s*\\d{2}/\\d{2}/\\d{2}\\s*-\\s*(\\d{2}/\\d{2}/\\d{2})", 1, null);
        statement.setStatementStartDate(parseShortNumericDate(periodStart));
        statement.setStatementEndDate(parseShortNumericDate(periodEnd));
        statement.setPaymentDueDate(parseShortNumericDate(match(text, "Payment\\s+due\\s+date:\\s*(\\d{2}/\\d{2}/\\d{2})", 1, null)));
        statement.setNewBalance(citiAmount(text, "New balance as of\\s+\\d{2}/\\d{2}/\\d{2}:\\s*([+-]?\\$?[0-9,]+(?:\\.\\d{2})?)", statement.getNewBalance()));
        statement.setMinimumPaymentDue(citiAmount(text, "Minimum payment due:\\s*([+-]?\\$?[0-9,]+(?:\\.\\d{2})?)", statement.getMinimumPaymentDue()));
        statement.setPreviousBalance(citiSummaryAmount(text, "Previous balance", statement.getPreviousBalance()));
        statement.setPayments(citiSummaryAmount(text, "Payments", statement.getPayments()));
        statement.setOtherCredits(citiSummaryAmount(text, "Credits", statement.getOtherCredits()));
        statement.setTransactions(citiSummaryAmount(text, "Purchases", statement.getTransactions()));
        statement.setCashAdvances(citiSummaryAmount(text, "Cash advances", statement.getCashAdvances()));
        statement.setFeesCharged(citiSummaryAmount(text, "Fees", statement.getFeesCharged()));
        statement.setInterestCharged(citiSummaryAmount(text, "Interest", statement.getInterestCharged()));
        statement.setCreditLimit(citiSummaryAmount(text, "Credit limit", citiSummaryAmount(text, "Credit Limit", statement.getCreditLimit())));
        statement.setAvailableCredit(citiSummaryAmount(text, "Available credit", citiSummaryAmount(text, "Available Credit Limit", statement.getAvailableCredit())));
        statement.setCashAdvanceLimit(citiCashAdvanceLimit(text, statement.getCashAdvanceLimit()));
        statement.setAvailableCashAdvanceCredit(citiAvailableCashAdvance(text, statement.getAvailableCashAdvanceCredit()));
    }

    private void applyBestBuySummary(String text, CreditCardStatement statement) {
        LocalDate closingDate = parseDateAny(match(text, "Statement\\s+Closing\\s+Date\\s+(\\d{2}/\\d{2}/20\\d{2})", 1, null));
        statement.setStatementEndDate(closingDate);
        statement.setNextClosingDate(parseDateAny(match(text, "Next\\s+Statement\\s+Closing\\s+Date\\s+(\\d{2}/\\d{2}/20\\d{2})", 1, null)));
        if (closingDate != null) {
            int days = (int) citiAmount(text, "Days\\s+in\\s+Billing\\s+Cycle\\s+([0-9]+)", 0);
            if (days > 0) {
                statement.setStatementStartDate(closingDate.minusDays(days - 1L));
            }
        }
        statement.setPaymentDueDate(parseDateAny(match(text, "Payment\\s+Due\\s+Date\\s+([A-Z][a-z]+\\s+\\d{1,2},\\s+20\\d{2})", 1, null)));
        statement.setPreviousBalance(citiSummaryAmount(text, "Previous Balance", statement.getPreviousBalance()));
        statement.setPayments(citiSummaryAmount(text, "Payments", statement.getPayments()));
        statement.setOtherCredits(citiSummaryAmount(text, "Other Credits", statement.getOtherCredits()));
        statement.setTransactions(citiSummaryAmount(text, "Purchases/Other Debits", statement.getTransactions()));
        statement.setCashAdvances(citiSummaryAmount(text, "Cash Advances", statement.getCashAdvances()));
        statement.setFeesCharged(citiSummaryAmount(text, "Fees Charged", statement.getFeesCharged()));
        statement.setInterestCharged(citiSummaryAmount(text, "Interest Charged", statement.getInterestCharged()));
        statement.setNewBalance(citiSummaryAmount(text, "New Balance", statement.getNewBalance()));
        statement.setMinimumPaymentDue(citiSummaryAmount(text, "Minimum Payment Due", statement.getMinimumPaymentDue()));
        statement.setCreditLimit(citiSummaryAmount(text, "Credit Limit", statement.getCreditLimit()));
        statement.setAvailableCredit(citiSummaryAmount(text, "Available Credit", statement.getAvailableCredit()));
    }

    private double citiAmount(String text, String regex, double fallback) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? Math.abs(Money.parse(matcher.group(1))) : fallback;
    }

    private double citiSummaryAmount(String text, String label, double fallback) {
        Matcher matcher = Pattern.compile("(?im)\\b" + Pattern.quote(label) + "\\s+([+-]?\\$?[0-9,]+(?:\\.\\d{2})?)").matcher(text);
        return matcher.find() ? Math.abs(Money.parse(matcher.group(1))) : fallback;
    }

    private double citiCashAdvanceLimit(String text, double fallback) {
        Matcher matcher = Pattern.compile("Includes\\s+([+-]?\\$?[0-9,]+(?:\\.\\d{2})?)\\s+cash advance limit", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? Math.abs(Money.parse(matcher.group(1))) : fallback;
    }

    private double citiAvailableCashAdvance(String text, double fallback) {
        Matcher matcher = Pattern.compile("Includes\\s+([+-]?\\$?[0-9,]+(?:\\.\\d{2})?)\\s+available for cash advances?", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? Math.abs(Money.parse(matcher.group(1))) : fallback;
    }

    private void applyDiscoverSummary(String text, CreditCardStatement statement) {
        statement.setPreviousBalance(discoverAmountAfterLine(text, "Previous Balance", statement.getPreviousBalance()));
        statement.setPayments(discoverPaymentsAndCredits(text, statement.getPayments()));
        statement.setTransactions(discoverAmountAfterLine(text, "Purchases", statement.getTransactions()));
        statement.setBalanceTransfers(discoverAmountAfterLine(text, "Balance Transfers", statement.getBalanceTransfers()));
        statement.setCashAdvances(discoverAmountAfterLine(text, "Cash Advances", statement.getCashAdvances()));
        statement.setFeesCharged(discoverAmountAfterLine(text, "Fees Charged", statement.getFeesCharged()));
        statement.setInterestCharged(discoverAmountAfterLine(text, "Interest Charged", statement.getInterestCharged()));
        statement.setNewBalance(discoverNewBalance(text, statement.getNewBalance()));
        statement.setMinimumPaymentDue(discoverMinimumPaymentDue(text, statement.getMinimumPaymentDue(), statement.getNewBalance()));
        statement.setCreditLimit(discoverAmountAfterLine(text, "Credit Line", statement.getCreditLimit()));
        statement.setAvailableCredit(discoverAmountAfterLine(text, "Credit Line Available", statement.getAvailableCredit()));
        statement.setCashAdvanceLimit(discoverAmountAfterLine(text, "Cash Advance Credit Line", statement.getCashAdvanceLimit()));
        statement.setAvailableCashAdvanceCredit(discoverAmountAfterLine(text, "Cash Advance Credit Line Available", statement.getAvailableCashAdvanceCredit()));
    }

    private List<CreditCardTransaction> parseTransactions(String text, LocalDate statementEndDate) {
        List<CreditCardTransaction> transactions = new ArrayList<>();
        int year = statementEndDate == null ? LocalDate.now().getYear() : statementEndDate.getYear();
        Pattern row = Pattern.compile("^(\\d{1,2}/\\d{1,2})\\s+(\\d{1,2}/\\d{1,2})\\s+(.+?)\\s+([+-]?\\$?[0-9,]+(?:\\.\\d{2})?)$", Pattern.CASE_INSENSITIVE);
        Pattern discoverRow = Pattern.compile("^[^0-9]*(\\d{1,2}/\\d{1,2})\\s+(.+?)\\s+([+-]?\\$?[0-9,]+(?:\\.\\d{2})?)$", Pattern.CASE_INSENSITIVE);
        for (String raw : text.split("\\R")) {
            String line = raw.replaceAll("\\s+", " ").trim();
            Matcher matcher = row.matcher(line);
            if (matcher.matches()) {
                double amount = Money.parse(matcher.group(4));
                String description = matcher.group(3).trim();
                transactions.add(new CreditCardTransaction(0, 0, parseMonthDay(matcher.group(1), year), parseMonthDay(matcher.group(2), year),
                    description, amount, classify(description, amount), ""));
                continue;
            }
            Matcher discoverMatcher = discoverRow.matcher(line);
            if (discoverMatcher.matches() && looksLikeCardTransaction(discoverMatcher.group(2))) {
                double amount = Money.parse(discoverMatcher.group(3));
                String description = discoverMatcher.group(2).trim();
                transactions.add(new CreditCardTransaction(0, 0, parseMonthDay(discoverMatcher.group(1), year), parseMonthDay(discoverMatcher.group(1), year),
                    description, amount, classify(description, amount), ""));
            }
        }
        return transactions;
    }

    private List<CreditCardTransaction> parseBestBuyTransactions(String text, LocalDate statementEndDate) {
        List<CreditCardTransaction> transactions = new ArrayList<>();
        int year = statementEndDate == null ? LocalDate.now().getYear() : statementEndDate.getYear();
        Pattern row = Pattern.compile("^(\\d{2}/\\d{2})\\s+(.+?)\\s+([A-Z0-9]{8,})\\s+\\$?\\s*([0-9,]+(?:\\.\\d{2})?)(-?)$", Pattern.CASE_INSENSITIVE);
        for (String raw : text.split("\\R")) {
            String line = raw.replaceAll("\\s+", " ").trim();
            Matcher matcher = row.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String description = matcher.group(2).trim();
            double amount = Money.parse(matcher.group(4));
            if ("-".equals(matcher.group(5))) {
                amount = -Math.abs(amount);
            }
            transactions.add(new CreditCardTransaction(0, 0, parseMonthDay(matcher.group(1), year), parseMonthDay(matcher.group(1), year),
                description, amount, classify(description, amount), ""));
        }
        return transactions;
    }

    private boolean looksLikeCardTransaction(String description) {
        String value = description == null ? "" : description.toLowerCase(Locale.ROOT);
        return value.contains("payment")
            || value.contains("purchase")
            || value.contains("credit")
            || value.contains("fee")
            || value.contains("interest")
            || value.contains("cash advance")
            || value.contains("thank you")
            || value.matches(".*[a-z]{4,}.*");
    }

    private double discoverPaymentsAndCredits(String text, double fallback) {
        List<String> lines = normalizedLines(text);
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).equalsIgnoreCase("Payments and Credits")) {
                continue;
            }
            for (int j = i - 1; j >= Math.max(0, i - 4); j--) {
                Double amount = firstMoney(lines.get(j));
                if (amount != null && amount < 0) {
                    return Math.abs(amount);
                }
            }
            return discoverAmountAfterLine(text, "Payments and Credits", fallback);
        }
        return fallback;
    }

    private double discoverNewBalance(String text, double fallback) {
        Matcher labeled = Pattern.compile("New Balance:\\s*(" + MONEY.pattern() + ")(?!\\s*%)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (labeled.find()) {
            return Math.abs(Money.parse(labeled.group(1)));
        }
        Matcher coupon = Pattern.compile("Payment Coupon[\\s\\S]{0,450}?New Balance\\s*(" + MONEY.pattern() + ")(?!\\s*%)", Pattern.CASE_INSENSITIVE).matcher(text);
        return coupon.find() ? Math.abs(Money.parse(coupon.group(1))) : fallback;
    }

    private double discoverMinimumPaymentDue(String text, double fallback, double newBalance) {
        Matcher coupon = Pattern.compile("Payment Coupon[\\s\\S]{0,650}?Minimum Payment Due\\s*(" + MONEY.pattern() + ")(?!\\s*%)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (coupon.find()) {
            return Math.abs(Money.parse(coupon.group(1)));
        }
        List<String> lines = normalizedLines(text);
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).equalsIgnoreCase("Minimum Payment Due")) {
                continue;
            }
            for (int j = i; j < Math.min(lines.size(), i + 4); j++) {
                Double amount = firstMoney(lines.get(j));
                if (amount != null && amount > 0 && Math.abs(amount - newBalance) > 0.009) {
                    return Math.abs(amount);
                }
            }
            for (int j = i - 1; j >= Math.max(0, i - 4); j--) {
                Double amount = firstMoney(lines.get(j));
                if (amount != null && Math.abs(amount) > 0 && Math.abs(Math.abs(amount) - newBalance) > 0.009) {
                    return Math.abs(amount);
                }
            }
        }
        return fallback;
    }

    private double discoverAmountAfterLine(String text, String label, double fallback) {
        List<String> lines = normalizedLines(text);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.equalsIgnoreCase(label)) {
                continue;
            }
            Double sameLine = firstMoney(line.substring(Math.min(line.length(), label.length())));
            if (sameLine != null) {
                return Math.abs(sameLine);
            }
            for (int j = i + 1; j < Math.min(lines.size(), i + 6); j++) {
                if (looksLikeDiscoverLabel(lines.get(j))) {
                    break;
                }
                Double amount = firstMoney(lines.get(j));
                if (amount != null) {
                    return Math.abs(amount);
                }
            }
        }
        return fallback;
    }

    private List<String> normalizedLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.replaceAll("\\s+", " ").trim();
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private Double firstMoney(String line) {
        Matcher matcher = MONEY.matcher(line);
        while (matcher.find()) {
            String token = matcher.group();
            int end = matcher.end();
            if (end < line.length() && line.substring(end).trim().startsWith("%")) {
                continue;
            }
            if (!token.contains("$") && !token.contains(".")) {
                continue;
            }
            double value = Money.parse(token);
            int start = matcher.start();
            int before = start - 1;
            while (before >= 0 && Character.isWhitespace(line.charAt(before))) {
                before--;
            }
            if (before >= 0 && line.charAt(before) == '-') {
                value = -Math.abs(value);
            }
            return value;
        }
        return null;
    }

    private boolean looksLikeDiscoverLabel(String line) {
        String value = line.toLowerCase(Locale.ROOT);
        return value.equals("previous balance")
            || value.equals("payments and credits")
            || value.equals("purchases")
            || value.equals("balance transfers")
            || value.equals("cash advances")
            || value.equals("fees charged")
            || value.equals("interest charged")
            || value.equals("credit line")
            || value.equals("credit line available")
            || value.equals("cash advance credit line")
            || value.equals("cash advance credit line available");
    }

    private String classify(String description, double amount) {
        String value = description.toLowerCase(Locale.ROOT);
        if (value.contains("payment")) return "pago";
        if (value.contains("interest")) return "interes";
        if (value.contains("fee")) return "fee";
        if (value.contains("cash advance")) return "cash advance";
        if (amount < 0) return "credito";
        return "compra";
    }

    private double amountAfter(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*(?:[+=-]\\s*)?(" + MONEY.pattern() + ")(?!\\s*%)", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? Math.abs(Money.parse(matcher.group(1))) : 0;
    }

    private double amountAfterAny(String text, String... labels) {
        for (String label : labels) {
            double amount = amountAfter(text, label);
            if (amount > 0) {
                return amount;
            }
        }
        return 0;
    }

    private double lastAmountAfter(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*(?:[+=-]\\s*)?(" + MONEY.pattern() + ")(?!\\s*%)", Pattern.CASE_INSENSITIVE).matcher(text);
        double value = 0;
        while (matcher.find()) {
            value = Math.abs(Money.parse(matcher.group(1)));
        }
        return value;
    }

    private double paymentCouponAmount(String text, int index, double fallback) {
        int marker = text.toLowerCase(Locale.ROOT).lastIndexOf("amount enclosed");
        if (marker < 0) {
            return fallback;
        }
        String slice = text.substring(marker, Math.min(text.length(), marker + 500));
        Matcher matcher = MONEY.matcher(slice);
        List<Double> amounts = new ArrayList<>();
        while (matcher.find()) {
            amounts.add(Math.abs(Money.parse(matcher.group())));
        }
        return amounts.size() > index ? amounts.get(index) : fallback;
    }

    private double minimumPaymentDue(String text, double newBalance) {
        Matcher coupon = Pattern.compile(
            "New Balance\\s+Minimum Payment Due\\s+Amount Enclosed[\\s\\S]{0,500}?(" + MONEY.pattern() + ")\\s+(" + MONEY.pattern() + ")",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (coupon.find()) {
            return Math.abs(Money.parse(coupon.group(2)));
        }

        double couponFallback = paymentCouponAmount(text, 1, 0);
        if (couponFallback > 0 && Math.abs(couponFallback - newBalance) > 0.009) {
            return couponFallback;
        }

        Matcher direct = Pattern.compile("Minimum Payment Due\\s+(" + MONEY.pattern() + ")", Pattern.CASE_INSENSITIVE).matcher(text);
        if (direct.find()) {
            return Math.abs(Money.parse(direct.group(1)));
        }

        Matcher dateMarker = Pattern.compile("\\d{2}/\\d{2}/20\\d{2}").matcher(text);
        while (dateMarker.find()) {
            int start = Math.max(0, dateMarker.start() - 180);
            String beforeDate = text.substring(start, dateMarker.start());
            if (!beforeDate.toLowerCase(Locale.ROOT).contains("minimum payment")) {
                continue;
            }
            Matcher amount = MONEY.matcher(beforeDate);
            double candidate = 0;
            while (amount.find()) {
                if (!amount.group().contains("$") && !amount.group().contains(".")) {
                    continue;
                }
                double value = Math.abs(Money.parse(amount.group()));
                if (value > 0 && Math.abs(value - newBalance) > 0.009) {
                    candidate = value;
                }
            }
            if (candidate > 0) {
                return candidate;
            }
        }

        int marker = text.toLowerCase(Locale.ROOT).indexOf("minimum payment due");
        if (marker < 0) {
            return 0;
        }
        String slice = text.substring(marker, Math.min(text.length(), marker + 450));
        Matcher matcher = MONEY.matcher(slice);
        while (matcher.find()) {
            double value = Math.abs(Money.parse(matcher.group()));
            if (value > 0 && Math.abs(value - newBalance) > 0.009) {
                return value;
            }
        }
        return 0;
    }

    private double amountAfterExactLine(String text, String labelPrefix, double fallback) {
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.equalsIgnoreCase(labelPrefix) && !line.toLowerCase(Locale.ROOT).startsWith(labelPrefix.toLowerCase(Locale.ROOT) + " ")) {
                continue;
            }
            for (int j = i + 1; j < Math.min(lines.length, i + 8); j++) {
                Matcher matcher = MONEY.matcher(lines[j]);
                if (matcher.find()) {
                    return Math.abs(Money.parse(matcher.group()));
                }
            }
        }
        return fallback;
    }

    private double rewardsBalance(String text) {
        String[] labels = {
            "Rewards Balance",
            "Rewards Cash Balance",
            "Cash Rewards Balance",
            "Available Rewards",
            "Rewards Available",
            "Reward Balance"
        };
        for (String label : labels) {
            double value = amountAfterExactLine(text, label, amountAfter(text, label));
            if (value > 0) {
                return value;
            }
        }
        Matcher matcher = Pattern.compile("(?:rewards|cash rewards|recompensas)[\\s\\S]{0,160}?(" + MONEY.pattern() + ")", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? Math.abs(Money.parse(matcher.group(1))) : 0;
    }

    private double rewardAmount(String text, String englishLabel, String spanishLabel) {
        String[] labels = {englishLabel, spanishLabel};
        for (String label : labels) {
            Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*(?:[+=-]\\s*)?(" + MONEY.pattern() + ")", Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                return Math.abs(Money.parse(matcher.group(1)));
            }
        }
        Matcher table = Pattern.compile(
            "Balance\\s+anterior\\s+Ganado\\s+este\\s+periodo\\s+Canjeado\\s+este\\s+periodo[\\s\\S]{0,220}?(" + MONEY.pattern() + ")\\s+(" + MONEY.pattern() + ")\\s+[-+]?\\s*(" + MONEY.pattern() + ")",
            Pattern.CASE_INSENSITIVE).matcher(text);
        if (table.find()) {
            if ("Previous Balance".equals(englishLabel)) return Math.abs(Money.parse(table.group(1)));
            if ("Earned This Period".equals(englishLabel)) return Math.abs(Money.parse(table.group(2)));
            return Math.abs(Money.parse(table.group(3)));
        }
        return 0;
    }

    private String dateAfterLabel(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + ":?[\\s\\S]{0,450}?([A-Z][a-z]{2}\\s+\\d{1,2},\\s+20\\d{2}|\\d{2}/\\d{2}/20\\d{2})", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String detectBank(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("my best buy") || lower.contains("best buy credit services") || lower.contains("bestbuy.accountonline.com")) {
            return "Best Buy";
        }
        if (lower.contains("citicards.com") || lower.contains("citi double cash") || lower.contains("citi simplicity") || lower.contains("citi cards")) {
            return "Citi";
        }
        if (lower.contains("discover it") || lower.contains("discover") || lower.contains("discover card") || lower.contains("card ending in")) {
            return "Discover";
        }
        return lower.contains("capital one") || lower.contains("capitalone") ? "Capital One" : "Tarjeta";
    }

    private String detectCardName(String text) {
        Matcher matcher = Pattern.compile("(.+?Credit Card.+?ending in\\s+\\d{4})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").trim();
        }
        Matcher discover = Pattern.compile("(Discover\\s+It.+?Card\\s+Ending\\s+In\\s+\\d{4})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (discover.find()) {
            return discover.group(1).replaceAll("\\s+", " ").trim();
        }
        Matcher bestBuy = Pattern.compile("(My\\s+Best\\s+Buy\\s+Credit\\s+Card)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (bestBuy.find()) {
            return bestBuy.group(1).replaceAll("\\s+", " ").trim();
        }
        Matcher citi = Pattern.compile("(Citi\\s+.+?Card)", Pattern.CASE_INSENSITIVE).matcher(text);
        return citi.find() ? citi.group(1).replaceAll("\\s+", " ").trim() : "Credit Card";
    }

    private String buildAlias(CreditCardStatement statement) {
        String digits = statement.getAccountLastDigits() == null || statement.getAccountLastDigits().isBlank() ? "sin_numero" : statement.getAccountLastDigits();
        return "tarjeta_" + digits;
    }

    private String match(String text, String regex, int group, String fallback) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(group).trim() : fallback;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.replaceAll("\\s+", " "), DATE);
    }

    private LocalDate parseDateAny(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.matches("\\d{2}/\\d{2}/20\\d{2}")) {
            return LocalDate.parse(normalized, NUMERIC_DATE);
        }
        return LocalDate.parse(normalized, DATE);
    }

    private LocalDate parseShortNumericDate(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.replaceAll("\\s+", " ").trim(), SHORT_NUMERIC_DATE);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private LocalDate parseMonthDay(String value, int year) {
        String[] parts = value.split("/");
        return LocalDate.of(year, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public record ParsedCreditCardStatement(CreditCardStatement statement, List<CreditCardTransaction> transactions) {
    }
}
