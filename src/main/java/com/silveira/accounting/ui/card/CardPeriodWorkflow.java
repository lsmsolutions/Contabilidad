package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.dto.CardPeriodSummary;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.services.ExcelExportService;
import java.io.File;
import java.util.List;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CardPeriodWorkflow {
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;
    private final ExcelExportService exports;
    private final Config config;

    public CardPeriodWorkflow(
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions,
        ExcelExportService exports,
        Config config
    ) {
        this.statements = statements;
        this.transactions = transactions;
        this.exports = exports;
        this.config = config;
    }

    public VBox build(
        String alias,
        TableView<CreditCardStatement> table,
        HBox totalsPanel,
        VBox statementCards
    ) {
        FlowPane[] visibleCards = new FlowPane[1];
        VBox box = new CardMonthlyCardsView().build(
            statements.periodSummaries(alias),
            period -> config.reviewMark().apply(alias, period.year(), period.month()),
            new CardMonthlyCardsView.Actions() {
                @Override
                public void open(CardPeriodSummary period) {
                    config.selectedPeriodChanged().accept(period.year(), period.month());
                    config.openPeriod().open(alias, period.year(), period.month());
                }

                @Override
                public void edit(CardPeriodSummary period) {
                    config.editPeriod().edit(
                        period.statements(),
                        () -> refresh(alias, table, totalsPanel, statementCards, visibleCards[0])
                    );
                }

                @Override
                public void delete(CardPeriodSummary period) {
                    deletePeriod(alias, period, table, totalsPanel, statementCards, visibleCards[0]);
                }

                @Override
                public Button downloadButton(CardPeriodSummary period) {
                    return monthlyExportButton(() -> exportMonthly(alias, period.year(), period.month()));
                }
            }
        );
        visibleCards[0] = (FlowPane) box.getChildren().get(1);
        return box;
    }

    private void refresh(
        String alias,
        TableView<CreditCardStatement> table,
        HBox totalsPanel,
        VBox statementCards,
        FlowPane cards
    ) {
        table.setItems(FXCollections.observableArrayList(
            statements.findByAccount(alias, config.selectedYear().get(), config.selectedMonth().get())
        ));
        Runnable visibleTotals = () -> totalsPanel.getChildren().setAll(
            config.accumulatedTotals().nodes(alias, config.selectedYear().get(), config.selectedMonth().get())
        );
        visibleTotals.run();
        config.statementCards().refresh(table, statementCards, visibleTotals);
        FlowPane refreshed = (FlowPane) build(alias, table, totalsPanel, statementCards).getChildren().get(1);
        cards.getChildren().setAll(refreshed.getChildren());
    }

    private void deletePeriod(
        String alias,
        CardPeriodSummary period,
        TableView<CreditCardStatement> table,
        HBox totalsPanel,
        VBox statementCards,
        FlowPane cards
    ) {
        if (period.statements().isEmpty()) {
            return;
        }
        boolean proceed = config.confirm().confirm(
            "Eliminar periodo de tarjeta",
            "Se eliminaran " + period.statements().size() + " resumen(es) de tarjeta del periodo "
                + period.title()
                + ", junto con sus movimientos y alertas.\n\nEsta accion no se puede deshacer.",
            "Eliminar periodo"
        );
        if (!proceed) {
            return;
        }
        statements.deletePeriod(period.statements());
        refresh(alias, table, totalsPanel, statementCards, cards);
    }

    private Button monthlyExportButton(Runnable action) {
        Button button = new Button("Descargar mes");
        button.setOnAction(event -> {
            event.consume();
            action.run();
        });
        return button;
    }

    private void exportMonthly(String alias, int year, int month) {
        File file = config.chooseExcel().apply(
            "tarjeta-" + config.safeFileName().apply(alias) + "-" + year + "-" + String.format("%02d", month) + ".xlsx"
        );
        if (file == null) {
            return;
        }
        exports.exportCreditCardMonthly(
            file.toPath(),
            statements.findByAccount(alias, year, month),
            transactions.findByAccount(alias, year, month)
        );
        config.alert().show("Mes exportado", file.getAbsolutePath());
    }

    public record Config(
        ReviewMarkFactory reviewMark,
        PeriodOpener openPeriod,
        PeriodEditor editPeriod,
        PeriodChanged selectedPeriodChanged,
        java.util.function.Supplier<Integer> selectedYear,
        java.util.function.Supplier<Integer> selectedMonth,
        CardShellWorkflow.AccumulatedTotalsFactory accumulatedTotals,
        CardShellWorkflow.StatementCardsRefresher statementCards,
        ConfirmAction confirm,
        Function<String, File> chooseExcel,
        Function<String, String> safeFileName,
        AlertSink alert
    ) {}

    @FunctionalInterface
    public interface ReviewMarkFactory {
        Node apply(String alias, int year, int month);
    }

    @FunctionalInterface
    public interface PeriodOpener {
        void open(String alias, int year, int month);
    }

    @FunctionalInterface
    public interface PeriodEditor {
        void edit(List<CreditCardStatement> statements, Runnable refresh);
    }

    @FunctionalInterface
    public interface PeriodChanged {
        void accept(Integer year, Integer month);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }

    @FunctionalInterface
    public interface AlertSink {
        void show(String title, String message);
    }
}
