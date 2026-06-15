package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;

public class CardEditWorkflow {
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;
    private final Config config;

    public CardEditWorkflow(
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions,
        Config config
    ) {
        this.statements = statements;
        this.transactions = transactions;
        this.config = config;
    }

    public void addManualStatement(String alias, Runnable refresh) {
        CreditCardStatement statement = statements.createManual(alias, LocalDate.now());
        showPeriodDialog(List.of(statement), refresh);
    }

    public void addManualMovement(
        TableView<CreditCardStatement> statementTable,
        TableView<CreditCardTransaction> movementTable
    ) {
        long statementId = statementTable.getItems().isEmpty() ? 0 : statementTable.getItems().get(0).getId();
        CreditCardTransaction movement = transactions.createManual(statementId, LocalDate.now());
        movementTable.getItems().add(movement);
        movementTable.getSelectionModel().select(movement);
    }

    public void saveVisibleStatements(TableView<CreditCardStatement> statementTable, Runnable refresh) {
        statements.saveVisible(statementTable.getItems());
        refresh.run();
        config.alert().show(Alert.AlertType.INFORMATION, "Resumen guardado", "Resumen visible guardado con su estado actual.");
    }

    public void saveVisibleMovements(
        TableView<CreditCardStatement> statementTable,
        TableView<CreditCardTransaction> movementTable,
        Runnable refresh
    ) {
        long statementId = statementTable.getItems().isEmpty() ? 0 : statementTable.getItems().get(0).getId();
        transactions.saveVisible(statementId, movementTable.getItems());
        refresh.run();
        config.alert().show(Alert.AlertType.INFORMATION, "Movimientos guardados", "Movimientos visibles guardados con su estado actual.");
    }

    public void showTransactionDialog(CreditCardStatement statement, Runnable refresh) {
        if (statement == null || statement.getId() <= 0) {
            config.alert().show(
                Alert.AlertType.WARNING,
                "Resumen sin guardar",
                "Guarda primero el resumen de la tarjeta antes de añadir movimientos."
            );
            return;
        }
        new CardTransactionDialogView().show(statement).ifPresent(form -> {
            if (form.transactionDate() == null || form.postDate() == null || form.description().isBlank()) {
                config.alert().show(Alert.AlertType.WARNING, "Movimiento incompleto", "Indica fecha, posteo y descripción.");
                return;
            }
            CreditCardTransaction movement = new CreditCardTransaction(
                0,
                statement.getId(),
                form.transactionDate(),
                form.postDate(),
                form.description().trim(),
                Money.parse(form.amount()),
                form.type().isBlank() ? "gasto" : form.type().trim(),
                form.category().isBlank() ? "manual" : form.category().trim()
            );
            movement.setPendingReview(!form.reviewed());
            movement.setReviewRequired(!form.reviewed());
            movement.setReviewNotes(form.notes());
            movement.setId(transactions.save(statement.getId(), movement));
            refresh.run();
        });
    }

    public void showPeriodDialog(List<CreditCardStatement> statementList, Runnable refresh) {
        if (statementList.isEmpty()) {
            config.alert().show(Alert.AlertType.INFORMATION, "Sin resumen", "No hay resumen de tarjeta para editar en este mes.");
            return;
        }
        new CardPeriodEditDialogView().show(statementList, config.statementTitle()).ifPresent(form -> {
            CreditCardStatement selected = form.statement();
            if (selected == null) {
                return;
            }
            if (form.start() == null || form.end() == null) {
                config.alert().show(
                    Alert.AlertType.WARNING,
                    "Periodo incompleto",
                    "Indica fecha Desde y Hasta para el resumen de tarjeta."
                );
                return;
            }
            if (form.start().isAfter(form.end())) {
                config.alert().show(
                    Alert.AlertType.WARNING,
                    "Periodo no valido",
                    "La fecha Desde no puede ser posterior a Hasta."
                );
                return;
            }
            try {
                apply(form, selected);
                if (selected.getId() > 0) {
                    statements.updateRecord(selected);
                }
                refresh.run();
            } catch (RuntimeException exception) {
                config.alert().show(
                    Alert.AlertType.ERROR,
                    "No se pudieron editar los datos",
                    config.rootCauseMessage().apply(exception)
                );
            }
        });
    }

    private void apply(CardPeriodEditDialogView.FormData form, CreditCardStatement selected) {
        selected.setStatementStartDate(form.start());
        selected.setStatementEndDate(form.end());
        selected.setPaymentDueDate(form.due());
        selected.setNextClosingDate(form.nextClosing());
        selected.setPreviousBalance(Money.parse(form.previous()));
        selected.setPayments(Money.parse(form.payments()));
        selected.setOtherCredits(Money.parse(form.credits()));
        selected.setTransactions(Money.parse(form.purchases()));
        selected.setBalanceTransfers(Money.parse(form.transfers()));
        selected.setCashAdvances(Money.parse(form.cash()));
        selected.setFeesCharged(Money.parse(form.fees()));
        selected.setInterestCharged(Money.parse(form.interest()));
        selected.setNewBalance(Money.parse(form.newBalance()));
        selected.setMinimumPaymentDue(Money.parse(form.minimum()));
        selected.setCreditLimit(Money.parse(form.limit()));
        selected.setAvailableCredit(Money.parse(form.available()));
        selected.setCashAdvanceLimit(Money.parse(form.cashLimit()));
        selected.setAvailableCashAdvanceCredit(Money.parse(form.cashAvailable()));
        selected.setRewardsBalance(Money.parse(form.rewardsBalance()));
        selected.setRewardsPreviousBalance(Money.parse(form.rewardsPrevious()));
        selected.setRewardsEarned(Money.parse(form.rewardsEarned()));
        selected.setRewardsRedeemed(Money.parse(form.rewardsRedeemed()));
        selected.setPendingReview(!form.reviewed());
        selected.setReviewRequired(!form.reviewed());
        selected.setReviewNotes(form.notes());
    }

    public record Config(
        AlertSink alert,
        Function<Throwable, String> rootCauseMessage,
        Function<CreditCardStatement, String> statementTitle
    ) {}

    @FunctionalInterface
    public interface AlertSink {
        void show(Alert.AlertType type, String title, String message);
    }
}
