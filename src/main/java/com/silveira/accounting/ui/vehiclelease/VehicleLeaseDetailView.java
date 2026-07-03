package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    public VBox build(
        VehicleLeaseAccount account,
        List<VehicleLeaseStatement> statements,
        Runnable backAction,
        Runnable importAction,
        ReviewLookup reviewLookup,
        Predicate<VehicleLeaseStatement> statementReviewed,
        FieldReviewAction fieldReviewAction,
        BiConsumer<VehicleLeaseStatement, Boolean> allReviewAction,
        Consumer<VehicleLeaseStatement> editAction,
        Consumer<VehicleLeaseStatement> saveAction,
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

        FlowPane monthlyCards = new FlowPane(12, 12);
        monthlyCards.getStyleClass().add("monthly-card-row");
        VBox statementCards = new VBox(16);
        statementCards.getStyleClass().add("statement-card-list");
        VehicleLeaseStatementView statementView = new VehicleLeaseStatementView();
        Map<Long, Label> reviewChips = new HashMap<>();
        for (VehicleLeaseStatement statement : statements) {
            MonthlyCard monthlyCard = monthlyCard(statement, statementReviewed.test(statement));
            VBox card = monthlyCard.node();
            reviewChips.put(statement.getId(), monthlyCard.reviewChip());
            card.setOnMouseClicked(event -> showStatement(
                statementCards,
                statementView,
                statement,
                reviewChips.get(statement.getId()),
                reviewLookup,
                fieldReviewAction,
                allReviewAction,
                editAction,
                saveAction,
                deleteAction
            ));
            monthlyCards.getChildren().add(card);
        }
        if (statements.isEmpty()) {
            statementCards.getChildren().add(new Label("No statements imported for this vehicle."));
        } else {
            showStatement(
                statementCards,
                statementView,
                statements.get(0),
                reviewChips.get(statements.get(0).getId()),
                reviewLookup,
                fieldReviewAction,
                allReviewAction,
                editAction,
                saveAction,
                deleteAction
            );
        }

        VBox page = new VBox(18, heading, actions, contractInfo, monthlyCards, statementCards);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        VBox.setVgrow(statementCards, Priority.ALWAYS);
        return page;
    }

    private MonthlyCard monthlyCard(VehicleLeaseStatement statement, boolean reviewed) {
        Label heading = new Label(statement.getStatementDate() == null ? "Statement" : statement.getStatementDate().format(MONTH));
        heading.getStyleClass().add("monthly-card-title");
        HBox title = new HBox(8, heading);
        title.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Label check = new Label("\u2713");
        check.getStyleClass().add("statement-reviewed-chip");
        setReviewChipVisible(check, reviewed);
        title.getChildren().add(check);
        GridPane details = new GridPane();
        details.getStyleClass().add("monthly-card-grid");
        addLine(details, 0, "Statement Date", date(statement.getStatementDate()));
        addLine(details, 1, "Due Date", date(statement.getDueDate()));
        addLine(details, 2, "Total Due", Money.format(statement.getTotalAmountDue()));
        addLine(details, 3, "Payments Made", String.valueOf(statement.getPaymentsMade()));
        addLine(details, 4, "Remaining", String.valueOf(statement.getPaymentsRemaining()));
        VBox card = new VBox(0, title, details);
        card.getStyleClass().add("monthly-card");
        return new MonthlyCard(card, check);
    }

    private void showStatement(
        VBox statementCards,
        VehicleLeaseStatementView statementView,
        VehicleLeaseStatement statement,
        Label reviewChip,
        ReviewLookup reviewLookup,
        FieldReviewAction fieldReviewAction,
        BiConsumer<VehicleLeaseStatement, Boolean> allReviewAction,
        Consumer<VehicleLeaseStatement> editAction,
        Consumer<VehicleLeaseStatement> saveAction,
        Consumer<VehicleLeaseStatement> deleteAction
    ) {
        Predicate<String> reviewed = field -> reviewLookup.isReviewed(statement, field);
        statementCards.getChildren().setAll(statementView.build(
            statement,
            reviewed,
            (field, value) -> {
                fieldReviewAction.setReviewed(statement, field, value);
                setReviewChipVisible(reviewChip, !statement.isPendingReview());
            },
            value -> {
                allReviewAction.accept(statement, value);
                setReviewChipVisible(reviewChip, !statement.isPendingReview());
            },
            () -> editAction.accept(statement),
            () -> saveAction.accept(statement),
            () -> deleteAction.accept(statement)
        ));
    }

    private void setReviewChipVisible(Label reviewChip, boolean reviewed) {
        reviewChip.setVisible(reviewed);
        reviewChip.setManaged(reviewed);
    }

    private void addLine(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(valueText);
        value.getStyleClass().add("monthly-card-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private String vehicleName(VehicleLeaseAccount account) {
        return (account.getVehicleYear() > 0 ? account.getVehicleYear() + " " : "")
            + text(account.getMake()) + " " + text(account.getModel()) + " " + text(account.getTrim());
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String date(LocalDate value) {
        return value == null ? "" : value.format(DATE);
    }

    @FunctionalInterface
    public interface ReviewLookup {
        boolean isReviewed(VehicleLeaseStatement statement, String field);
    }

    @FunctionalInterface
    public interface FieldReviewAction {
        void setReviewed(VehicleLeaseStatement statement, String field, boolean reviewed);
    }

    private record MonthlyCard(VBox node, Label reviewChip) {
    }
}
