package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

public class MortgageStatementSummaryView {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VBox build(
        MortgageStatement statement,
        List<MortgageTransaction> transactions,
        Consumer<Boolean> statementReviewedChanged,
        BiConsumer<MortgageTransaction, Boolean> transactionReviewedChanged,
        Runnable editAction
    ) {
        VBox card = new VBox(14);
        card.getStyleClass().add("mortgage-statement-summary");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox top = new HBox(14, borrowerBlock(statement), dueBox(statement, statementReviewedChanged));
        top.getStyleClass().add("mortgage-top-summary");

        HBox summary = new HBox(14,
            amountDue(statement, statementReviewedChanged),
            accountInfo(statement, statementReviewedChanged),
            pastPayment(statement, statementReviewedChanged)
        );
        summary.getStyleClass().add("mortgage-summary-row");

        VBox activity = transactionActivity(statement, transactions, transactionReviewedChanged);

        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> editAction.run());
        HBox footer = new HBox(12, status(statement), edit);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(0), Priority.ALWAYS);

        card.getChildren().addAll(top, summary, activity, footer);
        return card;
    }

    private VBox borrowerBlock(MortgageStatement statement) {
        Label title = new Label(text(statement.getLoanAlias()).isBlank() ? "Hipoteca" : text(statement.getLoanAlias()));
        title.getStyleClass().add("mortgage-month-title");
        Label servicer = new Label(text(statement.getServicerName()));
        servicer.getStyleClass().add("mortgage-line-label");
        Label address = new Label(text(statement.getPropertyAddress()));
        address.getStyleClass().add("mortgage-line-label");
        VBox box = new VBox(3, title, servicer, address);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox dueBox(MortgageStatement statement, Consumer<Boolean> reviewedChanged) {
        VBox box = new VBox(8);
        box.getStyleClass().add("mortgage-due-box");
        box.getChildren().addAll(
            blueLine("Statement Date", formatDate(statement.getStatementDate())),
            blueLine("Payment Amount Due", Money.format(dueAmount(statement))),
            blueLine("Payment Due Date", formatDate(statement.getPaymentDueDate()))
        );
        if (statement.getLateFeeDate() != null || statement.getLateFeeAmount() > 0) {
            Label late = new Label("If payment is received after " + formatDate(statement.getLateFeeDate()) + ", a " + Money.format(statement.getLateFeeAmount()) + " late fee may be charged.");
            late.getStyleClass().add("mortgage-blue-label");
            late.setWrapText(true);
            box.getChildren().add(late);
        }
        CheckBox reviewed = new CheckBox("Revisado");
        reviewed.setSelected(!statement.isPendingReview());
        reviewed.setOnAction(event -> reviewedChanged.accept(reviewed.isSelected()));
        box.getChildren().add(reviewed);
        return box;
    }

    private HBox blueLine(String label, String value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("mortgage-blue-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("mortgage-blue-value");
        HBox row = new HBox(12, labelNode, valueNode);
        HBox.setHgrow(labelNode, Priority.ALWAYS);
        return row;
    }

    private VBox amountDue(MortgageStatement statement, Consumer<Boolean> reviewedChanged) {
        VBox card = summaryCard("Explanation of Amount Due");
        card.getChildren().addAll(
            line("Principal", statement.getPrincipalDue(), false),
            line("Interest", statement.getInterestDue(), false),
            line("Escrow", statement.getEscrowDue(), false),
            line("Regular Monthly Payment", statement.getRegularMonthlyPayment(), true),
            subheader("Past Due Amounts"),
            line("Past Due Amount", statement.getPastDueAmount(), false),
            line("Fees", statement.getFees(), false),
            line("Other Fees & Charges", statement.getOtherFeesAndCharges(), false),
            totalLine("Total Due", dueAmount(statement), reviewedChanged, statement.isPendingReview())
        );
        return card;
    }

    private VBox accountInfo(MortgageStatement statement, Consumer<Boolean> reviewedChanged) {
        VBox card = summaryCard("Account Information");
        card.getChildren().addAll(
            textLine("Account Number", text(statement.getLoanNumber())),
            textLine("Property Address", text(statement.getPropertyAddress())),
            line("Original Principal Balance", statement.getOriginalPrincipalBalance(), false),
            line("Outstanding Principal Balance", statement.getOutstandingPrincipalBalance(), false),
            textLine("Maturity Date", formatDate(statement.getMaturityDate())),
            textLine("Interest Rate", percent(statement.getInterestRate())),
            line("Escrow Balance", statement.getEscrowBalance(), false),
            line("Unapplied Funds", statement.getUnappliedFunds(), false)
        );
        return card;
    }

    private VBox pastPayment(MortgageStatement statement, Consumer<Boolean> reviewedChanged) {
        VBox card = summaryCard("Past Payment Summary");
        card.setMinWidth(420);
        card.getChildren().add(headerRow("Paid since last statement", "Paid year-to-date"));
        card.getChildren().addAll(
            pastLine("Principal", statement.getPastPaidPrincipalSinceLastStatement(), statement.getPastPaidPrincipalYearToDate()),
            pastLine("Interest", statement.getPastPaidInterestSinceLastStatement(), statement.getPastPaidInterestYearToDate()),
            pastLine("Escrow (Taxes & Insurance)", statement.getPastPaidEscrowSinceLastStatement(), statement.getPastPaidEscrowYearToDate()),
            pastLine("Total", statement.getPastPaidTotalSinceLastStatement(), statement.getPastPaidTotalYearToDate())
        );
        return card;
    }

    private VBox transactionActivity(MortgageStatement statement, List<MortgageTransaction> transactions, BiConsumer<MortgageTransaction, Boolean> reviewedChanged) {
        VBox card = new VBox(8);
        card.getStyleClass().add("mortgage-activity-card");
        Label title = new Label("Transaction Activity Since Your Last Statement");
        title.getStyleClass().add("mortgage-section-header");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.getStyleClass().add("mortgage-activity-header");
        String[] headers = {"Date", "Description", "Total", "Principal", "Interest", "Escrow", "Fees", "Unapplied", "Corp. Adv.", "Other", "Revisado"};
        double[] widths = {8, 18, 9, 9, 9, 9, 7, 9, 8, 7, 7};
        for (int i = 0; i < headers.length; i++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(widths[i]);
            grid.getColumnConstraints().add(constraints);
            Label header = new Label(headers[i]);
            header.getStyleClass().add("mortgage-activity-cell");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setWrapText(true);
            grid.add(header, i, 0);
        }
        int row = 1;
        for (MortgageTransaction transaction : transactions) {
            activityCell(grid, 0, row, formatDate(transaction.getTransactionDate()), "mortgage-activity-date");
            activityCell(grid, 1, row, text(transaction.getDescription()), "mortgage-activity-description");
            activityCell(grid, 2, row, Money.format(transaction.getTotal()), "mortgage-activity-money");
            activityCell(grid, 3, row, Money.format(transaction.getPrincipal()), "mortgage-activity-money");
            activityCell(grid, 4, row, Money.format(transaction.getInterest()), "mortgage-activity-money");
            activityCell(grid, 5, row, Money.format(transaction.getEscrow()), "mortgage-activity-money");
            activityCell(grid, 6, row, Money.format(transaction.getFees()), "mortgage-activity-money");
            activityCell(grid, 7, row, Money.format(transaction.getUnapplied()), "mortgage-activity-money");
            activityCell(grid, 8, row, Money.format(transaction.getCorporateAdvance()), "mortgage-activity-money");
            activityCell(grid, 9, row, Money.format(transaction.getOther()), "mortgage-activity-money");
            CheckBox check = new CheckBox();
            check.getStyleClass().add("mortgage-activity-review-cell");
            check.setSelected(!transaction.isPendingReview());
            check.setOnAction(event -> reviewedChanged.accept(transaction, check.isSelected()));
            grid.add(check, 10, row++);
        }
        if (transactions.isEmpty()) {
            activityCell(grid, 0, row, "Sin movimientos guardados para este statement", "mortgage-activity-description");
        }
        card.getChildren().addAll(title, grid);
        return card;
    }

    private VBox summaryCard(String title) {
        VBox card = new VBox(5);
        card.getStyleClass().add("mortgage-summary-card");
        Label heading = new Label(title);
        heading.getStyleClass().add("mortgage-section-header");
        card.getChildren().add(heading);
        return card;
    }

    private Label subheader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("mortgage-section-subheader");
        return label;
    }

    private HBox line(String label, double amount, boolean bold) {
        HBox row = new HBox(8);
        row.getStyleClass().add("mortgage-summary-row");
        if (bold) {
            row.getStyleClass().add("mortgage-bold-line");
        }
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("mortgage-line-label");
        Label valueNode = new Label(Money.format(amount));
        valueNode.getStyleClass().add("mortgage-line-value");
        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private HBox textLine(String label, String value) {
        HBox row = new HBox(8);
        row.getStyleClass().add("mortgage-summary-row");
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("mortgage-line-label");
        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("mortgage-line-value");
        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    private HBox totalLine(String label, double amount, Consumer<Boolean> reviewedChanged, boolean pending) {
        HBox row = new HBox(8);
        row.getStyleClass().add("mortgage-total-line");
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("mortgage-line-label");
        Label valueNode = new Label(Money.format(amount));
        valueNode.getStyleClass().add("mortgage-line-value");
        CheckBox reviewed = new CheckBox();
        reviewed.setSelected(!pending);
        reviewed.setOnAction(event -> reviewedChanged.accept(reviewed.isSelected()));
        row.getChildren().addAll(labelNode, valueNode, reviewed);
        return row;
    }

    private HBox headerRow(String left, String right) {
        HBox row = new HBox(8);
        row.getStyleClass().add("mortgage-past-payment-row");
        Label blank = new Label("");
        blank.getStyleClass().add("mortgage-past-payment-label");
        blank.setMinWidth(150);
        Label leftLabel = new Label(left);
        leftLabel.getStyleClass().add("mortgage-past-payment-column");
        leftLabel.setMinWidth(112);
        leftLabel.setWrapText(true);
        Label rightLabel = new Label(right);
        rightLabel.getStyleClass().add("mortgage-past-payment-column");
        rightLabel.setMinWidth(112);
        rightLabel.setWrapText(true);
        row.getChildren().addAll(blank, leftLabel, rightLabel);
        return row;
    }

    private HBox pastLine(String label, double sinceLast, double yearToDate) {
        HBox row = new HBox(8);
        row.getStyleClass().add("mortgage-past-payment-row");
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("mortgage-past-payment-label");
        labelNode.setMinWidth(150);
        labelNode.setWrapText(true);
        Label since = new Label(Money.format(sinceLast));
        since.getStyleClass().add("mortgage-past-payment-column");
        since.setMinWidth(112);
        Label ytd = new Label(Money.format(yearToDate));
        ytd.getStyleClass().add("mortgage-past-payment-column");
        ytd.setMinWidth(112);
        row.getChildren().addAll(labelNode, since, ytd);
        return row;
    }

    private void activityCell(GridPane grid, int column, int row, String text, String widthClass) {
        Label label = new Label(text);
        label.getStyleClass().add("mortgage-activity-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        grid.add(label, column, row);
    }

    private Node status(MortgageStatement statement) {
        String notes = text(statement.getReviewNotes());
        String text = statement.isPendingReview() ? "Pdte revision" : "OK";
        if (!notes.isBlank()) {
            text += " | " + notes;
        }
        Label status = new Label(text);
        status.getStyleClass().add(statement.isPendingReview() ? "statement-pending-chip" : "statement-reviewed-chip");
        return status;
    }

    private double dueAmount(MortgageStatement statement) {
        return statement.getTotalDue() > 0 ? statement.getTotalDue() : statement.getPaymentAmountDue();
    }

    private String percent(double value) {
        return value == 0 ? "" : String.format(java.util.Locale.US, "%.5f%%", value);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
