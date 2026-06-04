package com.silveira.accounting.parsers;

import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.utils.Fingerprint;
import com.silveira.accounting.utils.Money;

import java.nio.file.Path;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NylPdfParser {
    private static final Pattern YEAR = Pattern.compile("\\b(20\\d{2})\\b");
    private static final Pattern MONEY_AT_END = Pattern.compile("^(.+?)\\s+(-?\\(?\\$?[0-9,]+\\.\\d{2}\\)?)$");
    private static final Pattern MONEY_ONLY = Pattern.compile("^-?\\(?\\$?[0-9,]+\\.\\d{2}\\)?$");
    private final PdfTextExtractor extractor = new PdfTextExtractor();
    private final Map<String, Integer> monthNames = buildMonthMap();

    public List<NylRecord> parse(Path pdf) {
        String text = extractor.extract(pdf);
        return parseText(text, pdf.getFileName().toString(), "importado_auto", false);
    }

    public List<NylRecord> parseText(String text, String sourcePdf, String importStatus, boolean forceReview) {
        int currentYear = detectYear(text, detectYear(sourcePdf, java.time.LocalDate.now().getYear()));
        List<NylRecord> hierarchical = parseHierarchicalMonthly(text, sourcePdf, importStatus, forceReview, currentYear);
        if (!hierarchical.isEmpty()) {
            return hierarchical;
        }
        Integer currentMonth = null;
        List<NylRecord> records = new ArrayList<>();
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            if (line.isBlank()) {
                continue;
            }
            Integer detectedMonth = detectMonth(line);
            if (detectedMonth != null) {
                currentMonth = detectedMonth;
                currentYear = detectYear(line, currentYear);
            }
            Matcher money = MONEY_AT_END.matcher(line);
            if (currentMonth == null || !money.matches()) {
                continue;
            }
            String concept = cleanConcept(money.group(1));
            if (concept.length() < 3 || concept.toLowerCase(Locale.ROOT).matches(".*\\b(total|balance|page)\\b.*")) {
                continue;
            }
            double amount = Money.parse(money.group(2));
            String type = classify(concept, amount);
            String fingerprint = Fingerprint.of(currentYear + "|" + currentMonth + "|" + concept + "|" + type + "|" + amount);
            boolean reviewRequired = forceReview || isSuspicious(concept, amount);
            String notes = reviewRequired ? reviewReason(concept, amount, forceReview) : "";
            records.add(new NylRecord(0, currentYear, currentMonth, concept, defaultSection(type), type, amount, sourcePdf, fingerprint, importStatus, reviewRequired, reviewRequired, notes));
        }
        return records;
    }

    private List<NylRecord> parseHierarchicalMonthly(String text, String sourcePdf, String importStatus, boolean forceReview, int currentYear) {
        List<NylRecord> records = new ArrayList<>();
        String currentConcept = null;
        String currentSection = "Creditos";
        Integer pendingMonth = null;
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            if (line.isBlank() || shouldIgnoreOcrLine(line)) {
                continue;
            }
            String section = detectSection(line);
            if (section != null) {
                currentSection = section;
                currentConcept = null;
                pendingMonth = null;
                continue;
            }
            Matcher monthWithAmount = Pattern.compile("^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+(-?\\(?\\$?[0-9,]+\\.\\d{2}\\)?)$", Pattern.CASE_INSENSITIVE).matcher(line);
            if (monthWithAmount.matches()) {
                Integer month = detectMonth(monthWithAmount.group(1));
                if (month != null && currentConcept != null) {
                    addRecord(records, currentYear, month, currentConcept, currentSection, Money.parse(monthWithAmount.group(2)), sourcePdf, importStatus, forceReview);
                }
                pendingMonth = null;
                continue;
            }
            Integer detectedMonth = detectExactMonth(line);
            if (detectedMonth != null) {
                pendingMonth = detectedMonth;
                continue;
            }
            if (MONEY_ONLY.matcher(line).matches()) {
                double amount = Money.parse(line);
                if (pendingMonth != null && currentConcept != null) {
                    addRecord(records, currentYear, pendingMonth, currentConcept, currentSection, amount, sourcePdf, importStatus, forceReview);
                }
                pendingMonth = null;
                continue;
            }
            if (isConceptCandidate(line)) {
                currentConcept = cleanConcept(line);
                pendingMonth = null;
            }
        }
        return records;
    }

    private void addRecord(List<NylRecord> records, int year, int month, String concept, String section, double amount, String sourcePdf, String importStatus, boolean forceReview) {
        if (amount == 0) {
            return;
        }
        String type = classify(concept, amount);
        if ("deduccion".equals(type) && amount > 0) {
            amount = -amount;
        }
        String fingerprint = Fingerprint.of(year + "|" + month + "|" + concept + "|" + type + "|" + amount);
        boolean reviewRequired = forceReview || isSuspicious(concept, amount);
        String notes = reviewRequired ? reviewReason(concept, amount, forceReview) : "";
        records.add(new NylRecord(0, year, month, concept, section, type, amount, sourcePdf, fingerprint, importStatus, reviewRequired, reviewRequired, notes));
    }

    private String detectSection(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        if (normalized.equals("credits") || normalized.equals("credit")) {
            return "Creditos";
        }
        if (normalized.equals("deductions") || normalized.equals("deduction")) {
            return "Deducciones";
        }
        if (normalized.equals("withdrawals") || normalized.equals("withdrawal")) {
            return "Withdrawals";
        }
        if (normalized.equals("adjustments") || normalized.equals("adjustment")) {
            return "Ajustes";
        }
        return null;
    }

    private String defaultSection(String type) {
        if ("deduccion".equals(type)) {
            return "Deducciones";
        }
        if ("withdrawal".equals(type)) {
            return "Withdrawals";
        }
        if ("ajuste".equals(type)) {
            return "Ajustes";
        }
        return "Creditos";
    }

    private boolean shouldIgnoreOcrLine(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.startsWith("--- page")
            || lower.startsWith("expand all")
            || lower.startsWith("collapse all")
            || lower.startsWith("agent:")
            || lower.startsWith("as of:")
            || lower.startsWith("estimating resolution")
            || lower.contains("yudit silveira");
    }

    private boolean isConceptCandidate(String line) {
        if (line.length() < 3 || line.length() > 80) {
            return false;
        }
        if (line.matches(".*\\d{2,}.*")) {
            return false;
        }
        return line.matches(".*[A-Za-z].*");
    }

    public double detectDeclaredTotal(String text) {
        double total = Double.NaN;
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            if (!line.toLowerCase(Locale.ROOT).matches(".*\\b(total|net|summary)\\b.*")) {
                continue;
            }
            Matcher money = MONEY_AT_END.matcher(line);
            if (money.matches()) {
                total = Money.parse(money.group(2));
            }
        }
        return total;
    }

    private String cleanConcept(String raw) {
        return raw.replaceAll("^[0-9/\\- ]+", "").trim();
    }

    private String classify(String concept, double amount) {
        String value = concept.toLowerCase(Locale.ROOT);
        if (value.contains("withdrawal")) {
            return "withdrawal";
        }
        if (value.contains("deduction") || value.contains("charge") || value.contains("loan") || value.contains("fee")) {
            return "deduccion";
        }
        if (value.contains("adjust") || value.contains("correction")) {
            return "ajuste";
        }
        if (value.contains("commission") || value.contains("credit") || value.contains("ledger payment")) {
            return amount < 0 ? "deduccion" : "comision";
        }
        return amount < 0 ? "deduccion" : "credito";
    }

    private boolean isSuspicious(String concept, double amount) {
        return Math.abs(amount) > 100_000
            || concept.matches(".*[{}\\[\\]|?].*")
            || concept.length() < 3;
    }

    private String reviewReason(String concept, double amount, boolean ocr) {
        List<String> reasons = new ArrayList<>();
        if (ocr) {
            reasons.add("OCR: revisar contra el PDF original");
        }
        if (Math.abs(amount) > 100_000) {
            reasons.add("importe inusualmente alto");
        }
        if (concept.matches(".*[{}\\[\\]|?].*") || concept.length() < 3) {
            reasons.add("concepto dudoso");
        }
        return String.join("; ", reasons);
    }

    private int detectYear(String text) {
        return detectYear(text, java.time.LocalDate.now().getYear());
    }

    private int detectYear(String text, int fallback) {
        Matcher matcher = YEAR.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private Integer detectMonth(String line) {
        String normalized = line.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Integer> entry : monthNames.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        Matcher numeric = Pattern.compile("\\b(0?[1-9]|1[0-2])[/-](20\\d{2})\\b").matcher(line);
        return numeric.find() ? Integer.parseInt(numeric.group(1)) : null;
    }

    private Integer detectExactMonth(String line) {
        String normalized = line.toLowerCase(Locale.ROOT).replace(".", "");
        return monthNames.get(normalized);
    }

    private Map<String, Integer> buildMonthMap() {
        Map<String, Integer> months = new HashMap<>();
        for (Month month : Month.values()) {
            months.put(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ROOT), month.getValue());
            months.put(month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase(Locale.ROOT).replace(".", ""), month.getValue());
        }
        return months;
    }
}
