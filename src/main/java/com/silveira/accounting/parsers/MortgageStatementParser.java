package com.silveira.accounting.parsers;

import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import com.silveira.accounting.utils.Money;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MortgageStatementParser {
    private static final Pattern MONEY = Pattern.compile("\\$?\\(?[0-9,]+\\.\\d{2}\\)?");
    private static final Pattern PERCENT = Pattern.compile("[0-9]+(?:\\.[0-9]+)?\\s*%");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

    public ParsedMortgageStatement parse(String text, String sourcePdf, String importStatus) {
        MortgageStatement statement = new MortgageStatement();
        statement.setSourcePdfPath(sourcePdf);
        statement.setImportStatus(importStatus);
        statement.setServicerName(detectServicer(text));
        statement.setStatementDate(dateAfter(text, "Statement Date"));
        statement.setPaymentDueDate(firstDateAfterAny(text, "Payment Due Date", "Due Date"));
        statement.setPaymentAmountDue(amountAfterAny(text, "Payment Amount Due", "Amount Due"));
        statement.setLateFeeDate(firstDateAfterAny(text, "Late Fee Date", "Late Charge Date"));
        statement.setLateFeeAmount(amountNear(text, "late fee", amountAfterAny(text, "Late Fee", "Late Charge")));
        statement.setLoanNumber(match(text, "(?:Loan|Account)\\s*(?:Number|No\\.?|#)\\s*:?\\s*([A-Z0-9\\-* ]{4,32})", 1, ""));
        statement.setPropertyAddress(extractPropertyAddress(text));
        statement.setOriginalPrincipalBalance(amountAfterAny(text, "Original Principal Balance", "Original Balance"));
        statement.setOutstandingPrincipalBalance(amountAfterAny(text, "Outstanding Principal Balance", "Principal Balance"));
        statement.setMaturityDate(firstDateAfterAny(text, "Maturity Date"));
        statement.setInterestRate(percentAfterAny(text, "Interest Rate", "Current Interest Rate"));
        statement.setEscrowBalance(amountAfterAny(text, "Escrow Balance"));
        statement.setUnappliedFunds(amountAfterAny(text, "Unapplied Funds", "Unapplied Balance"));
        statement.setCurrentPaymentDue(amountAfterAny(text, "Current Payment Due", "Current Due"));
        statement.setPrincipalDue(amountAfterAny(text, "Principal"));
        statement.setInterestDue(amountAfterAny(text, "Interest"));
        statement.setEscrowDue(amountAfterAny(text, "Escrow"));
        statement.setRegularMonthlyPayment(amountAfterAny(text, "Regular Monthly Payment", "Monthly Payment"));
        statement.setPastDueAmount(amountAfterAny(text, "Past Due Amounts", "Past Due Amount"));
        statement.setFees(amountAfterAny(text, "Fees", "Charges"));
        statement.setOtherFeesAndCharges(amountAfterAny(text, "Other Fees & Charges", "Other Fees and Charges"));
        statement.setTotalDue(amountAfterAny(text, "Total Due", "Total Amount Due"));
        applyPastPaymentSummary(text, statement);
        if (statement.getPaymentAmountDue() == 0) {
            statement.setPaymentAmountDue(statement.getTotalDue());
        }
        if (statement.getTotalDue() == 0) {
            statement.setTotalDue(statement.getPaymentAmountDue());
        }
        normalizePaymentBreakdown(statement);
        statement.setLoanAlias(buildAlias(statement, sourcePdf));
        statement.setReviewRequired(true);
        statement.setPendingReview(true);
        statement.setReviewNotes("Revisar contra el PDF original");
        return new ParsedMortgageStatement(statement, transactions(text, statement));
    }

    private void applyPastPaymentSummary(String text, MortgageStatement statement) {
        String section = sectionAfter(text, "Past Payment Summary", 900);
        double[] principal = twoAmountsAfter(section, "Principal");
        double[] interest = twoAmountsAfter(section, "Interest");
        double[] escrow = twoAmountsAfter(section, "Escrow");
        double[] total = twoAmountsAfter(section, "Total");
        statement.setPastPaidPrincipalSinceLastStatement(principal[0]);
        statement.setPastPaidPrincipalYearToDate(principal[1]);
        statement.setPastPaidInterestSinceLastStatement(interest[0]);
        statement.setPastPaidInterestYearToDate(interest[1]);
        statement.setPastPaidEscrowSinceLastStatement(escrow[0]);
        statement.setPastPaidEscrowYearToDate(escrow[1]);
        statement.setPastPaidTotalSinceLastStatement(total[0]);
        statement.setPastPaidTotalYearToDate(total[1]);
    }

    private String sectionAfter(String text, String label, int maxChars) {
        int start = text.toLowerCase(Locale.ROOT).indexOf(label.toLowerCase(Locale.ROOT));
        if (start < 0) {
            return "";
        }
        int end = Math.min(text.length(), start + maxChars);
        return text.substring(start, end);
    }

    private double[] twoAmountsAfter(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "(?:\\s*\\([^)]*\\))?[\\s\\S]{0,160}?(" + MONEY.pattern() + ")[\\s\\S]{0,80}?(" + MONEY.pattern() + ")", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return new double[] { Math.abs(amount(matcher.group(1))), Math.abs(amount(matcher.group(2))) };
        }
        return new double[] { 0, 0 };
    }

    private void normalizePaymentBreakdown(MortgageStatement statement) {
        double total = statement.getTotalDue() > 0 ? statement.getTotalDue() : statement.getPaymentAmountDue();
        double fees = statement.getFees() + statement.getOtherFeesAndCharges() + statement.getPastDueAmount();
        double calculatedPrincipal = total - statement.getInterestDue() - statement.getEscrowDue() - fees;
        if (total > 0 && statement.getInterestDue() > 0 && statement.getEscrowDue() > 0 && calculatedPrincipal >= 0) {
            statement.setPrincipalDue(calculatedPrincipal);
        }
        if (statement.getCurrentPaymentDue() == 0) {
            statement.setCurrentPaymentDue(total);
        }
        if (statement.getRegularMonthlyPayment() == 0) {
            statement.setRegularMonthlyPayment(total);
        }
    }

    private List<MortgageTransaction> transactions(String text, MortgageStatement statement) {
        List<MortgageTransaction> rows = new ArrayList<>();
        int year = statement.getStatementDate() == null ? LocalDate.now().getYear() : statement.getStatementDate().getYear();
        Pattern rowPattern = Pattern.compile("^(\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?)\\s+(.+?)\\s+(" + MONEY.pattern() + "(?:\\s+" + MONEY.pattern() + "){0,7})\\s*$", Pattern.CASE_INSENSITIVE);
        for (String raw : text.split("\\R")) {
            String line = raw.replaceAll("\\s+", " ").trim();
            Matcher matcher = rowPattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String description = matcher.group(2).trim();
            if (description.length() < 3 || description.toLowerCase(Locale.ROOT).contains("payment due date")) {
                continue;
            }
            List<Double> amounts = amounts(matcher.group(3));
            if (amounts.isEmpty()) {
                continue;
            }
            rows.add(new MortgageTransaction(
                0,
                0,
                transactionDate(matcher.group(1), year),
                description,
                amountAt(amounts, 0),
                amountAt(amounts, 1),
                amountAt(amounts, 2),
                amountAt(amounts, 3),
                amountAt(amounts, 4),
                amountAt(amounts, 5),
                amountAt(amounts, 6),
                amountAt(amounts, 7)
            ));
        }
        rows.forEach(transaction -> {
            transaction.setReviewRequired(true);
            transaction.setPendingReview(true);
            transaction.setReviewNotes("Revisar contra el PDF original");
        });
        return rows;
    }

    private List<Double> amounts(String text) {
        List<Double> values = new ArrayList<>();
        Matcher matcher = MONEY.matcher(text);
        while (matcher.find()) {
            values.add(Math.abs(amount(matcher.group())));
        }
        return values;
    }

    private double amountAt(List<Double> amounts, int index) {
        return index < amounts.size() ? amounts.get(index) : 0;
    }

    private double amountAfterAny(String text, String... labels) {
        for (String label : labels) {
            double value = amountNear(text, label, 0);
            if (Math.abs(value) > 0.009) return value;
        }
        return 0;
    }

    private double amountNear(String text, String label, double fallback) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "[\\s\\S]{0,160}?(" + MONEY.pattern() + ")", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? Math.abs(amount(matcher.group(1))) : fallback;
    }

    private LocalDate firstDateAfterAny(String text, String... labels) {
        for (String label : labels) {
            LocalDate date = dateAfter(text, label);
            if (date != null) return date;
        }
        return null;
    }

    private LocalDate dateAfter(String text, String label) {
        Matcher longMatcher = Pattern.compile(Pattern.quote(label) + "[\\s\\S]{0,180}?([A-Z][a-z]+\\s+\\d{1,2},\\s+20\\d{2})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (longMatcher.find()) return parseDate(longMatcher.group(1));
        Matcher shortMatcher = Pattern.compile(Pattern.quote(label) + "[\\s\\S]{0,180}?(\\d{1,2}/\\d{1,2}/20\\d{2})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (shortMatcher.find()) return parseDate(shortMatcher.group(1));
        Matcher monthYear = Pattern.compile(Pattern.quote(label) + "[\\s\\S]{0,180}?(\\d{1,2}/20\\d{2})", Pattern.CASE_INSENSITIVE).matcher(text);
        return monthYear.find() ? parseDate("01/" + monthYear.group(1)) : null;
    }

    private double percentAfterAny(String text, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile(Pattern.quote(label) + "[\\s\\S]{0,120}?(" + PERCENT.pattern() + ")", Pattern.CASE_INSENSITIVE).matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1).replace("%", "").trim());
            }
        }
        return 0;
    }

    private String detectServicer(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("united wholesale mortgage") || lower.contains("uwm")) return "UWM";
        if (lower.contains("morgan")) return "Morgan";
        return "Hipoteca";
    }

    private String extractPropertyAddress(String text) {
        Matcher matcher = Pattern.compile("Property Address\\s*:?\\s*(.+?)(?:\\R|$)", Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String buildAlias(MortgageStatement statement, String sourcePdf) {
        String digits = statement.getLoanNumber() == null ? "" : statement.getLoanNumber().replaceAll("\\D", "");
        if (digits.length() >= 4) {
            return "hipoteca_" + digits.substring(digits.length() - 4);
        }
        String name = PathName.nameWithoutExtension(sourcePdf).replaceAll("[^A-Za-z0-9]+", "_").toLowerCase(Locale.ROOT);
        return name.isBlank() ? "hipoteca" : "hipoteca_" + name;
    }

    private String match(String text, String regex, int group, String fallback) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        return matcher.find() ? matcher.group(group).trim() : fallback;
    }

    private double amount(String raw) {
        return Money.parse(raw);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.contains("/")) return LocalDate.parse(normalized, SHORT_DATE);
        return LocalDate.parse(normalized, LONG_DATE);
    }

    private LocalDate transactionDate(String value, int year) {
        String[] parts = value.split("/");
        if (parts.length >= 3) {
            int parsedYear = Integer.parseInt(parts[2]);
            if (parsedYear < 100) {
                parsedYear += 2000;
            }
            return LocalDate.of(parsedYear, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return LocalDate.of(year, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private static final class PathName {
        private static String nameWithoutExtension(String value) {
            if (value == null || value.isBlank()) return "";
            String name = java.nio.file.Path.of(value).getFileName().toString();
            int dot = name.lastIndexOf('.');
            return dot > 0 ? name.substring(0, dot) : name;
        }
    }

    public record ParsedMortgageStatement(MortgageStatement statement, List<MortgageTransaction> transactions) {
    }
}
