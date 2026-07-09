package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.application.vehiclelease.dto.VehicleLeaseLedgerRow;
import com.silveira.accounting.application.vehiclelease.dto.VehicleLeaseLedgerSnapshot;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class VehicleLeaseLedgerView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VBox build(
        VehicleLeaseLedgerSnapshot snapshot,
        Consumer<String> accountChanged,
        IntConsumer yearChanged,
        Runnable backAction
    ) {
        Label heading = new Label("Vehicle Ledger - " + snapshot.year());
        heading.getStyleClass().add("heading");

        ComboBox<VehicleLeaseAccount> account = new ComboBox<>();
        account.getItems().setAll(snapshot.accounts());
        snapshot.accounts().stream()
            .filter(candidate -> snapshot.selectedAccount().equals(candidate.getAlias()))
            .findFirst()
            .ifPresent(account::setValue);
        account.getStyleClass().add("vehicle-ledger-combo");
        account.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(VehicleLeaseAccount value) {
                return value == null ? "" : vehicleName(value);
            }

            @Override
            public VehicleLeaseAccount fromString(String value) {
                return null;
            }
        });
        account.setOnAction(event -> {
            VehicleLeaseAccount selected = account.getValue();
            if (selected != null && selected.getAlias() != null && !selected.getAlias().isBlank()) {
                accountChanged.accept(selected.getAlias());
            }
        });

        ComboBox<Integer> year = new ComboBox<>();
        year.getItems().setAll(snapshot.availableYears());
        year.setValue(snapshot.year());
        year.getStyleClass().add("vehicle-ledger-year");
        year.setOnAction(event -> {
            Integer selected = year.getValue();
            if (selected != null) {
                yearChanged.accept(selected);
            }
        });

        Button back = new Button("\u2190 Vehicle Leases");
        back.getStyleClass().add("back-button");
        back.setOnAction(event -> backAction.run());

        VBox controls = new VBox(10, new HBox(10, new Label("Vehicle"), account, new Label("Year"), year));
        controls.getStyleClass().add("vehicle-ledger-controls");

        VBox content = new VBox(18, heading, back, controls, ledgerTable(snapshot));
        content.setPadding(new Insets(28));
        content.getStyleClass().addAll("page", "vehicle-ledger-page");
        return content;
    }

    private VBox ledgerTable(VehicleLeaseLedgerSnapshot snapshot) {
        GridPane table = new GridPane();
        table.getStyleClass().add("vehicle-ledger-table");
        double[] widths = {17, 17, 17, 17, 16, 16};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            table.getColumnConstraints().add(column);
        }
        addHeader(table);
        int rowIndex = 1;
        for (VehicleLeaseLedgerRow row : snapshot.rows()) {
            addRow(table, rowIndex++, row);
        }
        addTotals(table, rowIndex, snapshot);
        VBox section = new VBox(8, table);
        section.getStyleClass().add("vehicle-ledger-section");
        return section;
    }

    private void addHeader(GridPane table) {
        String[] headers = {"Month", "Statement Date", "Due Date", "Total Due", "Payments Made", "Remaining"};
        for (int column = 0; column < headers.length; column++) {
            Label label = cell(headers[column], "vehicle-ledger-header", columnStyle(column, false));
            if (column > 2) {
                label.getStyleClass().add("vehicle-ledger-number-header");
            }
            table.add(label, column, 0);
        }
    }

    private void addRow(GridPane table, int rowIndex, VehicleLeaseLedgerRow row) {
        table.add(cell(Month.of(row.month()).getDisplayName(TextStyle.FULL, Locale.ENGLISH), "vehicle-ledger-cell"), 0, rowIndex);
        table.add(cell(dateOrBlank(row.statementDate()), "vehicle-ledger-cell", "vehicle-ledger-statement-date"), 1, rowIndex);
        table.add(cell(dateOrBlank(row.dueDate()), "vehicle-ledger-cell", "vehicle-ledger-due-date"), 2, rowIndex);
        table.add(cell(moneyOrBlank(row.totalDue(), row.hasStatement()), "vehicle-ledger-cell", "vehicle-ledger-money", "vehicle-ledger-total-due"), 3, rowIndex);
        table.add(cell(numberOrBlank(row.paymentsMade(), row.hasStatement()), "vehicle-ledger-cell", "vehicle-ledger-money", "vehicle-ledger-payments-made"), 4, rowIndex);
        table.add(cell(numberOrBlank(row.paymentsRemaining(), row.hasStatement()), "vehicle-ledger-cell", "vehicle-ledger-money", "vehicle-ledger-remaining"), 5, rowIndex);
    }

    private void addTotals(GridPane table, int rowIndex, VehicleLeaseLedgerSnapshot snapshot) {
        table.add(cell("Total", "vehicle-ledger-total-cell"), 0, rowIndex);
        table.add(cell("", "vehicle-ledger-total-cell"), 1, rowIndex);
        table.add(cell("", "vehicle-ledger-total-cell"), 2, rowIndex);
        table.add(cell(Money.format(snapshot.totalDue()), "vehicle-ledger-total-cell", "vehicle-ledger-money", "vehicle-ledger-total-due-total"), 3, rowIndex);
        table.add(cell(String.valueOf(snapshot.latestPaymentsMade()), "vehicle-ledger-total-cell", "vehicle-ledger-money", "vehicle-ledger-payments-made-total"), 4, rowIndex);
        table.add(cell(String.valueOf(snapshot.latestPaymentsRemaining()), "vehicle-ledger-total-cell", "vehicle-ledger-money", "vehicle-ledger-remaining-total"), 5, rowIndex);
    }

    private String dateOrBlank(LocalDate value) {
        return value == null ? "" : value.format(DATE);
    }

    private String moneyOrBlank(double value, boolean hasStatement) {
        return hasStatement ? Money.format(value) : "";
    }

    private String numberOrBlank(int value, boolean hasStatement) {
        return hasStatement ? String.valueOf(value) : "";
    }

    private String columnStyle(int column, boolean total) {
        return switch (column) {
            case 1 -> "vehicle-ledger-statement-date";
            case 2 -> "vehicle-ledger-due-date";
            case 3 -> total ? "vehicle-ledger-total-due-total" : "vehicle-ledger-total-due";
            case 4 -> total ? "vehicle-ledger-payments-made-total" : "vehicle-ledger-payments-made";
            case 5 -> total ? "vehicle-ledger-remaining-total" : "vehicle-ledger-remaining";
            default -> "";
        };
    }

    private String vehicleName(VehicleLeaseAccount account) {
        return (account.getVehicleYear() > 0 ? account.getVehicleYear() + " " : "")
            + text(account.getMake()) + " " + text(account.getModel()) + " " + text(account.getTrim());
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private Label cell(String text, String... styleClasses) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().addAll(styleClasses);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
