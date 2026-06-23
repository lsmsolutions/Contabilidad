package com.silveira.accounting.parsers.bank;

import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.parsers.ProviderDetector;
import com.silveira.accounting.utils.Fingerprint;
import com.silveira.accounting.utils.Money;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RevolutBankStatementParser {
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("Account Number\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATEMENT_TYPE = Pattern.compile("\\b([A-Z]{3})\\s+Statement\\b");
    private static final Pattern PERIOD = Pattern.compile(
        "Account transactions from\\s+([A-Za-z]+\\s+\\d{1,2},\\s+20\\d{2})\\s+to\\s+([A-Za-z]+\\s+\\d{1,2},\\s+20\\d{2})",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SUMMARY = Pattern.compile(
        "(?m)^Account\\s+([€$][0-9,]+\\.\\d{2})\\s+([€$][0-9,]+\\.\\d{2})\\s+([€$][0-9,]+\\.\\d{2})\\s+([€$][0-9,]+\\.\\d{2})\\s*$"
    );
    private static final Pattern TRANSACTION = Pattern.compile(
        "^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2}),\\s+(20\\d{2})\\s+(.+?)\\s+([€$][0-9,]+\\.\\d{2})\\s+([€$][0-9,]+\\.\\d{2})\\s*$"
    );
    private static final Pattern REFERENCE = Pattern.compile("^Reference:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private final PdfTextExtractor extractor = new PdfTextExtractor();
    private final ProviderDetector providerDetector = new ProviderDetector();

    public boolean isRevolut(Path pdf) {
        return isRevolut(extractor.extract(pdf));
    }

    public boolean isRevolut(String text) {
        return text != null && text.contains("Revolut Technologies Inc.") && text.contains("Statement");
    }

    public Optional<ParsedRevolutBankStatement> parseIfRevolut(Path pdf) {
        String text = extractor.extract(pdf);
        if (!isRevolut(text)) {
            return Optional.empty();
        }
        return Optional.of(parseText(text, pdf.getFileName().toString(), "importado_auto", false));
    }

    public ParsedRevolutBankStatement parseText(String text, String sourcePdf, String importStatus, boolean reviewRequired) {
        String currency = detectCurrency(text);
        String accountAlias = detectAccountAlias(text, currency);
        BankStatementPeriod period = detectPeriod(text, accountAlias, sourcePdf);
        List<BankTransaction> transactions = parseTransactions(text, accountAlias, sourcePdf, importStatus, reviewRequired, period.openingBalance());
        return new ParsedRevolutBankStatement(period, transactions);
    }

    private List<BankTransaction> parseTransactions(
        String text,
        String accountAlias,
        String sourcePdf,
        String importStatus,
        boolean reviewRequired,
        double openingBalance
    ) {
        List<BankTransaction> transactions = new ArrayList<>();
        Map<String, Integer> occurrenceByBaseFingerprint = new HashMap<>();
        double runningBalance = openingBalance;
        PendingTransaction pending = null;

        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            if (line.isBlank()) {
                continue;
            }
            Matcher transactionMatcher = TRANSACTION.matcher(line);
            if (transactionMatcher.matches()) {
                if (pending != null) {
                    transactions.add(createTransaction(pending, accountAlias, sourcePdf, importStatus, reviewRequired, occurrenceByBaseFingerprint));
                }
                LocalDate date = LocalDate.of(
                    Integer.parseInt(transactionMatcher.group(3)),
                    month(transactionMatcher.group(1)),
                    Integer.parseInt(transactionMatcher.group(2))
                );
                double magnitude = Money.parse(transactionMatcher.group(5));
                double balance = Money.parse(transactionMatcher.group(6));
                double amount = signedAmount(runningBalance, magnitude, balance);
                pending = new PendingTransaction(date, transactionMatcher.group(4).trim(), amount, "");
                runningBalance = balance;
                continue;
            }
            if (pending == null) {
                continue;
            }
            Matcher referenceMatcher = REFERENCE.matcher(line);
            if (referenceMatcher.matches()) {
                pending = new PendingTransaction(pending.date(), pending.description(), pending.amount(), referenceMatcher.group(1).trim());
            }
        }
        if (pending != null) {
            transactions.add(createTransaction(pending, accountAlias, sourcePdf, importStatus, reviewRequired, occurrenceByBaseFingerprint));
        }
        return transactions;
    }

    private BankTransaction createTransaction(
        PendingTransaction pending,
        String accountAlias,
        String sourcePdf,
        String importStatus,
        boolean reviewRequired,
        Map<String, Integer> occurrenceByBaseFingerprint
    ) {
        String movementType = providerDetector.movementType(pending.description(), pending.amount());
        String provider = providerDetector.detect(pending.description());
        String baseFingerprint = accountAlias + "|" + pending.date() + "|" + pending.description() + "|" + pending.amount() + "|" + pending.reference();
        int occurrence = occurrenceByBaseFingerprint.merge(baseFingerprint, 1, Integer::sum);
        String fingerprint = Fingerprint.of(occurrence == 1 ? baseFingerprint : baseFingerprint + "|occurrence:" + occurrence);
        BankTransaction transaction = new BankTransaction(
            0,
            pending.date(),
            pending.description(),
            pending.amount(),
            movementType,
            provider,
            pending.reference(),
            pending.date().getMonthValue(),
            pending.date().getYear(),
            sourcePdf,
            fingerprint,
            false
        );
        transaction.setAccountAlias(accountAlias);
        transaction.setImportStatus(importStatus);
        transaction.setReviewRequired(reviewRequired);
        transaction.setPendingReview(reviewRequired);
        transaction.setReviewNotes(reviewRequired ? "OCR: revisar contra el PDF original" : "Revisar contra el PDF original");
        return transaction;
    }

    private double signedAmount(double previousBalance, double magnitude, double currentBalance) {
        if (Math.abs((previousBalance + magnitude) - currentBalance) <= 0.01) {
            return magnitude;
        }
        if (Math.abs((previousBalance - magnitude) - currentBalance) <= 0.01) {
            return -magnitude;
        }
        return magnitude;
    }

    private BankStatementPeriod detectPeriod(String text, String accountAlias, String sourcePdf) {
        Matcher periodMatcher = PERIOD.matcher(text);
        boolean hasPeriod = periodMatcher.find();
        LocalDate start = hasPeriod ? LocalDate.parse(periodMatcher.group(1), LONG_DATE) : LocalDate.now();
        LocalDate end = hasPeriod ? LocalDate.parse(periodMatcher.group(2), LONG_DATE) : start;
        Matcher summaryMatcher = SUMMARY.matcher(text);
        double openingBalance = 0;
        double endingBalance = 0;
        if (summaryMatcher.find()) {
            openingBalance = Money.parse(summaryMatcher.group(1));
            endingBalance = Money.parse(summaryMatcher.group(4));
        }
        return new BankStatementPeriod(accountAlias, sourcePdf, start, end, openingBalance, endingBalance);
    }

    private String detectCurrency(String text) {
        Matcher matcher = STATEMENT_TYPE.matcher(text);
        return matcher.find() ? matcher.group(1) : "REV";
    }

    private String detectAccountAlias(String text, String currency) {
        Matcher matcher = ACCOUNT_NUMBER.matcher(text);
        String last4 = "0000";
        if (matcher.find()) {
            String account = matcher.group(1);
            last4 = account.length() <= 4 ? account : account.substring(account.length() - 4);
        }
        return "Revolut " + currency + " " + last4;
    }

    private Month month(String shortMonth) {
        return switch (shortMonth) {
            case "Jan" -> Month.JANUARY;
            case "Feb" -> Month.FEBRUARY;
            case "Mar" -> Month.MARCH;
            case "Apr" -> Month.APRIL;
            case "May" -> Month.MAY;
            case "Jun" -> Month.JUNE;
            case "Jul" -> Month.JULY;
            case "Aug" -> Month.AUGUST;
            case "Sep" -> Month.SEPTEMBER;
            case "Oct" -> Month.OCTOBER;
            case "Nov" -> Month.NOVEMBER;
            case "Dec" -> Month.DECEMBER;
            default -> throw new IllegalArgumentException("Mes Revolut no reconocido: " + shortMonth);
        };
    }

    private record PendingTransaction(LocalDate date, String description, double amount, String reference) {
    }

    public record ParsedRevolutBankStatement(BankStatementPeriod period, List<BankTransaction> transactions) {
    }
}
