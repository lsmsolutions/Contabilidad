package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CitiStatementSummaryView {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final List<String> FIELDS = List.of(
        "previous_balance",
        "payments",
        "other_credits",
        "transactions",
        "cash_advances",
        "fees_charged",
        "interest_charged",
        "new_balance",
        "credit_limit",
        "available_credit",
        "cash_advance_limit"
    );

    public VBox build(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged, Consumer<Boolean> allReviewedChanged, Runnable editAction) {
        VBox card = new VBox(13);
        card.getStyleClass().addAll("citi-statement-card", "monthly-card");

        CheckBox reviewed = new CheckBox("Todo revisado");
        reviewed.setSelected(FIELDS.stream().allMatch(fieldReviewed));
        reviewed.setOnAction(event -> allReviewedChanged.accept(reviewed.isSelected()));

        HBox header = new HBox(12, identity(statement), reviewed);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        HBox top = new HBox(12,
            paymentBox("New balance as of " + formatShortDate(statement.getStatementEndDate()), Money.format(statement.getNewBalance()), "citi-balance"),
            paymentBox("Minimum payment due", Money.format(statement.getMinimumPaymentDue()), "citi-minimum"),
            paymentBox("Payment due date", formatShortDate(statement.getPaymentDueDate()), "citi-date")
        );

        GridPane account = section("Account Summary");
        addRow(account, 1, "previous_balance", "Previous balance", "", statement.getPreviousBalance(), fieldReviewed, fieldReviewedChanged);
        addRow(account, 2, "payments", "Payments", "-", statement.getPayments(), fieldReviewed, fieldReviewedChanged);
        addRow(account, 3, "other_credits", "Credits", "-", statement.getOtherCredits(), fieldReviewed, fieldReviewedChanged);
        addRow(account, 4, "transactions", "Purchases", "+", statement.getTransactions(), fieldReviewed, fieldReviewedChanged);
        addRow(account, 5, "cash_advances", "Cash advances", "+", statement.getCashAdvances(), fieldReviewed, fieldReviewedChanged);
        addRow(account, 6, "fees_charged", "Fees", "+", statement.getFeesCharged(), fieldReviewed, fieldReviewedChanged);
        addRow(account, 7, "interest_charged", "Interest", "+", statement.getInterestCharged(), fieldReviewed, fieldReviewedChanged);
        addRow(account, 8, "new_balance", "New balance", "", statement.getNewBalance(), fieldReviewed, fieldReviewedChanged);

        GridPane credit = section("Credit Limit");
        addRow(credit, 1, "credit_limit", "Credit Limit", "", statement.getCreditLimit(), fieldReviewed, fieldReviewedChanged);
        addRow(credit, 2, "available_credit", "Available Credit Limit", "", statement.getAvailableCredit(), fieldReviewed, fieldReviewedChanged);
        addRow(credit, 3, "cash_advance_limit", "Includes cash advance limit", "", statement.getCashAdvanceLimit(), fieldReviewed, fieldReviewedChanged);

        HBox body = new HBox(14, account, credit);
        HBox.setHgrow(account, Priority.ALWAYS);
        HBox.setHgrow(credit, Priority.ALWAYS);

        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> editAction.run());
        HBox footer = new HBox(12, status(statement), edit);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(0), Priority.ALWAYS);

        card.getChildren().addAll(header, top, body, footer);
        return card;
    }

    public List<String> fieldKeys(CreditCardStatement statement) {
        return FIELDS;
    }

    private VBox identity(CreditCardStatement statement) {
        Label title = new Label(text(statement.getCardName()).isBlank() ? "Citi Card" : text(statement.getCardName()));
        title.getStyleClass().add("citi-title");
        Label subtitle = new Label("Billing Period: " + formatShortDate(statement.getStatementStartDate()) + "-" + formatShortDate(statement.getStatementEndDate())
            + " | Account number ending in: " + text(statement.getAccountLastDigits()));
        subtitle.getStyleClass().add("citi-subtitle");
        VBox box = new VBox(3, title, subtitle);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox paymentBox(String title, String value, String styleClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("citi-payment-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("citi-payment-value", styleClass);
        VBox box = new VBox(4, titleLabel, valueLabel);
        box.getStyleClass().add("citi-payment-box");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private GridPane section(String title) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("citi-section-grid");
        grid.setHgap(9);
        grid.setVgap(7);
        ColumnConstraints label = new ColumnConstraints();
        label.setHgrow(Priority.ALWAYS);
        ColumnConstraints sign = new ColumnConstraints();
        sign.setHalignment(HPos.CENTER);
        sign.setMinWidth(22);
        ColumnConstraints value = new ColumnConstraints();
        value.setHalignment(HPos.RIGHT);
        value.setMinWidth(118);
        ColumnConstraints review = new ColumnConstraints();
        review.setHalignment(HPos.CENTER);
        review.setMinWidth(76);
        grid.getColumnConstraints().setAll(label, sign, value, review);
        Label heading = new Label(title);
        heading.getStyleClass().add("citi-section-title");
        grid.add(heading, 0, 0, 4, 1);
        Label reviewTitle = new Label("Revisado");
        reviewTitle.getStyleClass().add("citi-review-title");
        grid.add(reviewTitle, 3, 0);
        return grid;
    }

    private void addRow(GridPane grid, int row, String fieldName, String label, String sign, double amount, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("citi-row-label");
        Label signNode = new Label(sign);
        signNode.getStyleClass().add("citi-row-sign");
        Label amountNode = new Label(Money.format(amount));
        amountNode.getStyleClass().add("citi-row-value");
        CheckBox check = new CheckBox();
        check.setSelected(fieldReviewed.test(fieldName));
        check.setOnAction(event -> fieldReviewedChanged.accept(fieldName, check.isSelected()));
        grid.add(labelNode, 0, row);
        grid.add(signNode, 1, row);
        grid.add(amountNode, 2, row);
        grid.add(check, 3, row);
    }

    private Node status(CreditCardStatement statement) {
        String notes = text(statement.getReviewNotes());
        String text = statement.isPendingReview() ? "Pdte revision" : "OK";
        if (!notes.isBlank()) {
            text += " | " + notes;
        }
        Label status = new Label(text);
        status.getStyleClass().add(statement.isPendingReview() ? "statement-status-pending" : "statement-status-ok");
        return status;
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
