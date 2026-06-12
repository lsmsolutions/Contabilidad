package com.silveira.accounting.ui.bank;

import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.utils.Money;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class BankBreakdownView {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final Pattern ACCOUNT_TRANSFER = Pattern.compile(
        "(?i)\\b(?:online\\s+)?transfer\\s+(to|from)\\s+(?:sav(?:ings)?|chk|checking|account)?\\s*\\.{0,3}\\s*(\\d{4})\\b"
    );
    private static final Pattern ACCOUNT_ENDING = Pattern.compile("(?i)(?:chk|checking|account)[^0-9]{0,12}(?:\\.{0,3})?(\\d{4})");
    private static final Pattern CARD_ENDING = Pattern.compile("(?i)(discover|capital one|citi|best buy|mastercard).*?(\\d{4})");
    private final VBox content = new VBox(18);

    public BankBreakdownView() {
        content.getStyleClass().add("bank-breakdown");
    }

    public VBox node() {
        return content;
    }

    public void refresh(List<BankTransaction> transactions) {
        content.getChildren().clear();
        List<BankTransaction> rows = transactions == null ? List.of() : transactions;
        if (rows.isEmpty()) {
            Label empty = new Label("No movements to summarize.");
            empty.getStyleClass().add("section-subtitle");
            content.getChildren().add(empty);
            return;
        }

        addSection("Deposits", rows.stream().filter(this::isDeposit).toList());
        addSection("Withdrawals", rows.stream().filter(row -> !isDeposit(row) && !isCardPayment(row)).toList());
        addSection("Card Payments", rows.stream().filter(this::isCardPayment).toList());
    }

    private void addSection(String title, List<BankTransaction> transactions) {
        if (transactions.isEmpty()) {
            return;
        }
        VBox section = new VBox(10);
        section.getStyleClass().add("bank-breakdown-section");
        Label heading = new Label(title);
        heading.getStyleClass().add("bank-breakdown-section-title");
        section.getChildren().add(heading);

        Map<String, List<BankTransaction>> groups = new LinkedHashMap<>();
        for (BankTransaction transaction : transactions) {
            groups.computeIfAbsent(groupName(transaction), ignored -> new ArrayList<>()).add(transaction);
        }
        groups.forEach((name, rows) -> section.getChildren().add(group(name, rows)));

        double sectionTotal = transactions.stream().mapToDouble(BankTransaction::getAmount).sum();
        section.getChildren().add(totalRow("Section total", sectionTotal, "bank-breakdown-section-total"));
        content.getChildren().add(section);
    }

    private VBox group(String name, List<BankTransaction> transactions) {
        VBox group = new VBox(5);
        group.getStyleClass().add("bank-breakdown-group");
        Label heading = new Label(name);
        heading.getStyleClass().add("bank-breakdown-group-title");
        GridPane rows = breakdownGrid();
        int row = 0;
        for (BankTransaction transaction : transactions) {
            Label date = new Label(transaction.getDate() == null ? "" : transaction.getDate().format(DATE_FORMAT));
            Label amount = new Label(Money.format(transaction.getAmount()));
            amount.getStyleClass().add("bank-breakdown-amount");
            rows.add(date, 0, row);
            rows.add(amount, 1, row++);
        }
        double total = transactions.stream().mapToDouble(BankTransaction::getAmount).sum();
        group.getChildren().addAll(heading, rows, totalRow("Total", total, "bank-breakdown-group-total"));
        return group;
    }

    private GridPane breakdownGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("bank-breakdown-grid");
        ColumnConstraints date = new ColumnConstraints(140);
        ColumnConstraints amount = new ColumnConstraints(120);
        amount.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().setAll(date, amount);
        return grid;
    }

    private GridPane totalRow(String label, double amount, String styleClass) {
        GridPane row = breakdownGrid();
        row.getStyleClass().add(styleClass);
        Label title = new Label(label);
        Label value = new Label(Money.format(amount));
        value.getStyleClass().add("bank-breakdown-amount");
        row.add(title, 0, 0);
        row.add(value, 1, 0);
        return row;
    }

    private boolean isDeposit(BankTransaction transaction) {
        return transaction.getAmount() >= 0 || "deposito".equalsIgnoreCase(text(transaction.getMovementType()));
    }

    private boolean isCardPayment(BankTransaction transaction) {
        String type = text(transaction.getMovementType()).toLowerCase(Locale.ROOT);
        String description = text(transaction.getDescription()).toLowerCase(Locale.ROOT);
        return "pago con tarjeta".equals(type)
            || description.contains("discover")
            || description.contains("capital one")
            || description.contains("citi card")
            || description.contains("best buy")
            || description.contains("mastercard");
    }

    private String groupName(BankTransaction transaction) {
        String description = text(transaction.getDescription());
        Matcher transfer = ACCOUNT_TRANSFER.matcher(description);
        if (transfer.find()) {
            return "Transfer " + titleCase(transfer.group(1)) + " " + transfer.group(2);
        }
        String merchant = merchantName(description);
        if (!merchant.isBlank()) {
            return merchant;
        }
        Matcher card = CARD_ENDING.matcher(description);
        if (card.find()) {
            return titleCase(card.group(1)) + " " + card.group(2);
        }
        Matcher account = ACCOUNT_ENDING.matcher(description);
        if (account.find()) {
            return "Transaction " + account.group(1);
        }
        String provider = text(transaction.getProvider()).trim();
        if (!provider.isBlank() && !"otros".equalsIgnoreCase(provider) && !"transferencias".equalsIgnoreCase(provider)) {
            return provider;
        }
        String reference = text(transaction.getReference()).trim();
        if (!reference.isBlank() && reference.length() <= 24 && !reference.matches("\\d+")) {
            return reference;
        }
        return description.isBlank() ? "Other" : description;
    }

    private String merchantName(String description) {
        String normalized = description.toLowerCase(Locale.ROOT);
        if (normalized.contains("costco")) {
            return "Costco";
        }
        if (normalized.contains("nylife financial") || normalized.contains("new york life")) {
            return "New York Life";
        }
        if (normalized.contains("sunpass")) {
            return "SunPass";
        }
        if (normalized.contains("am nat ins") || normalized.contains("anico")) {
            return "American National";
        }
        if (normalized.contains("prime video")) {
            return "Prime Video";
        }
        return "";
    }

    private String titleCase(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
