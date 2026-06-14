package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.utils.Money;
import java.util.Locale;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class VehicleLeaseStatementEditDialogView {
    public Optional<VehicleLeaseStatement> show(VehicleLeaseStatement statement) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit vehicle lease statement");
        DatePicker statementDate = new DatePicker(statement.getStatementDate());
        DatePicker dueDate = new DatePicker(statement.getDueDate());
        DatePicker lastPaymentDate = new DatePicker(statement.getLastPaymentDate());
        TextField totalDue = money(statement.getTotalAmountDue());
        TextField lastPayment = money(statement.getLastPaymentAmount());
        TextField paymentsMade = new TextField(String.valueOf(statement.getPaymentsMade()));
        TextField paymentsRemaining = new TextField(String.valueOf(statement.getPaymentsRemaining()));
        TextField leasePayment = money(statement.getLeasePayment());
        TextField salesTax = money(statement.getSalesUseTax());
        TextField propertyTax = money(statement.getPropertyTax());
        TextField parkingTickets = money(statement.getParkingTickets());
        TextField returnedCheckFees = money(statement.getReturnedCheckFees());
        TextField miscellaneous = money(statement.getMiscellaneousCharges());
        TextField pastDue = money(statement.getPastDueAmount());
        TextField lateCharges = money(statement.getLateCharges());
        TextArea notes = new TextArea(statement.getReviewNotes() == null ? "" : statement.getReviewNotes());
        notes.setPrefRowCount(2);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        int row = 0;
        form.addRow(row++, new Label("Statement Date"), statementDate);
        form.addRow(row++, new Label("Due Date"), dueDate);
        form.addRow(row++, new Label("Total Amount Due"), totalDue);
        form.addRow(row++, new Label("Last Payment Date"), lastPaymentDate);
        form.addRow(row++, new Label("Last Payment Amount"), lastPayment);
        form.addRow(row++, new Label("Payments Made"), paymentsMade);
        form.addRow(row++, new Label("Payments Remaining"), paymentsRemaining);
        form.addRow(row++, new Label("Lease Payment"), leasePayment);
        form.addRow(row++, new Label("Sales Use / Tax"), salesTax);
        form.addRow(row++, new Label("Property Tax"), propertyTax);
        form.addRow(row++, new Label("Parking Tickets"), parkingTickets);
        form.addRow(row++, new Label("Returned Check Fees"), returnedCheckFees);
        form.addRow(row++, new Label("Miscellaneous Charges"), miscellaneous);
        form.addRow(row++, new Label("Past Due Amount"), pastDue);
        form.addRow(row++, new Label("Late Charges"), lateCharges);
        form.addRow(row, new Label("Notes"), notes);
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(540);
        scroll.setPrefViewportHeight(560);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        return dialog.showAndWait().filter(ButtonType.OK::equals).map(result -> {
            statement.setStatementDate(statementDate.getValue());
            statement.setDueDate(dueDate.getValue());
            statement.setTotalAmountDue(Money.parse(totalDue.getText()));
            statement.setLastPaymentDate(lastPaymentDate.getValue());
            statement.setLastPaymentAmount(Money.parse(lastPayment.getText()));
            statement.setPaymentsMade(integer(paymentsMade.getText()));
            statement.setPaymentsRemaining(integer(paymentsRemaining.getText()));
            statement.setLeasePayment(Money.parse(leasePayment.getText()));
            statement.setSalesUseTax(Money.parse(salesTax.getText()));
            statement.setPropertyTax(Money.parse(propertyTax.getText()));
            statement.setParkingTickets(Money.parse(parkingTickets.getText()));
            statement.setReturnedCheckFees(Money.parse(returnedCheckFees.getText()));
            statement.setMiscellaneousCharges(Money.parse(miscellaneous.getText()));
            statement.setPastDueAmount(Money.parse(pastDue.getText()));
            statement.setLateCharges(Money.parse(lateCharges.getText()));
            statement.setReviewNotes(notes.getText());
            return statement;
        });
    }

    private TextField money(double value) {
        return new TextField(String.format(Locale.US, "%.2f", value));
    }

    private int integer(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
    }
}
