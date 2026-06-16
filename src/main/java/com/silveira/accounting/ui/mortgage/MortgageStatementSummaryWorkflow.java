package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class MortgageStatementSummaryWorkflow {
    private final MortgageApplicationService mortgage;
    private final Config config;

    public MortgageStatementSummaryWorkflow(MortgageApplicationService mortgage, Config config) {
        this.mortgage = mortgage;
        this.config = config;
    }

    public void refresh(TableView<MortgageStatement> table, VBox summaries, Runnable refreshTotals) {
        summaries.getChildren().clear();
        for (MortgageStatement statement : table.getItems()) {
            summaries.getChildren().add(horizontalStatementScroll(editableSummary(statement, table, summaries, refreshTotals)));
        }
    }

    private VBox editableSummary(MortgageStatement statement, TableView<MortgageStatement> table, VBox summaries, Runnable refreshTotals) {
        VBox summary = new MortgageStatementSummaryView().build(
            statement,
            transactionsForStatement(statement),
            fieldName -> mortgage.fieldReviews().isReviewed(statement.getId(), fieldName, !statement.isPendingReview()),
            (fieldName, reviewed) -> {
                updateStatementFieldReview(statement, fieldName, reviewed);
                table.refresh();
                refreshTotals.run();
            },
            (fieldName, value) -> {
                updateStatementAmount(statement, fieldName, value);
                table.refresh();
                refreshTotals.run();
            },
            reviewed -> {
                updateStatementReview(statement, reviewed);
                updateAllStatementFieldReviews(statement, reviewed);
                table.refresh();
                refreshTotals.run();
                refresh(table, summaries, refreshTotals);
            },
            (transaction, reviewed) -> {
                updateTransactionReview(transaction, reviewed);
                refreshTotals.run();
            },
            (transaction, fieldName, value) -> {
                updateTransactionAmount(transaction, fieldName, value);
                refreshTotals.run();
            },
            () -> config.editStatement().edit(statement, () -> {
                table.refresh();
                refreshTotals.run();
                refresh(table, summaries, refreshTotals);
            }),
            () -> {
                if (!config.confirm().confirm(
                    "Eliminar periodo de hipoteca",
                    "Se eliminaran este statement, sus movimientos y alertas.\n\nEsta accion no se puede deshacer.",
                    "Eliminar periodo"
                )) {
                    return;
                }
                if (statement.getId() > 0) {
                    mortgage.statements().delete(statement.getId());
                }
                config.showMortgageDetail().show(statement.getLoanAlias());
            }
        );
        summary.setOnMouseClicked(event -> table.getSelectionModel().select(statement));
        return summary;
    }

    private List<MortgageTransaction> transactionsForStatement(MortgageStatement statement) {
        if (statement.getId() <= 0 || statement.getStatementDate() == null) {
            return List.of();
        }
        return mortgage.transactions()
            .findByLoan(statement.getLoanAlias(), statement.getStatementDate().getYear(), statement.getStatementDate().getMonthValue())
            .stream()
            .filter(transaction -> transaction.getStatementId() == statement.getId())
            .toList();
    }

    private List<String> statementFieldKeys() {
        return List.of(
            "principal_due",
            "interest_due",
            "escrow_due",
            "regular_monthly_payment",
            "past_due_amount",
            "fees",
            "other_fees_and_charges",
            "total_due",
            "original_principal_balance",
            "outstanding_principal_balance",
            "escrow_balance",
            "unapplied_funds",
            "past_paid_principal_since_last_statement",
            "past_paid_principal_year_to_date",
            "past_paid_interest_since_last_statement",
            "past_paid_interest_year_to_date",
            "past_paid_escrow_since_last_statement",
            "past_paid_escrow_year_to_date",
            "past_paid_total_since_last_statement",
            "past_paid_total_year_to_date"
        );
    }

    private void updateStatementAmount(MortgageStatement statement, String fieldName, double value) {
        switch (fieldName) {
            case "principal_due" -> statement.setPrincipalDue(value);
            case "interest_due" -> statement.setInterestDue(value);
            case "escrow_due" -> statement.setEscrowDue(value);
            case "regular_monthly_payment" -> statement.setRegularMonthlyPayment(value);
            case "past_due_amount" -> statement.setPastDueAmount(value);
            case "fees" -> statement.setFees(value);
            case "other_fees_and_charges" -> statement.setOtherFeesAndCharges(value);
            case "total_due" -> {
                statement.setTotalDue(value);
                statement.setPaymentAmountDue(value);
            }
            case "original_principal_balance" -> statement.setOriginalPrincipalBalance(value);
            case "outstanding_principal_balance" -> statement.setOutstandingPrincipalBalance(value);
            case "escrow_balance" -> statement.setEscrowBalance(value);
            case "unapplied_funds" -> statement.setUnappliedFunds(value);
            case "past_paid_principal_since_last_statement" -> statement.setPastPaidPrincipalSinceLastStatement(value);
            case "past_paid_principal_year_to_date" -> statement.setPastPaidPrincipalYearToDate(value);
            case "past_paid_interest_since_last_statement" -> statement.setPastPaidInterestSinceLastStatement(value);
            case "past_paid_interest_year_to_date" -> statement.setPastPaidInterestYearToDate(value);
            case "past_paid_escrow_since_last_statement" -> statement.setPastPaidEscrowSinceLastStatement(value);
            case "past_paid_escrow_year_to_date" -> statement.setPastPaidEscrowYearToDate(value);
            case "past_paid_total_since_last_statement" -> statement.setPastPaidTotalSinceLastStatement(value);
            case "past_paid_total_year_to_date" -> statement.setPastPaidTotalYearToDate(value);
            default -> {
                return;
            }
        }
        if (statement.getId() > 0) {
            mortgage.statements().updateRecord(statement);
        }
    }

    private void updateTransactionAmount(MortgageTransaction transaction, String fieldName, double value) {
        switch (fieldName) {
            case "total" -> transaction.setTotal(value);
            case "principal" -> transaction.setPrincipal(value);
            case "interest" -> transaction.setInterest(value);
            case "escrow" -> transaction.setEscrow(value);
            case "fees" -> transaction.setFees(value);
            case "unapplied" -> transaction.setUnapplied(value);
            case "corporate_advance" -> transaction.setCorporateAdvance(value);
            case "other" -> transaction.setOther(value);
            default -> {
                return;
            }
        }
        if (transaction.getId() > 0) {
            mortgage.transactions().update(transaction);
        }
    }

    private void updateStatementFieldReview(MortgageStatement statement, String fieldName, boolean reviewed) {
        if (!mortgage.fieldReviews().hasReviews(statement.getId())) {
            mortgage.fieldReviews().setReviewed(statement.getId(), statementFieldKeys(), !statement.isPendingReview());
        }
        mortgage.fieldReviews().setReviewed(statement.getId(), fieldName, reviewed);
        boolean allReviewed = statementFieldKeys().stream()
            .allMatch(key -> mortgage.fieldReviews().isReviewed(statement.getId(), key, !statement.isPendingReview()));
        updateStatementReview(statement, allReviewed);
    }

    private void updateAllStatementFieldReviews(MortgageStatement statement, boolean reviewed) {
        mortgage.fieldReviews().setReviewed(statement.getId(), statementFieldKeys(), reviewed);
    }

    public void updateStatementReview(MortgageStatement statement, boolean reviewed) {
        statement.setPendingReview(!reviewed);
        statement.setReviewRequired(!reviewed);
        if (reviewed && (statement.getReviewNotes() == null || statement.getReviewNotes().isBlank() || statement.getReviewNotes().startsWith("Revisar") || statement.getReviewNotes().startsWith("OCR:"))) {
            statement.setReviewNotes("Revisado");
        } else if (!reviewed && "Revisado".equalsIgnoreCase(statement.getReviewNotes())) {
            statement.setReviewNotes("Revisar contra el PDF original");
        }
        if (statement.getId() > 0) {
            mortgage.statements().updateRecord(statement);
        }
    }

    public void updateTransactionReview(MortgageTransaction transaction, boolean reviewed) {
        transaction.setPendingReview(!reviewed);
        transaction.setReviewRequired(!reviewed);
        if (reviewed && (transaction.getReviewNotes() == null || transaction.getReviewNotes().isBlank() || transaction.getReviewNotes().startsWith("Revisar"))) {
            transaction.setReviewNotes("Revisado");
        } else if (!reviewed && "Revisado".equalsIgnoreCase(transaction.getReviewNotes())) {
            transaction.setReviewNotes("Revisar contra el PDF original");
        }
        if (transaction.getId() > 0) {
            mortgage.transactions().update(transaction);
        }
    }

    private ScrollPane horizontalStatementScroll(Node statementCard) {
        ScrollPane scroll = new ScrollPane(statementCard);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setMaxWidth(Double.MAX_VALUE);
        return scroll;
    }

    public record Config(
        ConfirmAction confirm,
        EditStatementAction editStatement,
        ShowMortgageDetailAction showMortgageDetail
    ) {}

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }

    @FunctionalInterface
    public interface EditStatementAction {
        void edit(MortgageStatement statement, Runnable refresh);
    }

    @FunctionalInterface
    public interface ShowMortgageDetailAction {
        void show(String alias);
    }
}
