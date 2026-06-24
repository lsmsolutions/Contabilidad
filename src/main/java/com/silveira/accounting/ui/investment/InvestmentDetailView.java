package com.silveira.accounting.ui.investment;

import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import com.silveira.accounting.utils.Money;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class InvestmentDetailView {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VBox build(
        InvestmentAccount account,
        List<InvestmentStatement> statements,
        InvestmentStatement selected,
        List<InvestmentAllocation> allocations,
        List<InvestmentPosition> positions,
        List<InvestmentTransaction> transactions,
        Runnable back,
        Runnable importPdf,
        Consumer<Long> selectStatement,
        Consumer<InvestmentStatement> deleteStatement
    ) {
        Label heading = new Label(account.getAlias());
        heading.getStyleClass().add("heading");
        Label subtitle = new Label(account.getProviderName() + " | " + account.getAccountType()
            + (blank(account.getAccountNumber()) ? "" : " | Account ending in " + ending(account.getAccountNumber())));
        subtitle.getStyleClass().add("section-subtitle");
        Button backButton = new Button("\u2190 Inversiones");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(event -> back.run());
        Button importButton = new Button("Import PDF");
        importButton.getStyleClass().add("primary");
        importButton.setOnAction(event -> importPdf.run());

        FlowPane periods = new FlowPane(12, 12);
        periods.getStyleClass().add("monthly-card-row");
        for (InvestmentStatement statement : statements) {
            Label month = new Label(statement.getPeriodEnd().format(MONTH));
            month.getStyleClass().add("monthly-card-title");
            GridPane values = new GridPane();
            values.getStyleClass().add("monthly-card-grid");
            addLine(values, 0, "Ending value", Money.format(statement.getEndingValue()));
            addLine(values, 1, "Market change", Money.format(statement.getMarketChange()));
            addLine(values, 2, "Unrealized gain/loss", Money.format(statement.getUnrealizedGainLoss()));
            VBox period = new VBox(0, month, values);
            period.getStyleClass().add("monthly-card");
            period.setOnMouseClicked(event -> selectStatement.accept(statement.getId()));
            periods.getChildren().add(period);
        }

        VBox detail = selected == null
            ? new VBox(new Label("Import a monthly statement to view the investment details."))
            : statementDetail(selected, allocations, positions, transactions, deleteStatement);

        VBox page = new VBox(16, heading, subtitle, new HBox(10, backButton, importButton), periods, detail);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        VBox.setVgrow(detail, Priority.ALWAYS);
        return page;
    }

    private VBox statementDetail(
        InvestmentStatement statement,
        List<InvestmentAllocation> allocations,
        List<InvestmentPosition> positions,
        List<InvestmentTransaction> transactions,
        Consumer<InvestmentStatement> deleteStatement
    ) {
        Label period = new Label(statement.getPeriodStart().format(DATE) + " - " + statement.getPeriodEnd().format(DATE));
        period.getStyleClass().add("section-title");
        Button delete = new Button("Delete period");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteStatement.accept(statement));

        TabPane tabs = new TabPane(
            tab("Summary", summary(statement, allocations)),
            tab("Positions", tableScroll(positionTable(positions))),
            tab("Transactions", tableScroll(transactionTable(transactions))),
            tab("Cash / Sweep", cashSweep(allocations, transactions))
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return new VBox(12, new HBox(12, period, delete), tabs);
    }

    private Node summary(InvestmentStatement statement, List<InvestmentAllocation> allocations) {
        FlowPane totals = new FlowPane(12, 12);
        totals.getChildren().addAll(
            metric("Beginning value", statement.getBeginningValue()),
            metric("Ending value", statement.getEndingValue()),
            metric("Deposits", statement.getDeposits()),
            metric("Withdrawals", statement.getWithdrawals()),
            metric("Dividends & interest", statement.getDividendsInterest()),
            metric("Market change", statement.getMarketChange()),
            metric("Expenses", statement.getExpenses()),
            metric("Unrealized gain/loss", statement.getUnrealizedGainLoss())
        );

        GridPane allocation = new GridPane();
        allocation.getStyleClass().add("statement-section");
        allocation.setHgap(24);
        allocation.setVgap(8);
        Label title = new Label("Asset Allocation");
        title.getStyleClass().add("statement-section-title");
        allocation.add(title, 0, 0, 3, 1);
        allocation.add(new Label("Asset"), 0, 1);
        allocation.add(new Label("Market value"), 1, 1);
        allocation.add(new Label("Allocation"), 2, 1);
        int row = 2;
        for (InvestmentAllocation value : allocations) {
            allocation.add(new Label(value.getCategory()), 0, row);
            allocation.add(new Label(Money.format(value.getMarketValue())), 1, row);
            allocation.add(new Label(String.format(Locale.US, "%.0f%%", value.getPercentage())), 2, row++);
        }
        return new VBox(14, totals, allocation);
    }

    private VBox metric(String label, double value) {
        Label name = new Label(label);
        name.getStyleClass().add("monthly-card-line");
        Label amount = new Label(Money.format(value));
        amount.getStyleClass().add("monthly-card-value-strong");
        VBox box = new VBox(5, name, amount);
        box.getStyleClass().add("summary-card");
        box.setPrefWidth(190);
        return box;
    }

    private TableView<InvestmentPosition> positionTable(List<InvestmentPosition> values) {
        TableView<InvestmentPosition> table = new TableView<>();
        table.getItems().setAll(values);
        table.getColumns().add(column("Symbol", value -> value.getSymbol(), 90));
        table.getColumns().add(column("Description", InvestmentPosition::getDescription, 260));
        table.getColumns().add(column("Type", InvestmentPosition::getAssetType, 120));
        table.getColumns().add(column("Quantity", value -> number(value.getQuantity(), 4), 100));
        table.getColumns().add(column("Price", value -> Money.format(value.getPrice()), 110));
        table.getColumns().add(column("Market value", value -> Money.format(value.getMarketValue()), 125));
        table.getColumns().add(column("Cost basis", value -> Money.format(value.getCostBasis()), 125));
        table.getColumns().add(column("Unrealized gain/loss", value -> Money.format(value.getUnrealizedGainLoss()), 155));
        table.setPlaceholder(new Label("No positions found in this statement."));
        return table;
    }

    private TableView<InvestmentTransaction> transactionTable(List<InvestmentTransaction> values) {
        TableView<InvestmentTransaction> table = new TableView<>();
        table.getItems().setAll(values);
        table.getColumns().add(column("Date", value -> value.getTransactionDate() == null ? "" : value.getTransactionDate().format(DATE), 100));
        table.getColumns().add(column("Action", InvestmentTransaction::getAction, 100));
        table.getColumns().add(column("Symbol", InvestmentTransaction::getSymbol, 90));
        table.getColumns().add(column("Description", InvestmentTransaction::getDescription, 360));
        table.getColumns().add(column("Quantity", value -> number(value.getQuantity(), 4), 100));
        table.getColumns().add(column("Price", value -> Money.format(value.getPrice()), 110));
        table.getColumns().add(column("Amount", value -> Money.format(value.getAmount()), 120));
        table.getColumns().add(column("Realized gain/loss", value -> Money.format(value.getRealizedGainLoss()), 145));
        table.setPlaceholder(new Label("No transactions found in this statement."));
        return table;
    }

    private Node cashSweep(List<InvestmentAllocation> allocations, List<InvestmentTransaction> transactions) {
        double cash = allocations.stream()
            .filter(value -> value.getCategory().toLowerCase(Locale.ROOT).contains("cash"))
            .mapToDouble(InvestmentAllocation::getMarketValue)
            .sum();
        Label value = new Label(Money.format(cash));
        value.getStyleClass().add("monthly-card-value-strong");
        VBox summary = new VBox(6, new Label("Ending cash and cash investments"), value);
        summary.getStyleClass().add("statement-section");
        List<InvestmentTransaction> cashTransactions = transactions.stream()
            .filter(item -> item.getAction().equals("Deposit")
                || item.getAction().equals("Withdrawal")
                || item.getAction().equals("Interest"))
            .toList();
        return new VBox(14, summary, tableScroll(transactionTable(cashTransactions)));
    }

    private ScrollPane tableScroll(TableView<?> table) {
        table.setPrefHeight(430);
        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private <T> TableColumn<T, String> column(String title, java.util.function.Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private Tab tab(String title, Node content) {
        return new Tab(title, content);
    }

    private void addLine(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(valueText);
        value.getStyleClass().add("monthly-card-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private String number(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    private String ending(String value) {
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
