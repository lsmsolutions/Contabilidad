package com.silveira.accounting.parsers.bank;

import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.parsers.ProviderDetector;
import com.silveira.accounting.utils.Fingerprint;
import com.silveira.accounting.utils.Money;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BankStatementParser {
    private static final Pattern TRANSACTION = Pattern.compile(
        "^(\\d{1,2}[/-]\\d{1,2})(?:[/-](\\d{2,4}))?\\s+(.+?)\\s+(-?\\(?\\$?[0-9,]+\\.\\d{2}\\)?)(?:\\s+-?\\(?\\$?[0-9,]+\\.\\d{2}\\)?)?$"
    );
    private static final Pattern DATE_ONLY = Pattern.compile("^(\\d{1,2}[/-]\\d{1,2})(?:[/-](\\d{2,4}))?$");
    private static final Pattern AMOUNT_ONLY = Pattern.compile("^-?\\(?\\$?[0-9,]+\\.\\d{2}\\)?$");
    private static final Pattern REFERENCE = Pattern.compile("(?:trace|ref|reference|id)\\s*[:#]?\\s*([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private final PdfTextExtractor extractor = new PdfTextExtractor();
    private final ProviderDetector providerDetector = new ProviderDetector();

    public List<BankTransaction> parse(Path pdf) {
        String text = extractor.extract(pdf);
        return parseText(text, pdf.getFileName().toString(), "importado_auto", false);
    }

    public List<BankTransaction> parseText(String text, String sourcePdf, String importStatus, boolean reviewRequired) {
        int fallbackYear = detectYear(text);
        String accountAlias = detectAccountAlias(text);
        List<BankTransaction> transactions = new ArrayList<>();
        String section = "";
        LocalDate pendingDate = null;
        Double pendingAmount = null;
        List<String> pendingDescription = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            String lower = line.toLowerCase(Locale.ROOT);
            if (line.isBlank() || isIgnorableLine(lower)) {
                continue;
            }
            String detectedSection = detectSection(lower);
            if (!detectedSection.isBlank()) {
                section = detectedSection;
                pendingDate = null;
                pendingAmount = null;
                pendingDescription.clear();
                continue;
            }
            if (lower.contains("*start*deposits and additions") || lower.equals("deposits and additions") || lower.startsWith("deposits and additions")) {
                section = "Deposito";
                continue;
            }
            if (lower.contains("*start*electronic withdrawal") || lower.equals("electronic withdrawals")) {
                section = "Retiro electronico";
                continue;
            }
            if (lower.contains("*start*fees section") || lower.equals("fees")) {
                section = "Fee";
                continue;
            }
            if (lower.startsWith("*end*")) {
                section = "";
                pendingDate = null;
                pendingAmount = null;
                pendingDescription.clear();
                continue;
            }
            Matcher matcher = TRANSACTION.matcher(line);
            if (matcher.matches()) {
                LocalDate date = parseDate(matcher.group(1), matcher.group(2), fallbackYear);
                String description = matcher.group(3).trim();
                double rawAmount = Money.parse(matcher.group(4));
                transactions.add(createTransaction(date, description, rawAmount, section, accountAlias, sourcePdf, importStatus, reviewRequired));
                pendingDate = null;
                pendingAmount = null;
                pendingDescription.clear();
                continue;
            }
            Matcher dateMatcher = DATE_ONLY.matcher(line);
            if (dateMatcher.matches()) {
                pendingDate = parseDate(dateMatcher.group(1), dateMatcher.group(2), fallbackYear);
                pendingAmount = null;
                pendingDescription.clear();
                continue;
            }
            if (pendingDate == null) {
                continue;
            }
            if (AMOUNT_ONLY.matcher(line).matches()) {
                pendingAmount = Money.parse(line);
                if (!pendingDescription.isEmpty()) {
                    String description = String.join(" ", pendingDescription).trim();
                    transactions.add(createTransaction(pendingDate, description, pendingAmount, section, accountAlias, sourcePdf, importStatus, reviewRequired));
                    pendingDate = null;
                    pendingAmount = null;
                    pendingDescription.clear();
                }
                continue;
            }
            if (isIgnorableDescriptionLine(lower)) {
                continue;
            }
            pendingDescription.add(line);
            if (pendingAmount != null) {
                String description = String.join(" ", pendingDescription).trim();
                transactions.add(createTransaction(pendingDate, description, pendingAmount, section, accountAlias, sourcePdf, importStatus, reviewRequired));
                pendingDate = null;
                pendingAmount = null;
                pendingDescription.clear();
            }
        }
        return transactions;
    }

    private BankTransaction createTransaction(LocalDate date, String description, double rawAmount, String section,
                                              String accountAlias, String sourcePdf, String importStatus, boolean reviewRequired) {
        double amount = signedAmount(rawAmount, section);
        String provider = providerDetector.detect(description);
        String movementType = section == null || section.isBlank() ? providerDetector.movementType(description, amount) : section;
        String reference = detectReference(description);
        String fingerprint = Fingerprint.of(date + "|" + description + "|" + amount);
        BankTransaction transaction = new BankTransaction(0, date, description, amount, movementType, provider, reference,
            date.getMonthValue(), date.getYear(), sourcePdf, fingerprint, false);
        transaction.setAccountAlias(accountAlias);
        transaction.setImportStatus(importStatus);
        transaction.setReviewRequired(reviewRequired);
        transaction.setPendingReview(reviewRequired);
        transaction.setReviewNotes(reviewRequired ? "OCR: revisar contra el PDF original" : "Revisar contra el PDF original");
        return transaction;
    }

    private boolean isIgnorableLine(String lower) {
        return lower.startsWith("--- page")
            || lower.startsWith("estimating resolution")
            || lower.startsWith("page ")
            || lower.equals("date")
            || lower.equals("description")
            || lower.equals("amount");
    }

    private boolean isIgnorableDescriptionLine(String lower) {
        return lower.length() == 1
            || lower.equals("|")
            || lower.equals(":")
            || lower.equals("l")
            || lower.equals("date")
            || lower.equals("description")
            || lower.equals("amount");
    }

    private String detectSection(String lower) {
        boolean heading = lower.contains("[") || lower.contains("*start*") || lower.length() < 60;
        if (heading && containsAny(lower, "deposits and additions", "depositsand additions", "depositos y abonos", "depósitos y abonos", "credits and deposits", "money in")) {
            return "Deposito";
        }
        if (heading && containsAny(lower, "electronic withdrawal", "withdrawals", "debits", "checks paid", "card purchases",
            "money out", "retiros", "débito", "debito", "salidas")) {
            return "Retiro electronico";
        }
        if (heading && (lower.equals("fees") || lower.contains("fees section") || lower.contains("[fees") || lower.contains("comisiones"))) {
            return "Fee";
        }
        return "";
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String detectAccountAlias(String text) {
        Matcher matcher = Pattern.compile("Account Number:\\s*([0-9]+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            String account = matcher.group(1);
            String last4 = account.length() <= 4 ? account : account.substring(account.length() - 4);
            return "cta_" + last4;
        }
        matcher = Pattern.compile("(?:account ending in|ending in|acct\\.?\\s*#?|cuenta)\\s*[:#-]?\\s*(?:x+|\\*+)?\\s*([0-9]{4,})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            String account = matcher.group(1);
            String last4 = account.length() <= 4 ? account : account.substring(account.length() - 4);
            return "cta_" + last4;
        }
        return "sin_cuenta";
    }

    private double signedAmount(double amount, String section) {
        if ("Retiro electronico".equals(section) || "Fee".equals(section)) {
            return -Math.abs(amount);
        }
        if (section == null || section.isBlank()) {
            return amount;
        }
        return Math.abs(amount);
    }

    private LocalDate parseDate(String monthDay, String explicitYear, int fallbackYear) {
        String[] parts = monthDay.replace('-', '/').split("/");
        int year = explicitYear == null ? fallbackYear : normalizeYear(explicitYear);
        return LocalDate.of(year, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    private int normalizeYear(String year) {
        int parsed = Integer.parseInt(year);
        return parsed < 100 ? 2000 + parsed : parsed;
    }

    private int detectYear(String text) {
        Matcher matcher = Pattern.compile("\\b(20\\d{2})\\b").matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : LocalDate.now().getYear();
    }

    private String detectReference(String description) {
        Matcher matcher = REFERENCE.matcher(description);
        return matcher.find() ? matcher.group(1) : "";
    }
}
