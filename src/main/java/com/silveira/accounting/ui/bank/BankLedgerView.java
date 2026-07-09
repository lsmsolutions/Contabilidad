package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.dto.BankLedgerRow;
import com.silveira.accounting.application.bank.dto.BankLedgerSnapshot;
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

public class BankLedgerView {
    public VBox build(BankLedgerSnapshot snapshot, IntConsumer yearChanged, IntConsumer monthChanged, Node backButton) {
        Label heading = new Label("Bank Ledger - " + snapshot.year());
        heading.getStyleClass().add("heading");

        ComboBox<Integer> year = new ComboBox<>();
        year.getItems().setAll(snapshot.availableYears());
        year.setValue(snapshot.year());
        year.getStyleClass().add("bank-ledger-year");
        year.setOnAction(event -> {
            Integer selected = year.getValue();
            if (selected != null) {
                yearChanged.accept(selected);
            }
        });

        HBox months = monthButtons(snapshot.month(), monthChanged);
        VBox controls = new VBox(10, new HBox(10, new Label("Year"), year), months);
        controls.getStyleClass().add("bank-ledger-controls");

        VBox content = new VBox(
            18,
            heading,
            backButton,
            controls,
            section(
                "Accounts",
                snapshot.accountRows(),
                snapshot.accountTotals(),
                snapshot.accumulatedAccountRows(),
                snapshot.accumulatedAccountTotals()
            ),
            section(
                "Rey Account",
                snapshot.reyRows(),
                snapshot.reyTotals(),
                snapshot.accumulatedReyRows(),
                snapshot.accumulatedReyTotals()
            )
        );
        content.setPadding(new Insets(28));
        content.getStyleClass().addAll("page", "bank-ledger-page");
        return content;
    }

    private HBox monthButtons(int selectedMonth, IntConsumer monthChanged) {
        HBox row = new HBox(6);
        row.getStyleClass().add("bank-ledger-months");
        for (int month = 1; month <= 12; month++) {
            Button button = new Button(Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            button.getStyleClass().add("bank-ledger-month-button");
            if (month == selectedMonth) {
                button.getStyleClass().add("bank-ledger-month-selected");
            }
            int selected = month;
            button.setOnAction(event -> monthChanged.accept(selected));
            row.getChildren().add(button);
        }
        return row;
    }

    private VBox section(
        String title,
        java.util.List<BankLedgerRow> rows,
        BankLedgerRow totals,
        java.util.List<BankLedgerRow> accumulatedRows,
        BankLedgerRow accumulatedTotals
    ) {
        Label label = new Label(title);
        label.getStyleClass().add("bank-ledger-section-title");
        GridPane table = table("bank-ledger-table-monthly", true);
        addHeader(table, "Account");
        int rowIndex = 1;
        for (BankLedgerRow row : rows) {
            addRow(table, rowIndex++, row, false);
        }
        addRow(table, rowIndex, totals, true);
        GridPane accumulated = table("bank-ledger-table-accumulated", false);
        addHeader(accumulated, "");
        rowIndex = 1;
        for (BankLedgerRow row : accumulatedRows) {
            addAccumulatedRow(accumulated, rowIndex++, row, false);
        }
        addAccumulatedRow(accumulated, rowIndex, accumulatedTotals, true);
        Label monthlyTitle = new Label("Monthly");
        monthlyTitle.getStyleClass().add("bank-ledger-table-title");
        Label accumulatedTitle = new Label("Accumulated");
        accumulatedTitle.getStyleClass().add("bank-ledger-table-title");
        VBox monthlyBlock = new VBox(6, monthlyTitle, table);
        monthlyBlock.getStyleClass().add("bank-ledger-table-block");
        VBox accumulatedBlock = new VBox(6, accumulatedTitle, accumulated);
        accumulatedBlock.getStyleClass().add("bank-ledger-table-block");
        HBox tables = new HBox(14, monthlyBlock, accumulatedBlock);
        tables.getStyleClass().add("bank-ledger-table-row");
        VBox section = new VBox(8, label, tables);
        section.getStyleClass().add("bank-ledger-section");
        return section;
    }

    private GridPane table(String styleClass, boolean includeAccount) {
        GridPane table = new GridPane();
        table.getStyleClass().addAll("bank-ledger-table", styleClass);
        double[] widths = includeAccount ? new double[] {40, 20, 20, 20} : new double[] {33.33, 33.33, 33.34};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            table.getColumnConstraints().add(column);
        }
        return table;
    }

    private void addHeader(GridPane table, String firstColumn) {
        String[] headers = {firstColumn, "Deposits", "Withdrawals", "Available Balance"};
        int start = firstColumn.isBlank() ? 1 : 0;
        for (int column = start; column < headers.length; column++) {
            Label label = cell(headers[column], "bank-ledger-header", columnStyle(column, false));
            if (column > 0) {
                label.getStyleClass().add("bank-ledger-number-header");
            }
            table.add(label, firstColumn.isBlank() ? column - 1 : column, 0);
        }
    }

    private void addRow(GridPane table, int rowIndex, BankLedgerRow row, boolean total) {
        String style = total ? "bank-ledger-total-cell" : "bank-ledger-cell";
        table.add(cell(accountText(row), style), 0, rowIndex);
        table.add(cell(Money.format(row.deposits()), style, "bank-ledger-money", columnStyle(1, total)), 1, rowIndex);
        table.add(cell(Money.format(row.withdrawals()), style, "bank-ledger-money", columnStyle(2, total)), 2, rowIndex);
        table.add(cell(Money.format(row.calculatedBalance()), style, "bank-ledger-money", columnStyle(3, total)), 3, rowIndex);
    }

    private void addAccumulatedRow(GridPane table, int rowIndex, BankLedgerRow row, boolean total) {
        String style = total ? "bank-ledger-total-cell" : "bank-ledger-cell";
        table.add(cell(Money.format(row.deposits()), style, "bank-ledger-money", columnStyle(1, total)), 0, rowIndex);
        table.add(cell(Money.format(row.withdrawals()), style, "bank-ledger-money", columnStyle(2, total)), 1, rowIndex);
        table.add(cell(Money.format(row.calculatedBalance()), style, "bank-ledger-money", columnStyle(3, total)), 2, rowIndex);
    }

    private String accountText(BankLedgerRow row) {
        if (row.account().startsWith("Total")) {
            return row.account();
        }
        return java.util.stream.Stream.of(row.account(), row.bank(), row.ending())
            .filter(value -> value != null && !value.isBlank())
            .collect(java.util.stream.Collectors.joining(" - "));
    }

    private String columnStyle(int column, boolean total) {
        return switch (column) {
            case 1 -> total ? "bank-ledger-deposits-total" : "bank-ledger-deposits";
            case 2 -> total ? "bank-ledger-withdrawals-total" : "bank-ledger-withdrawals";
            case 3 -> total ? "bank-ledger-balance-total" : "bank-ledger-balance";
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
