package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
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

    public VBox build(CreditCardStatement statement, Consumer<Boolean> reviewedChanged, Runnable editAction) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("card-statement-sheet", "monthly-card");

        CheckBox reviewed = reviewedCheck(statement, reviewedChanged);
        HBox header = new HBox(12, statementIdentity(statement), reviewed);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(0), Priority.ALWAYS);

        HBox payment = new HBox(12,
            heroAmount("New Balance", Money.format(statement.getNewBalance()), "statement-balance-due"),
            heroAmount("Minimum Payment Due", Money.format(statement.getMinimumPaymentDue()), "statement-minimum-due"),
            heroAmount("Payment Due Date", formatShortDate(statement.getPaymentDueDate()), "statement-date-due")
        );
        payment.getStyleClass().add("statement-payment-row");

        GridPane accountSummary = section("Account Summary");
        addMoneyRow(accountSummary, 1, "Previous Balance", statement.getPreviousBalance(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 2, "Payments", statement.getPayments(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 3, "Other Credits", statement.getOtherCredits(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 4, "Transactions", statement.getTransactions(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 5, "Balance Transfers", statement.getBalanceTransfers(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 6, "Cash Advances", statement.getCashAdvances(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 7, "Fees Charged", statement.getFeesCharged(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 8, "Interest Charged", statement.getInterestCharged(), statement, reviewedChanged);
        addMoneyRow(accountSummary, 9, "New Balance", statement.getNewBalance(), statement, reviewedChanged);

        GridPane creditLine = section("Credit Line");
        addMoneyRow(creditLine, 1, "Credit Limit", statement.getCreditLimit(), statement, reviewedChanged);
        addMoneyRow(creditLine, 2, "Available Credit", statement.getAvailableCredit(), statement, reviewedChanged);
        addMoneyRow(creditLine, 3, "Cash Advance Limit", statement.getCashAdvanceLimit(), statement, reviewedChanged);
        addMoneyRow(creditLine, 4, "Available Cash Advance Credit", statement.getAvailableCashAdvanceCredit(), statement, reviewedChanged);

        HBox body = new HBox(14, accountSummary, creditLine);
        HBox.setHgrow(accountSummary, Priority.ALWAYS);
        HBox.setHgrow(creditLine, Priority.ALWAYS);

        VBox rewards = rewards(statement, reviewedChanged);
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

    private VBox statementIdentity(CreditCardStatement statement) {
        Label bank = new Label(identityLine(statement));
        bank.getStyleClass().add("statement-bank-title");
        Label period = new Label(periodLine(statement));
        period.getStyleClass().add("statement-period-line");
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

    private VBox heroAmount(String title, String value, String style) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("statement-hero-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("statement-hero-value", style);
        VBox box = new VBox(4, titleLabel, valueLabel);
        box.getStyleClass().add("statement-hero-box");
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private GridPane section(String title) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("statement-section-grid");
        grid.setHgap(10);
        grid.setVgap(7);
        ColumnConstraints label = new ColumnConstraints();
        label.setHgrow(Priority.ALWAYS);
        ColumnConstraints value = new ColumnConstraints();
        value.setHalignment(HPos.RIGHT);
        value.setMinWidth(120);
        ColumnConstraints review = new ColumnConstraints();
        review.setHalignment(HPos.CENTER);
        review.setMinWidth(78);
        grid.getColumnConstraints().setAll(label, value, review);
        Label heading = new Label(title);
        heading.getStyleClass().add("statement-section-title");
        grid.add(heading, 0, 0, 3, 1);
        Label reviewTitle = new Label("Revisado");
        reviewTitle.getStyleClass().add("statement-review-title");
        grid.add(reviewTitle, 2, 0);
        return grid;
    }

    private void addMoneyRow(GridPane grid, int row, String label, double amount, CreditCardStatement statement, Consumer<Boolean> reviewedChanged) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("statement-row-label");
        Label valueNode = new Label(Money.format(amount));
        valueNode.getStyleClass().add("statement-row-value");
        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
        grid.add(reviewedCheck(statement, reviewedChanged), 2, row);
    }

    private VBox rewards(CreditCardStatement statement, Consumer<Boolean> reviewedChanged) {
        if (statement.getRewardsBalance() == 0
            && statement.getRewardsPreviousBalance() == 0
            && statement.getRewardsEarned() == 0
            && statement.getRewardsRedeemed() == 0) {
            return null;
        }
        GridPane grid = section("Rewards");
        addMoneyRow(grid, 1, "Previous Rewards", statement.getRewardsPreviousBalance(), statement, reviewedChanged);
        addMoneyRow(grid, 2, "Earned This Period", statement.getRewardsEarned(), statement, reviewedChanged);
        addMoneyRow(grid, 3, "Redeemed This Period", statement.getRewardsRedeemed(), statement, reviewedChanged);
        addMoneyRow(grid, 4, "Rewards Balance", statement.getRewardsBalance(), statement, reviewedChanged);
        VBox box = new VBox(grid);
        box.getStyleClass().add("statement-rewards-box");
        return box;
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

    private CheckBox reviewedCheck(CreditCardStatement statement, Consumer<Boolean> reviewedChanged) {
        CheckBox check = new CheckBox();
        check.setSelected(!statement.isPendingReview());
        check.setOnAction(event -> reviewedChanged.accept(check.isSelected()));
        return check;
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
