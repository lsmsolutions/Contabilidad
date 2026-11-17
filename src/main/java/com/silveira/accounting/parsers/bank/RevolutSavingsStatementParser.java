package com.silveira.accounting.parsers.bank;

import com.silveira.accounting.application.bank.dto.BankImportData;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.utils.Fingerprint;
import com.silveira.accounting.utils.Money;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RevolutSavingsStatementParser {
    private static final Pattern FILE_PERIOD = Pattern.compile("(20\\d{2}-\\d{2}-\\d{2})_(20\\d{2}-\\d{2}-\\d{2})");
    private static final Pattern PRINTED_PERIOD = Pattern.compile("Period:\\s*([A-Za-z]+\\s+\\d{1,2},\\s+20\\d{2})\\s+[^A-Za-z0-9\\r\\n]+\\s*([A-Za-z]+\\s+\\d{1,2},\\s+20\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("(?m)^\\s*(\\d{8,})\\s*$");
    private static final Pattern TRANSACTION = Pattern.compile("(?m)^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+(\\d{1,2}),\\s+(20\\d{2})\\s+(.+?)\\s+((?:\\p{Sc}|\\x{FFFD})?[0-9,]+\\.\\d{2})\\s+((?:\\p{Sc}|\\x{FFFD})?[0-9,]+\\.\\d{2})\\s*$");
    private static final DateTimeFormatter PRINTED_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private final PdfTextExtractor extractor = new PdfTextExtractor();

    public Optional<BankImportData> parseIfSupported(Path pdf) {
        String text = extractor.extract(pdf);
        if (!supports(text)) return Optional.empty();
        return Optional.of(parse(text, pdf.getFileName().toString()));
    }

    public boolean supports(String text) {
        return text != null && text.contains("Savings Statement") && text.contains("Balance summary") && text.contains("High Yield Savings");
    }

    public BankImportData parse(String text, String sourcePdf) {
        LocalDate[] period = period(text, sourcePdf);
        double opening = labeledAmount(text, "Opening Balance");
        double deposits = labeledAmount(text, "Total Deposit");
        double withdrawals = labeledAmount(text, "Total Withdrawal");
        double interest = labeledAmount(text, "Total Interest Paid");
        double closing = labeledAmount(text, "Closing Balance");
        validateBalance(opening, deposits, withdrawals, interest, closing);

        String alias = "cuenta_" + accountEnding(text);
        BankStatementPeriod statementPeriod = new BankStatementPeriod(alias, sourcePdf, period[0], period[1], opening, closing);
        return new BankImportData(transactions(text, sourcePdf, alias), Optional.of(statementPeriod));
    }

    private LocalDate[] period(String text, String sourcePdf) {
        Matcher file = FILE_PERIOD.matcher(sourcePdf == null ? "" : sourcePdf);
        if (file.find()) return new LocalDate[] { LocalDate.parse(file.group(1)), LocalDate.parse(file.group(2)) };
        Matcher printed = PRINTED_PERIOD.matcher(text);
        if (!printed.find()) throw new IllegalArgumentException("Savings statement period was not found");
        LocalDate start = LocalDate.parse(fileDateText(printed.group(1)), PRINTED_DATE);
        LocalDate end = LocalDate.parse(fileDateText(printed.group(2)), PRINTED_DATE);
        if (start.getDayOfMonth() == 1 && end.getDayOfMonth() == 1 && end.equals(start.plusMonths(1))) end = end.minusDays(1);
        return new LocalDate[] { start, end };
    }

    private String fileDateText(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private double labeledAmount(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s+((?:\\p{Sc}|\\x{FFFD})?[0-9,]+\\.\\d{2})", Pattern.CASE_INSENSITIVE).matcher(text);
        if (!matcher.find()) throw new IllegalArgumentException(label + " was not found in savings statement");
        return Money.parse(matcher.group(1));
    }

    private void validateBalance(double opening, double deposits, double withdrawals, double interest, double closing) {
        double calculated = opening + deposits - withdrawals + interest;
        if (Math.abs(calculated - closing) > 0.011) {
            throw new IllegalArgumentException("Savings statement balances do not reconcile");
        }
    }

    private String accountEnding(String text) {
        Matcher matcher = ACCOUNT_NUMBER.matcher(text);
        if (!matcher.find()) return "0000";
        String number = matcher.group(1);
        return number.substring(number.length() - 4);
    }

    private List<BankTransaction> transactions(String text, String sourcePdf, String alias) {
        List<BankTransaction> result = new ArrayList<>();
        Matcher matcher = TRANSACTION.matcher(text);
        while (matcher.find()) {
            LocalDate date = LocalDate.parse(matcher.group(1) + " " + matcher.group(2) + ", " + matcher.group(3), PRINTED_DATE);
            String description = matcher.group(4).trim();
            double amount = Money.parse(matcher.group(5));
            if (description.toLowerCase(Locale.ROOT).contains("withdraw")) amount = -amount;
            String fingerprint = Fingerprint.of(alias + "|" + date + "|" + description + "|" + amount + "|" + sourcePdf);
            BankTransaction transaction = new BankTransaction(0, date, description, amount, amount < 0 ? "Gasto" : "Ingreso", "Revolut", "", date.getMonthValue(), date.getYear(), sourcePdf, fingerprint, false);
            transaction.setAccountAlias(alias);
            transaction.setImportStatus("importado_auto");
            transaction.setReviewRequired(true);
            transaction.setPendingReview(true);
            transaction.setReviewNotes("Review against the original savings statement");
            result.add(transaction);
        }
        return result;
    }
}
