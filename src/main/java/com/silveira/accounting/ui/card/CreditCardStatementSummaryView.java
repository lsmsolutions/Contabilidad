package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

public class CreditCardStatementSummaryView {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public VBox build(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged, Consumer<Boolean> allReviewedChanged, Runnable editAction) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("statement-card", "monthly-card");

        CheckBox reviewed = new CheckBox("Todo revisado");
        reviewed.setSelected(fieldKeys(statement).stream().allMatch(fieldReviewed));
        reviewed.setOnAction(event -> allReviewedChanged.accept(reviewed.isSelected()));
        HBox header = new HBox(12, statementIdentity(statement), reviewed);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        HBox payment = new HBox(12,
            heroAmount("New Balance", Money.format(accountSummaryBalance(statement))),
            heroAmount("Minimum Payment Due", Money.format(statement.getMinimumPaymentDue())),
            heroAmount("Payment Due Date", formatShortDate(statement.getPaymentDueDate()))
        );
        payment.getStyleClass().add("statement-field-row");

        GridPane accountSummary = section("Account Summary");
        addMoneyRow(accountSummary, 1, "previous_balance", "Previous Balance", "+", statement.getPreviousBalance(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 2, "payments", "Payments", "-", statement.getPayments(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 3, "other_credits", "Other Credits", "-", statement.getOtherCredits(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 4, "transactions", "Transactions", "+", statement.getTransactions(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 5, "balance_transfers", "Balance Transfers", "+", statement.getBalanceTransfers(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 6, "cash_advances", "Cash Advances", "+", statement.getCashAdvances(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 7, "fees_charged", "Fees Charged", "+", statement.getFeesCharged(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 8, "interest_charged", "Interest Charged", "+", statement.getInterestCharged(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(accountSummary, 9, "new_balance", "New Balance", "=", accountSummaryBalance(statement), fieldReviewed, fieldReviewedChanged);

        GridPane creditLine = section("Credit Line");
        addMoneyRow(creditLine, 1, "credit_limit", "Credit Limit", statement.getCreditLimit(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(creditLine, 2, "available_credit", "Available Credit", statement.getAvailableCredit(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(creditLine, 3, "cash_advance_limit", "Cash Advance Limit", statement.getCashAdvanceLimit(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(creditLine, 4, "available_cash_advance_credit", "Available Cash Advance Credit", statement.getAvailableCashAdvanceCredit(), fieldReviewed, fieldReviewedChanged);

        HBox body = new HBox(14, accountSummary, creditLine);
        HBox.setHgrow(accountSummary, Priority.ALWAYS);
        HBox.setHgrow(creditLine, Priority.ALWAYS);

        VBox rewards = rewards(statement, fieldReviewed, fieldReviewedChanged);
        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> editAction.run());
        HBox footer = new HBox(12, status(statement), edit);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(0), Priority.ALWAYS);

        card.getChildren().addAll(header, payment, body);
        if (rewards != null) {
            card.getChildren().add(rewards);
        }
        card.getChildren().add(footer);
        return card;
    }

    public List<String> fieldKeys(CreditCardStatement statement) {
        List<String> fields = new ArrayList<>(List.of(
            "previous_balance",
            "payments",
            "other_credits",
            "transactions",
            "balance_transfers",
            "cash_advances",
            "fees_charged",
            "interest_charged",
            "new_balance",
            "credit_limit",
            "available_credit",
            "cash_advance_limit",
            "available_cash_advance_credit"
        ));
        if (hasRewards(statement)) {
            fields.addAll(List.of("rewards_previous_balance", "rewards_earned", "rewards_redeemed", "rewards_balance"));
        }
        return fields;
    }

    private VBox statementIdentity(CreditCardStatement statement) {
        Label bank = new Label(identityLine(statement));
        bank.getStyleClass().add("credit-info-title");
        Label period = new Label(periodLine(statement));
        period.getStyleClass().add("statement-field-label");
        VBox box = new VBox(3, bank, period);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private String identityLine(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).isBlank() ? text(statement.getAccountAlias()) : text(statement.getBankName());
        String card = text(statement.getCardName());
        String digits = text(statement.getAccountLastDigits());
        String suffix = digits.isBlank() ? "" : " ending in " + digits;
        return card.isBlank() ? bank + suffix : bank + " | " + card + suffix;
    }

    private String periodLine(CreditCardStatement statement) {
        String start = formatShortDate(statement.getStatementStartDate());
        String end = formatShortDate(statement.getStatementEndDate());
        if (!start.isBlank() && !end.isBlank()) {
            return "Statement Period: " + start + " - " + end;
        }
        return end.isBlank() ? "Statement Period" : "Statement Closing Date: " + end;
    }

    private VBox heroAmount(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("statement-field-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("credit-info-title");
        VBox box = new VBox(4, titleLabel, valueLabel);
        box.getStyleClass().add("statement-section");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private GridPane section(String title) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("statement-section");
        grid.setHgap(10);
        grid.setVgap(7);
        ColumnConstraints label = new ColumnConstraints();
        label.setHgrow(Priority.ALWAYS);
        ColumnConstraints sign = new ColumnConstraints();
        sign.setHalignment(HPos.CENTER);
        sign.setMinWidth(24);
        ColumnConstraints value = new ColumnConstraints();
        value.setHalignment(HPos.RIGHT);
        value.setMinWidth(120);
        ColumnConstraints review = new ColumnConstraints();
        review.setHalignment(HPos.CENTER);
        review.setMinWidth(78);
        grid.getColumnConstraints().setAll(label, sign, value, review);
        Label heading = new Label(title);
        heading.getStyleClass().add("statement-section-title");
        grid.add(heading, 0, 0, 4, 1);
        Label reviewTitle = new Label("Revisado");
        reviewTitle.getStyleClass().add("statement-field-label");
        grid.add(reviewTitle, 3, 0);
        return grid;
    }

    private void addMoneyRow(GridPane grid, int row, String fieldName, String label, double amount, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        addMoneyRow(grid, row, fieldName, label, "", amount, fieldReviewed, fieldReviewedChanged);
    }

    private void addMoneyRow(GridPane grid, int row, String fieldName, String label, String sign, double amount, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("statement-bilingual-label");
        Label signNode = new Label(sign);
        signNode.getStyleClass().add("statement-readonly-value");
        Label valueNode = new Label(Money.format(amount));
        valueNode.getStyleClass().add("statement-readonly-value");
        grid.add(labelNode, 0, row);
        grid.add(signNode, 1, row);
        grid.add(valueNode, 2, row);
        grid.add(reviewedCheck(fieldName, fieldReviewed, fieldReviewedChanged), 3, row);
    }

    private double accountSummaryBalance(CreditCardStatement statement) {
        return statement.getPreviousBalance()
            - statement.getPayments()
            - statement.getOtherCredits()
            + statement.getTransactions()
            + statement.getBalanceTransfers()
            + statement.getCashAdvances()
            + statement.getFeesCharged()
            + statement.getInterestCharged();
    }

    private VBox rewards(CreditCardStatement statement, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        if (!hasRewards(statement)) {
            return null;
        }
        GridPane grid = section("Rewards");
        addMoneyRow(grid, 1, "rewards_previous_balance", "Previous Rewards", statement.getRewardsPreviousBalance(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 2, "rewards_earned", "Earned This Period", statement.getRewardsEarned(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 3, "rewards_redeemed", "Redeemed This Period", statement.getRewardsRedeemed(), fieldReviewed, fieldReviewedChanged);
        addMoneyRow(grid, 4, "rewards_balance", "Rewards Balance", statement.getRewardsBalance(), fieldReviewed, fieldReviewedChanged);
        VBox box = new VBox(grid);
        box.getStyleClass().add("rewards-row");
        return box;
    }

    private boolean hasRewards(CreditCardStatement statement) {
        return statement.getRewardsBalance() != 0
            || statement.getRewardsPreviousBalance() != 0
            || statement.getRewardsEarned() != 0
            || statement.getRewardsRedeemed() != 0;
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

    private CheckBox reviewedCheck(String fieldName, Predicate<String> fieldReviewed, BiConsumer<String, Boolean> fieldReviewedChanged) {
        CheckBox check = new CheckBox();
        check.setSelected(fieldReviewed.test(fieldName));
        check.setOnAction(event -> fieldReviewedChanged.accept(fieldName, check.isSelected()));
        return check;
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
