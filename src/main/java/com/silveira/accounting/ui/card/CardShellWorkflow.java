package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.models.MonthlySourceTotals;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CardShellWorkflow {
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;
    private final Config config;

    public CardShellWorkflow(
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions,
        Config config
    ) {
        this.statements = statements;
        this.transactions = transactions;
        this.config = config;
    }

    public void showAccount(String alias) {
        TableView<CreditCardStatement> statementTable = config.statementTable().get();
        HBox totals = totalsPanel();
        VBox statementCards = new VBox(10);
        Runnable refreshTotals = () -> totals.getChildren().setAll(
            config.accumulatedTotals().nodes(alias, config.selectedYear().get(), config.selectedMonth().get())
        );
        Runnable refresh = () -> {
            statementTable.setItems(FXCollections.observableArrayList(
                statements.findByAccount(alias, config.selectedYear().get(), config.selectedMonth().get())
            ));
            refreshTotals.run();
        };
        refresh.run();
        VBox monthlyCards = config.monthlyCards().build(alias, statementTable, totals, statementCards);

        CardAccountDetailControls controls = new CardAccountDetailControls(
            config.selectedYearValue().get(),
            config.selectedMonthValue().get()
        );
        controls.filter().setOnAction(event -> {
            config.selectedPeriodChanged().accept(controls.year().getValue(), controls.month().getValue());
            refresh.run();
            monthlyCards.getChildren().setAll(
                config.monthlyCards().build(alias, statementTable, totals, statementCards).getChildren()
            );
        });
        controls.importPdf().setOnAction(event -> config.importPdf().accept(alias));
        controls.analysis().setOnAction(event -> config.showAnalysis().accept(alias));
        controls.addStatement().setOnAction(event -> config.addStatement().accept(alias, () -> showAccount(alias)));
        Label note = config.helperNote().apply("Haz clic en una card para abrir el detalle completo de ese periodo.");
        config.setPage().accept(config.page().build(
            "Tarjeta - " + alias,
            config.backButton().apply("Volver a Tarjetas", config.showCards()),
            controls.actions(),
            totals,
            note,
            monthlyCards
        ));
    }

    public void showPeriodDetail(String alias, int year, int month) {
        config.selectedPeriodChanged().accept(year, month);
        TableView<CreditCardStatement> statementTable = config.statementTable().get();
        TableView<CreditCardTransaction> movementTable = config.transactionTable().get();
        HBox totals = totalsPanel();
        VBox statementCards = new VBox(10);
        Runnable refreshTotals = () -> totals.getChildren().setAll(
            config.periodTotals().apply(statementTable.getItems())
        );
        Runnable refresh = () -> {
            statementTable.setItems(FXCollections.observableArrayList(statements.findByAccount(alias, year, month)));
            movementTable.setItems(FXCollections.observableArrayList(transactions.findByAccount(alias, year, month)));
            refreshTotals.run();
            config.statementCards().refresh(statementTable, statementCards, refreshTotals);
        };
        refresh.run();

        CardPeriodDetailView.View periodView = new CardPeriodDetailView().build(
            statementCards,
            movementTable,
            () -> config.saveStatements().save(statementTable, refresh),
            () -> config.addMovement().accept(statementTable, movementTable),
            () -> {
                movementTable.requestFocus();
                config.saveMovements().save(statementTable, movementTable, refresh);
            }
        );
        config.movementsTabChanged().accept(periodView.showMovements());
        String periodTitle = statementTable.getItems().isEmpty()
            ? String.format("%02d/%d", month, year)
            : statements.periodTitle(
                statementTable.getItems(),
                new MonthlySourceTotals(year, month, 0, 0, 0, 0, 0)
            );
        config.setPage().accept(config.page().build(
            "Tarjeta - " + alias + " - " + periodTitle,
            config.backButton().apply("Volver a la tarjeta", () -> showAccount(alias)),
            totals,
            periodView.tabs()
        ));
    }

    private HBox totalsPanel() {
        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        return totals;
    }

    public record Config(
        Consumer<Parent> setPage,
        PageFactory page,
        BiFunction<String, Runnable, Node> backButton,
        Runnable showCards,
        Supplier<TableView<CreditCardStatement>> statementTable,
        Supplier<TableView<CreditCardTransaction>> transactionTable,
        Supplier<Integer> selectedYear,
        Supplier<Integer> selectedMonth,
        Supplier<Integer> selectedYearValue,
        Supplier<Integer> selectedMonthValue,
        BiConsumer<Integer, Integer> selectedPeriodChanged,
        AccumulatedTotalsFactory accumulatedTotals,
        Function<List<CreditCardStatement>, List<Node>> periodTotals,
        MonthlyCardsFactory monthlyCards,
        StatementCardsRefresher statementCards,
        Consumer<String> importPdf,
        Consumer<String> showAnalysis,
        BiConsumer<String, Runnable> addStatement,
        Function<String, Label> helperNote,
        SaveStatementsAction saveStatements,
        BiConsumer<TableView<CreditCardStatement>, TableView<CreditCardTransaction>> addMovement,
        SaveMovementsAction saveMovements,
        Consumer<Runnable> movementsTabChanged
    ) {}

    @FunctionalInterface
    public interface PageFactory {
        Parent build(String title, Node... nodes);
    }

    @FunctionalInterface
    public interface AccumulatedTotalsFactory {
        List<Node> nodes(String alias, Integer year, Integer month);
    }

    @FunctionalInterface
    public interface MonthlyCardsFactory {
        VBox build(String alias, TableView<CreditCardStatement> statements, HBox totals, VBox statementCards);
    }

    @FunctionalInterface
    public interface StatementCardsRefresher {
        void refresh(TableView<CreditCardStatement> statements, VBox statementCards, Runnable refreshTotals);
    }

    @FunctionalInterface
    public interface SaveStatementsAction {
        void save(TableView<CreditCardStatement> statements, Runnable refresh);
    }

    @FunctionalInterface
    public interface SaveMovementsAction {
        void save(
            TableView<CreditCardStatement> statements,
            TableView<CreditCardTransaction> movements,
            Runnable refresh
        );
    }
}
