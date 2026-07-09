package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.dto.MortgageLedgerRow;
import com.silveira.accounting.application.mortgage.dto.MortgageLedgerSnapshot;
import com.silveira.accounting.utils.Money;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MortgageLedgerView {
    public VBox build(
        MortgageLedgerSnapshot snapshot,
        Consumer<String> loanChanged,
        IntConsumer yearChanged,
        Node backButton
    ) {
        Label heading = new Label("Mortgage Ledger - " + snapshot.year());
        heading.getStyleClass().add("heading");

        ComboBox<String> loan = new ComboBox<>();
        loan.getItems().setAll(snapshot.loans());
        loan.setValue(snapshot.selectedLoan());
        loan.getStyleClass().add("mortgage-ledger-combo");
        loan.setOnAction(event -> {
            String selected = loan.getValue();
            if (selected != null && !selected.isBlank()) {
                loanChanged.accept(selected);
            }
        });

        ComboBox<Integer> year = new ComboBox<>();
        year.getItems().setAll(snapshot.availableYears());
        year.setValue(snapshot.year());
        year.getStyleClass().add("mortgage-ledger-year");
        year.setOnAction(event -> {
            Integer selected = year.getValue();
            if (selected != null) {
                yearChanged.accept(selected);
            }
        });

        VBox controls = new VBox(10, new HBox(10, new Label("Mortgage"), loan, new Label("Year"), year));
        controls.getStyleClass().add("mortgage-ledger-controls");

        VBox content = new VBox(18, heading, backButton, controls, initialDebt(snapshot), ledgerTable(snapshot));
        content.setPadding(new Insets(28));
        content.getStyleClass().addAll("page", "mortgage-ledger-page");
        return content;
    }

    private VBox initialDebt(MortgageLedgerSnapshot snapshot) {
        Label label = new Label("Initial Debt");
        label.getStyleClass().add("mortgage-ledger-initial-label");
        Label value = new Label(Money.format(snapshot.initialDebt()));
        value.getStyleClass().add("mortgage-ledger-initial-value");
        VBox box = new VBox(4, label, value);
        box.getStyleClass().add("mortgage-ledger-initial");
        return box;
    }

    private VBox ledgerTable(MortgageLedgerSnapshot snapshot) {
        GridPane table = new GridPane();
        table.getStyleClass().add("mortgage-ledger-table");
        double[] widths = {16, 17, 17, 17, 22};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            table.getColumnConstraints().add(column);
        }
        addHeader(table);
        int rowIndex = 1;
        for (MortgageLedgerRow row : snapshot.rows()) {
            addRow(table, rowIndex++, row);
        }
        addTotals(table, rowIndex, snapshot);
        VBox box = new VBox(8, table);
        box.getStyleClass().add("mortgage-ledger-section");
        return box;
    }

    private void addHeader(GridPane table) {
        String[] headers = {"Month", "Debt Paid", "Interest", "Escrow", "Outstanding Debt"};
        for (int column = 0; column < headers.length; column++) {
            Label label = cell(headers[column], "mortgage-ledger-header", columnStyle(column, false));
            if (column > 0) {
                label.getStyleClass().add("mortgage-ledger-number-header");
            }
            table.add(label, column, 0);
        }
    }

    private void addRow(GridPane table, int rowIndex, MortgageLedgerRow row) {
        table.add(cell(Month.of(row.month()).getDisplayName(TextStyle.FULL, Locale.ENGLISH), "mortgage-ledger-cell"), 0, rowIndex);
        table.add(cell(moneyOrBlank(row.debtPaid(), row.hasStatement()), "mortgage-ledger-cell", "mortgage-ledger-money", "mortgage-ledger-debt-paid"), 1, rowIndex);
        table.add(cell(moneyOrBlank(row.interest(), row.hasStatement()), "mortgage-ledger-cell", "mortgage-ledger-money", "mortgage-ledger-interest"), 2, rowIndex);
        table.add(cell(moneyOrBlank(row.escrow(), row.hasStatement()), "mortgage-ledger-cell", "mortgage-ledger-money", "mortgage-ledger-escrow"), 3, rowIndex);
        table.add(cell(moneyOrBlank(row.outstandingDebt(), row.hasStatement()), "mortgage-ledger-cell", "mortgage-ledger-money", "mortgage-ledger-outstanding"), 4, rowIndex);
    }

    private void addTotals(GridPane table, int rowIndex, MortgageLedgerSnapshot snapshot) {
        table.add(cell("Total", "mortgage-ledger-total-cell"), 0, rowIndex);
        table.add(cell(Money.format(snapshot.totalDebtPaid()), "mortgage-ledger-total-cell", "mortgage-ledger-money", "mortgage-ledger-debt-paid-total"), 1, rowIndex);
        table.add(cell(Money.format(snapshot.totalInterest()), "mortgage-ledger-total-cell", "mortgage-ledger-money", "mortgage-ledger-interest-total"), 2, rowIndex);
        table.add(cell(Money.format(snapshot.totalEscrow()), "mortgage-ledger-total-cell", "mortgage-ledger-money", "mortgage-ledger-escrow-total"), 3, rowIndex);
        table.add(cell(Money.format(snapshot.endingOutstandingDebt()), "mortgage-ledger-total-cell", "mortgage-ledger-money", "mortgage-ledger-outstanding-total"), 4, rowIndex);

        Label paidTotal = cell(Money.format(snapshot.totalPaidIndicators()), "mortgage-ledger-total-paid", "mortgage-ledger-money");
        GridPane.setColumnSpan(paidTotal, 3);
        table.add(cell("Total Paid", "mortgage-ledger-total-paid-label"), 0, rowIndex + 1);
        table.add(paidTotal, 1, rowIndex + 1);
    }

    private String moneyOrBlank(double value, boolean hasStatement) {
        return hasStatement ? Money.format(value) : "";
    }

    private String columnStyle(int column, boolean total) {
        return switch (column) {
            case 1 -> total ? "mortgage-ledger-debt-paid-total" : "mortgage-ledger-debt-paid";
            case 2 -> total ? "mortgage-ledger-interest-total" : "mortgage-ledger-interest";
            case 3 -> total ? "mortgage-ledger-escrow-total" : "mortgage-ledger-escrow";
            case 4 -> total ? "mortgage-ledger-outstanding-total" : "mortgage-ledger-outstanding";
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
