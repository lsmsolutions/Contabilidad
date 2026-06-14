package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.application.vehiclelease.VehicleLeaseApplicationService;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class VehicleLeaseStatementView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VBox build(
        VehicleLeaseStatement statement,
        Predicate<String> fieldReviewed,
        BiConsumer<String, Boolean> fieldReviewChanged,
        Consumer<Boolean> allReviewChanged,
        Runnable editAction,
        Runnable deleteAction
    ) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("statement-card", "monthly-card");

        VBox identity = new VBox(3,
            title("Statement Date: " + date(statement.getStatementDate())),
            label("Due Date: " + date(statement.getDueDate()))
        );
        CheckBox reviewed = new CheckBox("Todo revisado");
        reviewed.setSelected(VehicleLeaseApplicationService.REVIEW_FIELDS.stream().allMatch(fieldReviewed));
        reviewed.setOnAction(event -> allReviewChanged.accept(reviewed.isSelected()));
        HBox header = new HBox(12, identity, reviewed);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identity, Priority.ALWAYS);

        HBox highlights = new HBox(12,
            highlight("Total Amount Due", Money.format(statement.getTotalAmountDue())),
            highlight("Due Date", date(statement.getDueDate())),
            highlight("Last Payment", Money.format(statement.getLastPaymentAmount()))
        );
        highlights.getStyleClass().add("statement-field-row");

        GridPane contract = section("Contract Details");
        addTextRow(contract, 1, "Payments Made", String.valueOf(statement.getPaymentsMade()));
        addTextRow(contract, 2, "Payments Remaining", String.valueOf(statement.getPaymentsRemaining()));
        addTextRow(contract, 3, "Last Payment Date", date(statement.getLastPaymentDate()));
        addMoneyRow(contract, 4, "last_payment_amount", "Last Payment Amount", statement.getLastPaymentAmount(), fieldReviewed, fieldReviewChanged);

        GridPane charges = section("Itemization of Total Amount Due");
        addMoneyRow(charges, 1, "lease_payment", "Lease Payment(s)", statement.getLeasePayment(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 2, "sales_use_tax", "Sales Use / Tax", statement.getSalesUseTax(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 3, "property_tax", "Property Tax", statement.getPropertyTax(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 4, "parking_tickets", "Parking Ticket(s)", statement.getParkingTickets(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 5, "returned_check_fees", "Returned Check Fee(s)", statement.getReturnedCheckFees(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 6, "miscellaneous_charges", "Miscellaneous Charge(s)", statement.getMiscellaneousCharges(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 7, "past_due_amount", "Past Due Amount", statement.getPastDueAmount(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 8, "late_charges", "Late Charge(s)", statement.getLateCharges(), fieldReviewed, fieldReviewChanged);
        addMoneyRow(charges, 9, "total_amount_due", "Total Amount Due", statement.getTotalAmountDue(), fieldReviewed, fieldReviewChanged);

        HBox body = new HBox(14, contract, charges);
        HBox.setHgrow(contract, Priority.ALWAYS);
        HBox.setHgrow(charges, Priority.ALWAYS);
        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> editAction.run());
        Button delete = new Button("Eliminar periodo");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteAction.run());
        Label status = new Label(statement.isPendingReview() ? "Pdte revision" : "OK | Revisado");
        status.getStyleClass().add(statement.isPendingReview() ? "status-pending" : "status-ok");
        HBox footer = new HBox(12, status, edit, delete);
        footer.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(header, highlights, body, footer);
        return card;
    }

    private GridPane section(String name) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("statement-section");
        Label title = new Label(name);
        title.getStyleClass().add("statement-section-title");
        grid.add(title, 0, 0, 3, 1);
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private void addTextRow(GridPane grid, int row, String name, String value) {
        grid.add(new Label(name), 0, row);
        Label amount = new Label(value);
        amount.getStyleClass().add("statement-money");
        grid.add(amount, 1, row);
    }

    private void addMoneyRow(
        GridPane grid,
        int row,
        String field,
        String name,
        double value,
        Predicate<String> reviewed,
        BiConsumer<String, Boolean> changed
    ) {
        grid.add(new Label(name), 0, row);
        Label amount = new Label(Money.format(value));
        amount.getStyleClass().add("statement-money");
        grid.add(amount, 1, row);
        CheckBox check = new CheckBox();
        check.setSelected(reviewed.test(field));
        check.setOnAction(event -> changed.accept(field, check.isSelected()));
        grid.add(check, 2, row);
    }

    private VBox highlight(String name, String value) {
        Label heading = label(name);
        Label amount = title(value);
        VBox box = new VBox(6, heading, amount);
        box.getStyleClass().add("statement-highlight");
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Label title(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("credit-info-title");
        return label;
    }

    private Label label(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("statement-field-label");
        return label;
    }

    private String date(LocalDate value) {
        return value == null ? "" : value.format(DATE);
    }
}
