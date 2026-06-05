package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
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

public class BestBuyStatementSummaryView {
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
        "minimum_payment_due",
        "payment_due_date",
        "statement_end_date",
        "next_closing_date",
        "credit_limit",
        "available_credit"
    );

    public VBox build(
        CreditCardStatement statement,
        List<CreditCardTransaction> transactions,
        Predicate<String> fieldReviewed,
        BiConsumer<String, Boolean> fieldReviewedChanged,
        Consumer<Boolean> allReviewedChanged,
        BiConsumer<CreditCardTransaction, Boolean> transactionReviewedChanged,
        Runnable editAction
    ) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("statement-card", "monthly-card");

        CheckBox reviewed = new CheckBox("Todo revisado");
        reviewed.setSelected(FIELDS.stream().allMatch(fieldReviewed) && transactions.stream().noneMatch(CreditCardTransaction::isPendingReview));
        reviewed.setOnAction(event -> allReviewedChanged.accept(reviewed.isSelected()));

        HBox header = new HBox(12, identity(statement), reviewed);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        HBox top = new HBox(14, accountSummary(statement, fieldReviewed, fieldReviewedChanged), paymentInformation(statement, fieldReviewed, fieldReviewedChanged));
        top.getStyleClass().add("statement-body-row");

        HBox bottom = new HBox(14, creditInformation(statement, fieldReviewed, fieldReviewedChanged), transactionsPanel(transactions, transactionReviewedChanged));
        bottom.getStyleClass().add("statement-body-row");

        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> editAction.run());
        HBox footer = new HBox(12, status(statement), edit);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(0), Priority.ALWAYS);

        card.getChildren().addAll(header, top, bottom, footer);
        return card;
    }

    public List<String> fieldKeys(CreditCardStatement statement) {
        return FIELDS;
    }

    private VBox identity(CreditCardStatement statement) {
        Label title = new Label(text(statement.getCardName()).isBlank() ? "My Best Buy Credit Card" : text(statement.getCardName()));
        title.getStyleClass().add("credit-info-title");
        Label subtitle = new Label("Account number ending in: " + text(statement.getAccountLastDigits()));
        subtitle.getStyleClass().add("statement-field-label");
        VBox box = new VBox(3, title, subtitle);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private GridPane accountSummary(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        GridPane grid = section("Summary of Account Activity");
        addMoneyRow(grid, 1, "previous_balance", "Previous Balance", "", statement.getPreviousBalance(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 2, "payments", "Payments", "-", statement.getPayments(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 3, "other_credits", "Other Credits", "-", statement.getOtherCredits(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 4, "transactions", "Purchases/Other Debits", "+", statement.getTransactions(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 5, "cash_advances", "Cash Advances", "+", statement.getCashAdvances(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 6, "fees_charged", "Fees Charged", "+", statement.getFeesCharged(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 7, "interest_charged", "Interest Charged", "+", statement.getInterestCharged(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 8, "new_balance", "New Balance", "", statement.getNewBalance(), fieldReviewed, fieldReviewedChanged);
        return grid;
    }

    private GridPane paymentInformation(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        GridPane grid = section("Payment Information");
        addMoneyRow(grid, 1, "new_balance", "New Balance", "", statement.getNewBalance(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 2, "minimum_payment_due", "Minimum Payment Due", "", statement.getMinimumPaymentDue(), fieldReviewed, fieldReviewedChanged);
        addTextRow(grid, 3, "payment_due_date", "Payment Due Date", formatShortDate(statement.getPaymentDueDate()), fieldReviewed, fieldReviewedChanged);
        return grid;
    }

    private GridPane creditInformation(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        GridPane grid = section("Credit Information");
        addMoneyRow(grid, 1, "credit_limit", "Credit Limit", "", statement.getCreditLimit(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 2, "available_credit", "Available Credit", "", statement.getAvailableCredit(), fieldReviewed, fieldReviewedChanged);
        addTextRow(grid, 3, "statement_end_date", "Statement Closing Date", formatDate(statement.getStatementEndDate()), fieldReviewed, fieldReviewedChanged);
        addTextRow(grid, 4, "next_closing_date", "Next Statement Closing Date", formatDate(statement.getNextClosingDate()), fieldReviewed, fieldReviewedChanged);
        return grid;
    }

    private GridPane transactionsPanel(List<CreditCardTransaction> transactions, BiConsumer<CreditCardTransaction, Boolean> transactionReviewedChanged) {
        GridPane grid = section("Transactions");
        addHeader(grid, 1, "Date", "Description", "Amount", "Revisado");
        int row = 2;
        for (CreditCardTransaction transaction : transactions) {
            Label date = label(formatShortDate(transaction.getTransactionDate()), "statement-readonly-value");
            Label description = label(text(transaction.getDescription()), "statement-bilingual-label");
            Label amount = label(Money.format(transaction.getAmount()), "statement-readonly-value");
            CheckBox reviewed = new CheckBox();
            reviewed.setSelected(!transaction.isPendingReview());
            reviewed.setOnAction(event -> transactionReviewedChanged.accept(transaction, reviewed.isSelected()));
            grid.add(date, 0, row);
            grid.add(description, 1, row);
            grid.add(amount, 2, row);
            grid.add(reviewed, 3, row++);
        }
        if (transactions.isEmpty()) {
            grid.add(label("Sin movimientos guardados", "statement-bilingual-label"), 0, row, 4, 1);
        }
        return grid;
    }

    private GridPane section(String title) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("statement-section");
        grid.setHgap(9);
        grid.setVgap(7);
        ColumnConstraints label = new ColumnConstraints();
        label.setHgrow(Priority.ALWAYS);
        label.setMinWidth(150);
        ColumnConstraints sign = new ColumnConstraints();
        sign.setHalignment(HPos.CENTER);
        sign.setMinWidth(22);
        ColumnConstraints value = new ColumnConstraints();
        value.setHalignment(HPos.RIGHT);
        value.setMinWidth(112);
        ColumnConstraints review = new ColumnConstraints();
        review.setHalignment(HPos.CENTER);
        review.setMinWidth(76);
        grid.getColumnConstraints().setAll(label, sign, value, review);
        Label heading = new Label(title);
        heading.getStyleClass().add("statement-section-title");
        grid.add(heading, 0, 0, 4, 1);
        return grid;
    }

    private void addHeader(GridPane grid, int row, String left, String description, String amount, String reviewed) {
        grid.add(label(left, "statement-field-label"), 0, row);
        grid.add(label(description, "statement-field-label"), 1, row);
        grid.add(label(amount, "statement-field-label"), 2, row);
        grid.add(label(reviewed, "statement-field-label"), 3, row);
    }

    private void addMoneyRow(GridPane grid, int row, String fieldName, String label, String sign, double amount, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        addTextRow(grid, row, fieldName, label, sign, Money.format(amount), fieldReviewed, fieldReviewedChanged);
    }

    private void addTextRow(GridPane grid, int row, String fieldName, String label, String value, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        addTextRow(grid, row, fieldName, label, "", value, fieldReviewed, fieldReviewedChanged);
    }

    private void addTextRow(GridPane grid, int row, String fieldName, String label, String sign, String value, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        grid.add(label(label, "statement-bilingual-label"), 0, row);
        grid.add(label(sign, "statement-readonly-value"), 1, row);
        grid.add(label(value, "statement-readonly-value"), 2, row);
        CheckBox reviewed = new CheckBox();
        reviewed.setSelected(fieldReviewed.test(fieldName));
        reviewed.setOnAction(event -> fieldReviewedChanged.accept(fieldName, reviewed.isSelected()));
        grid.add(reviewed, 3, row);
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private Node status(CreditCardStatement statement) {
        String notes = text(statement.getReviewNotes());
        String text = statement.isPendingReview() ? "Pdte revision" : "OK";
        if (!notes.isBlank()) {
            text += " | " + notes;
        }
        Label status = new Label(text);
        status.getStyleClass().add(statement.isPendingReview() ? "statement-pending-chip" : "statement-reviewed-chip");
        return status;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
