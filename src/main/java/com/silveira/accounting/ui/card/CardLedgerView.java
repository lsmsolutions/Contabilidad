package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.dto.CardLedgerRow;
import com.silveira.accounting.application.card.dto.CardLedgerSnapshot;
import com.silveira.accounting.utils.Money;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CardLedgerView {
    public VBox build(CardLedgerSnapshot snapshot, IntConsumer yearChanged, IntConsumer monthChanged, Node backButton) {
        Label heading = new Label("Card Ledger - " + snapshot.year());
        heading.getStyleClass().add("heading");

        ComboBox<Integer> year = new ComboBox<>();
        year.getItems().setAll(snapshot.availableYears());
        year.setValue(snapshot.year());
        year.getStyleClass().add("card-ledger-year");
        year.setOnAction(event -> {
            Integer selected = year.getValue();
            if (selected != null) {
                yearChanged.accept(selected);
            }
        });

        VBox controls = new VBox(10, new HBox(10, new Label("Year"), year), monthButtons(snapshot.month(), monthChanged));
        controls.getStyleClass().add("card-ledger-controls");

        VBox content = new VBox(18, heading, backButton, controls, section(snapshot.rows(), snapshot.totals()));
        content.setPadding(new Insets(28));
        content.getStyleClass().addAll("page", "card-ledger-page");
        return content;
    }

    private HBox monthButtons(int selectedMonth, IntConsumer monthChanged) {
        HBox row = new HBox(6);
        row.getStyleClass().add("card-ledger-months");
        for (int month = 1; month <= 12; month++) {
            Button button = new Button(Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            button.getStyleClass().add("card-ledger-month-button");
            if (month == selectedMonth) {
                button.getStyleClass().add("card-ledger-month-selected");
            }
            int selected = month;
            button.setOnAction(event -> monthChanged.accept(selected));
            row.getChildren().add(button);
        }
        return row;
    }

    private VBox section(java.util.List<CardLedgerRow> rows, CardLedgerRow totals) {
        Label title = new Label("Cards");
        title.getStyleClass().add("card-ledger-section-title");

        GridPane monthly = monthlyTable();
        addMonthlyHeader(monthly);
        int rowIndex = 1;
        for (CardLedgerRow row : rows) {
            addMonthlyRow(monthly, rowIndex++, row, false);
        }
        addMonthlyRow(monthly, rowIndex, totals, true);

        GridPane accumulated = accumulatedTable();
        addAccumulatedHeader(accumulated);
        rowIndex = 1;
        for (CardLedgerRow row : rows) {
            addAccumulatedRow(accumulated, rowIndex++, row, false);
        }
        addAccumulatedRow(accumulated, rowIndex, totals, true);

        Label monthlyTitle = new Label("Monthly");
        monthlyTitle.getStyleClass().add("card-ledger-table-title");
        Label accumulatedTitle = new Label("Accumulated Interest");
        accumulatedTitle.getStyleClass().add("card-ledger-table-title");
        VBox monthlyBlock = new VBox(6, monthlyTitle, monthly);
        monthlyBlock.getStyleClass().add("card-ledger-table-block");
        VBox accumulatedBlock = new VBox(6, accumulatedTitle, accumulated);
        accumulatedBlock.getStyleClass().add("card-ledger-table-block");
        HBox tables = new HBox(18, monthlyBlock, accumulatedBlock);
        tables.getStyleClass().add("card-ledger-table-row");

        VBox section = new VBox(8, title, tables);
        section.getStyleClass().add("card-ledger-section");
        return section;
    }

    private GridPane monthlyTable() {
        return table("card-ledger-table-monthly", 36, 16, 16, 16, 16);
    }

    private GridPane accumulatedTable() {
        return table("card-ledger-table-accumulated", 100);
    }

    private GridPane table(String styleClass, double... widths) {
        GridPane table = new GridPane();
        table.getStyleClass().addAll("card-ledger-table", styleClass);
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            table.getColumnConstraints().add(column);
        }
        return table;
    }

    private void addMonthlyHeader(GridPane table) {
        String[] headers = {"Account", "Credit Limit", "Used Balance", "Available Credit", "Interest"};
        for (int column = 0; column < headers.length; column++) {
            Label label = cell(headers[column], "card-ledger-header", columnStyle(column, false));
            if (column > 0) {
                label.getStyleClass().add("card-ledger-number-header");
            }
            table.add(label, column, 0);
        }
    }

    private void addAccumulatedHeader(GridPane table) {
        table.add(cell("Interest", "card-ledger-header", "card-ledger-interest", "card-ledger-number-header"), 0, 0);
    }

    private void addMonthlyRow(GridPane table, int rowIndex, CardLedgerRow row, boolean total) {
        String style = total ? "card-ledger-total-cell" : "card-ledger-cell";
        table.add(cell(accountText(row), style), 0, rowIndex);
        table.add(cell(Money.format(row.creditLimit()), style, "card-ledger-money", columnStyle(1, total)), 1, rowIndex);
        table.add(cell(Money.format(row.usedBalance()), style, "card-ledger-money", columnStyle(2, total)), 2, rowIndex);
        table.add(cell(Money.format(row.availableCredit()), style, "card-ledger-money", columnStyle(3, total)), 3, rowIndex);
        table.add(cell(Money.format(row.interest()), style, "card-ledger-money", columnStyle(4, total)), 4, rowIndex);
    }

    private void addAccumulatedRow(GridPane table, int rowIndex, CardLedgerRow row, boolean total) {
        String style = total ? "card-ledger-total-cell" : "card-ledger-cell";
        table.add(cell(Money.format(row.accumulatedInterest()), style, "card-ledger-money", total ? "card-ledger-interest-total" : "card-ledger-interest"), 0, rowIndex);
    }

    private String accountText(CardLedgerRow row) {
        if (row.account().startsWith("Total")) {
            return row.account();
        }
        return java.util.stream.Stream.of(row.account(), row.bank(), row.ending())
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining(" - "));
    }

    private String columnStyle(int column, boolean total) {
        return switch (column) {
            case 1 -> total ? "card-ledger-limit-total" : "card-ledger-limit";
            case 2 -> total ? "card-ledger-used-total" : "card-ledger-used";
            case 3 -> total ? "card-ledger-available-total" : "card-ledger-available";
            case 4 -> total ? "card-ledger-interest-total" : "card-ledger-interest";
            default -> "";
        };
    }

    private Label cell(String text, String... styleClasses) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().addAll(styleClasses);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
