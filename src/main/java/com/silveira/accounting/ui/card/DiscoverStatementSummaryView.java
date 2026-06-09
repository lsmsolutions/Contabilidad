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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DiscoverStatementSummaryView {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final List<String> FIELDS = List.of(
        "previous_balance",
        "payments",
        "transactions",
        "balance_transfers",
        "cash_advances",
        "fees_charged",
        "interest_charged",
        "new_balance",
        "minimum_payment_due",
        "payment_due_date",
        "credit_limit",
        "available_credit",
        "cash_advance_limit",
        "available_cash_advance_credit"
    );

    public VBox build(
        CreditCardStatement statement,
        List<CreditCardTransaction> transactions,
        Predicate<String> fieldReviewed,
        BiConsumer<String, Boolean> fieldReviewedChanged,
        Consumer<Boolean> allReviewedChanged,
        BiConsumer<CreditCardTransaction, Boolean> transactionReviewedChanged,
        Runnable addTransactionAction,
        Runnable editAction
    ) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("discover-statement-card", "monthly-card");

        CheckBox reviewed = new CheckBox("Todo revisado");
        reviewed.setSelected(FIELDS.stream().allMatch(fieldReviewed) && transactions.stream().noneMatch(CreditCardTransaction::isPendingReview));
        reviewed.setOnAction(event -> allReviewedChanged.accept(reviewed.isSelected()));

        HBox titleRow = new HBox(12, title(statement), reviewed);
        titleRow.getStyleClass().add("discover-title-row");
        HBox.setHgrow(titleRow.getChildren().get(0), Priority.ALWAYS);

        HBox topRow = new HBox(14, paymentPanel(statement, fieldReviewed, fieldReviewedChanged), accountSummaryPanel(statement, fieldReviewed, fieldReviewedChanged));
        topRow.getStyleClass().add("discover-top-row");

        HBox lowerRow = new HBox(14, creditLinePanel(statement, fieldReviewed, fieldReviewedChanged), transactionsPanel(transactions, transactionReviewedChanged, addTransactionAction));
        lowerRow.getStyleClass().add("discover-lower-row");

        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> editAction.run());
        HBox footer = new HBox(12, status(statement), edit);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(0), Priority.ALWAYS);

        card.getChildren().addAll(titleRow, topRow, lowerRow, footer);
        return card;
    }

    public List<String> fieldKeys(CreditCardStatement statement) {
        return FIELDS;
    }

    private VBox title(CreditCardStatement statement) {
        Label title = new Label(text(statement.getCardName()).isBlank() ? "Discover Card" : text(statement.getCardName()));
        title.getStyleClass().add("credit-info-title");
        Label period = new Label("Statement Period: " + formatShortDate(statement.getStatementStartDate()) + " - " + formatShortDate(statement.getStatementEndDate())
            + " | Account ending in " + text(statement.getAccountLastDigits()));
        period.getStyleClass().add("statement-field-label");
        VBox box = new VBox(3, title, period);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox accountSummaryPanel(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        VBox panel = panel("Account Summary", "");
        VBox rows = new VBox();
        rows.getStyleClass().add("discover-line-list");
        rows.getChildren().addAll(
            line("previous_balance", "Previous Balance", "", statement.getPreviousBalance(), fieldReviewed, fieldReviewedChanged, false),
            line("payments", "Payments and Credits", "-", statement.getPayments(), fieldReviewed, fieldReviewedChanged, false),
            line("transactions", "Purchases", "+", statement.getTransactions(), fieldReviewed, fieldReviewedChanged, false),
            line("balance_transfers", "Balance Transfers", "+", statement.getBalanceTransfers(), fieldReviewed, fieldReviewedChanged, false),
            line("cash_advances", "Cash Advances", "+", statement.getCashAdvances(), fieldReviewed, fieldReviewedChanged, false),
            line("fees_charged", "Fees Charged", "+", statement.getFeesCharged(), fieldReviewed, fieldReviewedChanged, false),
            line("interest_charged", "Interest Charged", "+", statement.getInterestCharged(), fieldReviewed, fieldReviewedChanged, false),
            line("new_balance", "New Balance", "=", statement.getNewBalance(), fieldReviewed, fieldReviewedChanged, true)
        );
        panel.getChildren().add(rows);
        return panel;
    }

    private VBox paymentPanel(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        VBox panel = panel("Payment Information", "");
        panel.getStyleClass().add("discover-payment-panel");
        GridPane grid = new GridPane();
        grid.getStyleClass().add("discover-payment-grid");
        paymentCell(grid, 0, "minimum_payment_due", "Minimum Payment Due", Money.format(statement.getMinimumPaymentDue()), fieldReviewed, fieldReviewedChanged);
        paymentCell(grid, 1, "new_balance", "New Balance", Money.format(statement.getNewBalance()), fieldReviewed, fieldReviewedChanged);
        paymentCell(grid, 2, "payment_due_date", "Payment Due Date", formatShortDate(statement.getPaymentDueDate()), fieldReviewed, fieldReviewedChanged);
        panel.getChildren().add(grid);
        return panel;
    }

    private VBox creditLinePanel(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        VBox panel = panel("Credit Line", "");
        VBox rows = new VBox();
        rows.getStyleClass().add("discover-line-list");
        rows.getChildren().addAll(
            line("credit_limit", "Credit Line", "", statement.getCreditLimit(), fieldReviewed, fieldReviewedChanged, false),
            line("available_credit", "Credit Line Available", "", statement.getAvailableCredit(), fieldReviewed, fieldReviewedChanged, false),
            line("cash_advance_limit", "Cash Advance Credit Line", "", statement.getCashAdvanceLimit(), fieldReviewed, fieldReviewedChanged, false),
            line("available_cash_advance_credit", "Cash Advance Credit Line Available", "", statement.getAvailableCashAdvanceCredit(), fieldReviewed, fieldReviewedChanged, false)
        );
        panel.getChildren().add(rows);
        return panel;
    }

    private VBox transactionsPanel(List<CreditCardTransaction> transactions, BiConsumer<CreditCardTransaction, Boolean> transactionReviewedChanged, Runnable addTransactionAction) {
        VBox panel = panel("Transactions", "");
        Label title = new Label("Posted Activity");
        title.getStyleClass().add("discover-subsection-title");
        Button add = new Button();
        add.setText("Añadir");
        add.getStyleClass().add("discover-add-movement-link");
        add.setOnAction(event -> {
            event.consume();
            addTransactionAction.run();
        });
        HBox header = new HBox(12, title, add);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(title, Priority.ALWAYS);
        GridPane grid = new GridPane();
        grid.getStyleClass().add("discover-transactions-grid");
        transactionHeader(grid, 0, "Date");
        transactionHeader(grid, 1, "Description");
        transactionHeader(grid, 2, "Amount");
        transactionHeader(grid, 3, "Revisado");
        int row = 1;
        for (CreditCardTransaction transaction : transactions) {
            transactionCell(grid, 0, row, formatShortDate(transaction.getTransactionDate()), "discover-date-cell");
            transactionCell(grid, 1, row, text(transaction.getDescription()), "discover-description-cell");
            transactionCell(grid, 2, row, Money.format(transaction.getAmount()), "discover-amount-cell");
            CheckBox check = new CheckBox();
            check.getStyleClass().add("discover-check-cell");
            check.setSelected(!transaction.isPendingReview());
            check.setOnAction(event -> transactionReviewedChanged.accept(transaction, check.isSelected()));
            grid.add(check, 3, row++);
        }
        if (transactions.isEmpty()) {
            transactionCell(grid, 0, row, "Sin movimientos guardados", "discover-description-cell");
        }
        panel.getChildren().addAll(header, grid);
        return panel;
    }

    private VBox panel(String title, String period) {
        VBox panel = new VBox();
        panel.getStyleClass().add("discover-panel");
        HBox header = new HBox();
        header.getStyleClass().add("discover-section-header");
        Label label = new Label(title);
        label.getStyleClass().add("discover-section-title");
        header.getChildren().add(label);
        panel.getChildren().add(header);
        return panel;
    }

    private HBox line(String fieldName, String label, String sign, double amount, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged, boolean total) {
        HBox row = new HBox(8);
        row.getStyleClass().add(total ? "discover-total-row" : "discover-line-row");
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add(total ? "discover-total-label" : "discover-line-label");
        Label signNode = new Label(sign);
        signNode.getStyleClass().add("discover-sign");
        Label amountNode = new Label(Money.format(amount));
        amountNode.getStyleClass().add(total ? "discover-total-input" : "discover-line-amount");
        CheckBox check = new CheckBox();
        check.getStyleClass().add("discover-line-check");
        check.setSelected(fieldReviewed.test(fieldName));
        check.setOnAction(event -> fieldReviewedChanged.accept(fieldName, check.isSelected()));
        row.getChildren().addAll(labelNode, signNode, amountNode, check);
        return row;
    }

    private void paymentCell(GridPane grid, int row, String fieldName, String label, String value, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("discover-large-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("discover-large-input");
        CheckBox check = new CheckBox();
        check.getStyleClass().add("discover-line-check");
        check.setSelected(fieldReviewed.test(fieldName));
        check.setOnAction(event -> fieldReviewedChanged.accept(fieldName, check.isSelected()));
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
        grid.add(check, 2, row);
    }

    private void transactionHeader(GridPane grid, int column, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("discover-transaction-header");
        grid.add(label, column, 0);
    }

    private void transactionCell(GridPane grid, int column, int row, String text, String widthClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("discover-transaction-cell", widthClass);
        grid.add(label, column, row);
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

    private String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
