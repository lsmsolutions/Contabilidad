package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.utils.Money;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class VehicleLeaseDetailView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VBox build(
        VehicleLeaseAccount account,
        List<VehicleLeaseStatement> statements,
        Runnable backAction,
        Runnable importAction,
        ReviewLookup reviewLookup,
        FieldReviewAction fieldReviewAction,
        BiConsumer<VehicleLeaseStatement, Boolean> allReviewAction,
        Consumer<VehicleLeaseStatement> editAction,
        Consumer<VehicleLeaseStatement> deleteAction
    ) {
        Label heading = new Label(vehicleName(account));
        heading.getStyleClass().add("heading");
        Button back = new Button("\u2190 Vehicle Leases");
        back.getStyleClass().add("back-button");
        back.setOnAction(event -> backAction.run());
        Button importPdf = new Button("Import PDF");
        importPdf.getStyleClass().add("primary");
        importPdf.setOnAction(event -> importAction.run());
        HBox actions = new HBox(10, back, importPdf);

        GridPane contractInfo = new GridPane();
        contractInfo.getStyleClass().add("statement-section");
        contractInfo.setHgap(16);
        contractInfo.setVgap(8);
        Label contractTitle = new Label("Contract Details");
        contractTitle.getStyleClass().add("statement-section-title");
        contractInfo.add(contractTitle, 0, 0, 4, 1);
        contractInfo.add(new Label("Provider"), 0, 1);
        contractInfo.add(new Label(text(account.getProviderName())), 1, 1);
        contractInfo.add(new Label("Account"), 2, 1);
        contractInfo.add(new Label(text(account.getAccountNumber())), 3, 1);
        contractInfo.add(new Label("VIN"), 0, 2);
        contractInfo.add(new Label(text(account.getVin())), 1, 2, 3, 1);

        VehicleLeaseStatement latest = statements.isEmpty() ? null : statements.get(0);
        FlowPane totals = new FlowPane(12, 12);
        totals.getStyleClass().add("totals-panel");
        totals.getChildren().addAll(
            total("Monthly Payment", latest == null ? "$0.00" : Money.format(latest.getTotalAmountDue()), "neutral-total"),
            total("Payments Made", latest == null ? "0" : String.valueOf(latest.getPaymentsMade()), "income-total"),
            total("Payments Remaining", latest == null ? "0" : String.valueOf(latest.getPaymentsRemaining()), "pending-total"),
            total("Maturity Date", account.getMaturityDate() == null ? "" : account.getMaturityDate().format(DATE), "expense-total")
        );

        VBox statementCards = new VBox(16);
        statementCards.getStyleClass().add("statement-card-list");
        VehicleLeaseStatementView statementView = new VehicleLeaseStatementView();
        for (VehicleLeaseStatement statement : statements) {
            Predicate<String> reviewed = field -> reviewLookup.isReviewed(statement, field);
            statementCards.getChildren().add(statementView.build(
                statement,
                reviewed,
                (field, value) -> fieldReviewAction.setReviewed(statement, field, value),
                value -> allReviewAction.accept(statement, value),
                () -> editAction.accept(statement),
                () -> deleteAction.accept(statement)
            ));
        }
        if (statements.isEmpty()) {
            statementCards.getChildren().add(new Label("No statements imported for this vehicle."));
        }

        VBox page = new VBox(18, heading, actions, contractInfo, totals, statementCards);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        VBox.setVgrow(statementCards, Priority.ALWAYS);
        return page;
    }

    private VBox total(String name, String value, String style) {
        Label label = new Label(name);
        label.getStyleClass().add("total-label");
        Label amount = new Label(value);
        amount.getStyleClass().add("total-value");
        VBox box = new VBox(6, label, amount);
        box.getStyleClass().addAll("total-card", style);
        return box;
    }

    private String vehicleName(VehicleLeaseAccount account) {
        return (account.getVehicleYear() > 0 ? account.getVehicleYear() + " " : "")
            + text(account.getMake()) + " " + text(account.getModel()) + " " + text(account.getTrim());
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    @FunctionalInterface
    public interface ReviewLookup {
        boolean isReviewed(VehicleLeaseStatement statement, String field);
    }

    @FunctionalInterface
    public interface FieldReviewAction {
        void setReviewed(VehicleLeaseStatement statement, String field, boolean reviewed);
    }
}
