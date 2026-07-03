package com.silveira.accounting.parsers.investment;

import com.silveira.accounting.application.importing.DocumentImportGateway;
import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.utils.Money;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SchwabInvestmentStatementParser implements DocumentImportGateway<InvestmentImportData> {
    private static final Pattern PERIOD = Pattern.compile(
        "(January|February|March|April|May|June|July|August|September|October|November|December)"
            + "\\s+(\\d{1,2})-(\\d{1,2}),\\s+(\\d{4})",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern POSITION_LINE = Pattern.compile(
        "^([A-Z][A-Z0-9.]{0,9})\\s+(.+?)\\s+[?,\\s]*([\\d.]+)\\s+([\\d.]+)\\s+"
            + "([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s+(\\(?[\\d,]+\\.\\d{2}\\)?)\\s+.*$"
    );
    private static final Pattern TRANSACTION_LINE = Pattern.compile(
        "^(?:(\\d{2}/\\d{2})\\s+)?(Sale|Purchase|Deposit|Withdrawal|Dividend|Interest|Reinvest(?:ed)?)\\s+(.*)$",
        Pattern.CASE_INSENSITIVE
    );
    private final PdfTextExtractor extractor = new PdfTextExtractor();

    public InvestmentImportData parse(Path pdf) {
        return importPdf(pdf);
    }

    @Override
    public InvestmentImportData importPdf(Path pdf) {
        String text = extractor.extract(pdf).replace('\uFFFD', ' ');
        if (!text.toLowerCase(Locale.ROOT).contains("schwab one")
            || !text.toLowerCase(Locale.ROOT).contains("positions - summary")) {
            throw new IllegalArgumentException("El PDF no corresponde a un estado de inversiones Schwab One.");
        }

        DateRange period = period(text);
        String ending = match(text, "(?:XXXX-X|\\d{4}-)(\\d{4})");
        if (ending.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar la cuenta Schwab.");
        }

        InvestmentAccount account = new InvestmentAccount();
        account.setAlias("Schwab One " + ending);
        account.setProviderName("Charles Schwab");
        account.setAccountType("Brokerage");
        account.setAccountNumber(ending);

        List<InvestmentPosition> positions = positions(text);
        List<Double> positionSummary = positionSummaryValues(text);

        InvestmentStatement statement = new InvestmentStatement();
        statement.setAccountAlias(account.getAlias());
        statement.setPeriodStart(period.start());
        statement.setPeriodEnd(period.end());
        statement.setBeginningValue(summaryValue(positionSummary, 0, money(text, "Beginning Account Value\\s+\\$?([\\d,]+\\.\\d{2})")));
        statement.setTransferOfSecurities(summaryValue(positionSummary, 1, summaryMoney(text, "Transfer of Securities(In/Out)")));
        statement.setDividendsReinvested(summaryValue(positionSummary, 2, summaryMoney(text, "Dividends Reinvested")));
        statement.setCashActivity(summaryValue(positionSummary, 3, summaryMoney(text, "Cash Activity")));
        statement.setChangeInMarketValue(summaryValue(positionSummary, 4, summaryMoney(text, "Change in Market Value")));
        statement.setEndingValue(summaryValue(positionSummary, 5, money(text, "Ending Account Value\\s+\\$?([\\d,]+\\.\\d{2})")));
        statement.setDeposits(summaryMoney(text, "Deposits"));
        statement.setWithdrawals(summaryMoney(text, "Withdrawals"));
        statement.setDividendsInterest(summaryMoney(text, "Dividends and Interest"));
        statement.setMarketChange(summaryMoney(text, "Market Appreciation/(Depreciation)"));
        statement.setExpenses(summaryMoney(text, "Expenses"));
        statement.setCostBasisTotal(summaryValue(positionSummary, 6, costBasisTotal(text, positions)));
        statement.setUnrealizedGainLoss(summaryValue(positionSummary, 7, money(text, "Unrealized\\s+\\$?([\\d,()]+\\.\\d{2})")));
        statement.setSourcePdfPath(pdf.toAbsolutePath().toString());

        return new InvestmentImportData(
            account,
            statement,
            allocations(text),
            positions,
            transactions(text, period.end().getYear())
        );
    }

    private List<Double> positionSummaryValues(String text) {
        String section = between(text, "Positions - Summary", "Cash and Cash Investments");
        for (String rawLine : section.split("\\R")) {
            List<Double> values = moneyValues(rawLine);
            if (values.size() >= 8) {
                return values.subList(0, 8);
            }
        }
        return List.of();
    }

    private double summaryValue(List<Double> values, int index, double fallback) {
        return values.size() > index ? values.get(index) : fallback;
    }

    private List<Double> moneyValues(String line) {
        List<Double> values = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\(?\\$?[\\d,]+\\.\\d{2}\\)?").matcher(line);
        while (matcher.find()) {
            values.add(Money.parse(matcher.group()));
        }
        return values;
    }

    private double costBasisTotal(String text, List<InvestmentPosition> positions) {
        double parsed = money(text, "\\bCost Basis\\s+\\$?([\\d,]+\\.\\d{2})");
        if (parsed != 0) {
            return parsed;
        }
        return positions.stream().mapToDouble(InvestmentPosition::getCostBasis).sum();
    }

    private List<InvestmentAllocation> allocations(String text) {
        List<InvestmentAllocation> values = new ArrayList<>();
        String section = between(text, "Asset Allocation", "Top Account Holdings");
        for (String category : List.of(
            "Cash and Cash Investments",
            "Equities",
            "Mutual Funds",
            "Exchange Traded Funds"
        )) {
            Matcher matcher = Pattern.compile(
                Pattern.quote(category) + "\\s+([\\d,]+\\.\\d{2})\\s+(<?\\d+)%",
                Pattern.CASE_INSENSITIVE
            ).matcher(section);
            if (matcher.find()) {
                InvestmentAllocation value = new InvestmentAllocation();
                value.setCategory(category);
                value.setMarketValue(Money.parse(matcher.group(1)));
                value.setPercentage(Double.parseDouble(matcher.group(2).replace("<", "")));
                values.add(value);
            }
        }
        return values;
    }

    private List<InvestmentPosition> positions(String text) {
        List<InvestmentPosition> values = new ArrayList<>();
        String assetType = "";
        boolean reading = false;
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith("Positions - Equities")) {
                assetType = "Equity";
                reading = true;
                continue;
            }
            if (line.startsWith("Positions - Mutual Funds")) {
                assetType = "Mutual Fund";
                reading = true;
                continue;
            }
            if (line.startsWith("Positions - Exchange Traded Funds")) {
                assetType = "ETF";
                reading = true;
                continue;
            }
            if (line.startsWith("Transactions - Summary")) {
                break;
            }
            if (!reading || line.startsWith("Total ") || line.startsWith("Symbol ") || line.startsWith("Unrealized ")) {
                continue;
            }
            Matcher matcher = POSITION_LINE.matcher(line);
            if (matcher.matches()) {
                InvestmentPosition value = new InvestmentPosition();
                value.setSymbol(matcher.group(1));
                value.setDescription(cleanDescription(matcher.group(2)));
                value.setAssetType(assetType);
                value.setQuantity(Money.parse(matcher.group(3)));
                value.setPrice(Money.parse(matcher.group(4)));
                value.setMarketValue(Money.parse(matcher.group(5)));
                value.setCostBasis(Money.parse(matcher.group(6)));
                value.setUnrealizedGainLoss(Money.parse(matcher.group(7)));
                values.add(value);
            }
        }
        return values;
    }

    private List<InvestmentTransaction> transactions(String text, int year) {
        String section = between(text, "Transaction Details", "Total Transactions");
        List<InvestmentTransaction> values = new ArrayList<>();
        LocalDate currentDate = null;
        for (String rawLine : section.split("\\R")) {
            String line = rawLine.trim();
            Matcher matcher = TRANSACTION_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            if (matcher.group(1) != null) {
                currentDate = date(matcher.group(1), year);
            }
            String action = normalizeAction(matcher.group(2));
            String rest = matcher.group(3).replaceAll("\\s+", " ").trim();
            InvestmentTransaction value = parseTransaction(currentDate, action, rest);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private InvestmentTransaction parseTransaction(LocalDate date, String action, String rest) {
        List<String> numbers = new ArrayList<>();
        Matcher amounts = Pattern.compile("\\(?[\\d,]+\\.\\d{2,4}\\)?(?:,?\\(ST\\)|,?\\(LT\\))?").matcher(rest);
        while (amounts.find()) {
            numbers.add(amounts.group());
        }
        if (numbers.isEmpty()) {
            return null;
        }

        InvestmentTransaction value = new InvestmentTransaction();
        value.setTransactionDate(date);
        value.setAction(action);
        value.setCategory(categoryForAction(action));
        String first = rest.split("\\s+", 2)[0];
        boolean securityAction = action.equals("Sale") || action.equals("Purchase") || action.equals("Reinvest");
        value.setSymbol(securityAction && first.matches("[A-Z][A-Z0-9.]{0,9}") ? first : "");
        value.setDescription(rest);
        if (securityAction && numbers.size() >= 3) {
            value.setQuantity(Math.abs(Money.parse(numbers.get(0))));
            value.setPrice(Math.abs(Money.parse(numbers.get(1))));
            value.setAmount(Money.parse(numbers.get(2)));
            if (numbers.size() >= 4) {
                value.setRealizedGainLoss(transactionMoney(numbers.get(3)));
            }
        } else {
            value.setAmount(Money.parse(numbers.get(numbers.size() - 1)));
        }
        if (action.equals("Purchase") || action.equals("Withdrawal")) {
            value.setAmount(-Math.abs(value.getAmount()));
        }
        return value;
    }

    private String categoryForAction(String action) {
        return switch (action) {
            case "Purchase" -> "Purchases";
            case "Dividend", "Interest", "Reinvest" -> "Dividends/Interest";
            case "Sale" -> "Sales/Redemptions";
            case "Deposit" -> "Deposits";
            case "Withdrawal" -> "Withdrawals";
            default -> action == null || action.isBlank() ? "Other Activity" : action;
        };
    }

    private double summaryMoney(String text, String label) {
        return money(text, "\\b" + flexibleLabel(label) + "\\s+(\\(?[\\d,]+\\.\\d{2}\\)?)");
    }

    private String flexibleLabel(String label) {
        String[] parts = label.trim().split("\\s+");
        List<String> quoted = new ArrayList<>();
        for (String part : parts) {
            quoted.add(Pattern.quote(part));
        }
        return String.join("\\s+", quoted);
    }

    private double transactionMoney(String value) {
        return Money.parse(value.replaceAll(",?\\((?:ST|LT)\\)$", ""));
    }

    private double money(String text, String regex) {
        String value = match(text, regex);
        return value.isBlank() ? 0 : Money.parse(value);
    }

    private DateRange period(String text) {
        Matcher matcher = PERIOD.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No se pudo identificar el periodo del estado Schwab.");
        }
        Month month = Month.valueOf(matcher.group(1).toUpperCase(Locale.ENGLISH));
        int year = Integer.parseInt(matcher.group(4));
        return new DateRange(
            LocalDate.of(year, month, Integer.parseInt(matcher.group(2))),
            LocalDate.of(year, month, Integer.parseInt(matcher.group(3)))
        );
    }

    private LocalDate date(String monthDay, int year) {
        return LocalDate.parse(monthDay + "/" + year, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    private String between(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex < 0) {
            return "";
        }
        int endIndex = text.indexOf(end, startIndex + start.length());
        return endIndex < 0 ? text.substring(startIndex) : text.substring(startIndex, endIndex);
    }

    private String match(String text, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String cleanDescription(String value) {
        return value.replaceAll("[?,]+$", "").replaceAll("\\s+", " ").trim();
    }

    private String normalizeAction(String value) {
        String action = value.toLowerCase(Locale.ROOT);
        if (action.startsWith("reinvest")) {
            return "Reinvest";
        }
        return Character.toUpperCase(action.charAt(0)) + action.substring(1);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
