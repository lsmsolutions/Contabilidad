package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.controllers.bank.BankImportController;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.utils.Money;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BankPeriodDetailScreenView {
    private final BankApplicationService bank;
    private final BankImportController imports;

    public BankPeriodDetailScreenView(BankApplicationService bank, BankImportController imports) {
        this.bank = bank;
        this.imports = imports;
    }

    public VBox build(BankPeriodSummary period, Config config) {
        BankStatementPeriod statement = period.statementPeriod();
        SourceTotals totals = period.totals();
        HBox totalCards = new HBox(12);
        totalCards.getStyleClass().add("totals-panel");
        totalCards.getChildren().setAll(new BankTotalsView().build(totals, statement.openingBalance()));

        BankBreakdownView breakdown = new BankBreakdownView();
        Runnable[] rowsChanged = new Runnable[1];
        TableView<BankTransaction> table = new BankTransactionTableView(bank).build(() -> {
            if (rowsChanged[0] != null) {
                rowsChanged[0].run();
            }
        });
        table.setItems(FXCollections.observableArrayList(period.transactions()));
        table.setMinHeight(420);
        table.setPrefHeight(520);
        VBox.setVgrow(table, Priority.ALWAYS);
        rowsChanged[0] = () -> {
            SourceTotals currentTotals = bank.transactions().totalsFromRows(table.getItems());
            totalCards.getChildren().setAll(new BankTotalsView().build(currentTotals, statement.openingBalance()));
            breakdown.refresh(table.getItems());
        };

        Button add = new Button("Añadir registro");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> {
            BankTransaction transaction = imports.createManualTransaction(
                statement.accountAlias(), statement.periodStart(), statement
            );
            table.getItems().add(transaction);
            int index = table.getItems().size() - 1;
            table.getSelectionModel().select(transaction);
            table.scrollTo(transaction);
            table.edit(index, table.getColumns().get(0));
        });

        Button save = new Button("Guardar");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> {
            table.requestFocus();
            saveRows(table);
            config.info().accept("Banco guardado", "Cambios del periodo guardados.");
            config.reload().run();
        });

        Button edit = new Button("Editar periodo");
        edit.setOnAction(event -> config.editPeriod().accept(period));
        Button download = new Button("Descargar");
        download.setOnAction(event -> config.downloadPeriod().accept(statement));
        Button delete = new Button("Eliminar periodo");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> config.deletePeriod().accept(period));

        HBox movementActions = new HBox(10, add, save, edit, download, delete);
        movementActions.getStyleClass().add("action-row");
        VBox movements = new VBox(10, movementActions, table);

        breakdown.refresh(table.getItems());
        Tab movementsTab = tab("Movements", movements);
        Tab breakdownTab = tab("Breakdown", breakdown.node());
        breakdownTab.setOnSelectionChanged(event -> {
            if (breakdownTab.isSelected()) {
                breakdown.refresh(table.getItems());
            }
        });
        TabPane tabs = new TabPane(movementsTab, breakdownTab);
        tabs.getStyleClass().add("bank-detail-tabs");

        Label heading = new Label(config.title() + " - " + BankPeriodTextFormatter.title(statement));
        heading.getStyleClass().add("heading");
        VBox page = new VBox(18, heading, config.backButton(), totalCards, periodSummary(period), tabs);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        return page;
    }

    private Node periodSummary(BankPeriodSummary period) {
        BankStatementPeriod statement = period.statementPeriod();
        SourceTotals totals = period.totals();
        double calculatedEnding = statement.openingBalance() + totals.net();
        GridPane grid = new GridPane();
        grid.getStyleClass().add("bank-period-detail-grid");
        for (int index = 0; index < 8; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(12.5);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }
        summaryItem(grid, 0, "Periodo", BankPeriodTextFormatter.title(statement));
        summaryItem(grid, 1, "Saldo inicial", Money.format(statement.openingBalance()));
        summaryItem(grid, 2, "Depósitos", Money.format(totals.income()));
        summaryItem(grid, 3, "Salidas", Money.format(Math.abs(totals.expenses())));
        summaryItem(grid, 4, "Movimiento neto", Money.format(totals.net()));
        summaryItem(grid, 5, "Pendientes", String.valueOf(totals.pendingCount()));
        summaryItem(grid, 6, "Saldo final calculado", Money.format(calculatedEnding));
        summaryItem(grid, 7, "Saldo final PDF", statement.hasStatementEndingBalance()
            ? Money.format(statement.statementEndingBalance()) : "Pendiente");
        return grid;
    }

    private void summaryItem(GridPane grid, int column, String title, String value) {
        Label label = new Label(title);
        label.getStyleClass().add("bank-period-detail-label");
        label.setWrapText(true);
        Label amount = new Label(value);
        amount.getStyleClass().add("bank-period-detail-value");
        amount.setWrapText(true);
        VBox item = new VBox(4, label, amount);
        item.getStyleClass().add("bank-period-detail-item");
        grid.add(item, column, 0);
    }

    private void saveRows(TableView<BankTransaction> table) {
        for (BankTransaction transaction : table.getItems()) {
            bank.transactions().normalizeSign(transaction);
            if (transaction.getId() > 0) {
                bank.transactions().update(transaction);
            } else {
                transaction.setId(bank.transactions().save(transaction));
            }
        }
    }

    private Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public record Config(
        String title,
        Node backButton,
        Runnable reload,
        BiMessageConsumer info,
        Consumer<BankPeriodSummary> editPeriod,
        Consumer<BankStatementPeriod> downloadPeriod,
        Consumer<BankPeriodSummary> deletePeriod
    ) {}

    @FunctionalInterface
    public interface BiMessageConsumer {
        void accept(String title, String message);
    }
}
