package com.silveira.accounting.ui.investment;

import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.geometry.Side;
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
        BiConsumer<InvestmentStatement, List<InvestmentPosition>> savePositions,
        BiConsumer<InvestmentStatement, List<InvestmentTransaction>> saveTransactions,
        Consumer<InvestmentStatement> editStatement,
        Consumer<InvestmentStatement> deleteStatement,
        Function<InvestmentStatement, Node> reviewMark
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
            Node mark = reviewMark.apply(statement);
            HBox titleRow = new HBox(month, mark);
            titleRow.getStyleClass().addAll("monthly-card-title-row", "investment-period-title-row");
            HBox.setHgrow(month, Priority.ALWAYS);
            GridPane values = new GridPane();
            values.getStyleClass().add("monthly-card-grid");
            addInvestmentCardLine(
                values,
                0,
                "Ending value",
                Money.format(statement.getEndingValue()),
                "Valor total actual de tu cartera.\n\nIncluye todas las inversiones y el efectivo disponible al final del periodo seleccionado.",
                "investment-card-value-neutral"
            );
            addInvestmentCardLine(
                values,
                1,
                "Cost basis total",
                Money.format(statement.getCostBasisTotal()),
                "Importe total invertido en las inversiones actuales.\n\nRepresenta el dinero que pagaste por las inversiones que aun conservas.",
                "investment-card-value-basis"
            );
            addInvestmentCardLine(
                values,
                2,
                "Unrealized gain/loss",
                signedMoney(statement.getUnrealizedGainLoss()),
                "Diferencia entre el valor actual de tus inversiones y lo que pagaste por ellas.\n\n"
                    + "Positivo: las inversiones valen mas de lo que costaron.\n"
                    + "Negativo: las inversiones valen menos de lo que costaron.\n\n"
                    + "La ganancia o perdida solo sera definitiva cuando vendas las inversiones.",
                signedStyle(statement.getUnrealizedGainLoss())
            );
            addInvestmentCardLine(
                values,
                3,
                "Market change (this period)",
                signedMoney(statement.getMarketChange()),
                "Cambio en el valor de la cartera debido unicamente a la subida o bajada del mercado durante el periodo seleccionado.\n\n"
                    + "No incluye ingresos, retiradas ni dividendos.",
                signedStyle(statement.getMarketChange())
            );
            VBox period = new VBox(0, titleRow, values);
            period.getStyleClass().add("monthly-card");
            if (selected != null && selected.getId() == statement.getId()) {
                period.getStyleClass().add("investment-selected-period-card");
            }
            period.setOnMouseClicked(event -> {
                event.consume();
                selectStatement.accept(statement.getId());
            });
            periods.getChildren().add(period);
        }

        VBox detail = selected == null
            ? new VBox(new Label("Import a monthly statement to view the investment details."))
            : statementDetail(selected, allocations, positions, transactions, savePositions, saveTransactions, editStatement, deleteStatement);

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
        BiConsumer<InvestmentStatement, List<InvestmentPosition>> savePositions,
        BiConsumer<InvestmentStatement, List<InvestmentTransaction>> saveTransactions,
        Consumer<InvestmentStatement> editStatement,
        Consumer<InvestmentStatement> deleteStatement
    ) {
        Label period = new Label(statement.getPeriodStart().format(DATE) + " - " + statement.getPeriodEnd().format(DATE));
        period.getStyleClass().add("section-title");
        Button edit = new Button("Editar periodo");
        edit.setOnAction(event -> editStatement.accept(statement));
        Button delete = new Button("Delete period");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteStatement.accept(statement));
        uniformButtons(edit, delete);
        HBox periodActions = new HBox(12, period, edit, delete);
        periodActions.getStyleClass().add("investment-period-actions");
        periodActions.setPadding(new Insets(0, 0, 10, 0));

        TabPane tabs = new TabPane(
            tab("Summary", summary(statement, allocations)),
            tab("Positions", positionsTab(statement, positions, savePositions)),
            tab("Transactions", transactionsTab(statement, allocations, transactions, saveTransactions))
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        return new VBox(14, periodActions, tabs);
    }

    private Node summary(InvestmentStatement statement, List<InvestmentAllocation> allocations) {
        HBox valueEquation = new HBox(12,
            summaryMetric("Beginning Value\nas of " + statement.getPeriodStart().format(DATE), statement.getBeginningValue(), false),
            operator("+"),
            summaryMetric("Transfer of\nSecurities(In/Out)", statement.getTransferOfSecurities(), false),
            operator("+"),
            summaryMetric("Dividends\nReinvested", statement.getDividendsReinvested(), false),
            operator("+"),
            summaryMetric("Cash Activity", statement.getCashActivity(), false),
            operator("+"),
            summaryMetric("Change in\nMarket Value", statement.getChangeInMarketValue(), false),
            operator("="),
            summaryMetric("Ending Value\nas of " + statement.getPeriodEnd().format(DATE), statement.getEndingValue(), true)
        );
        valueEquation.getStyleClass().add("statement-section");

        HBox gainBasis = new HBox(18,
            summaryMetric("Cost Basis", statement.getCostBasisTotal(), false),
            summaryMetric("Unrealized\nGain/(Loss)", statement.getUnrealizedGainLoss(), false)
        );
        gainBasis.getStyleClass().add("statement-section");

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
        double totalMarketValue = 0;
        double totalPercentage = 0;
        for (InvestmentAllocation value : allocations) {
            allocation.add(new Label(value.getCategory()), 0, row);
            Label marketValue = new Label(Money.format(value.getMarketValue()));
            marketValue.getStyleClass().add("investment-cash-value");
            Label percentage = new Label(String.format(Locale.US, "%.0f%%", value.getPercentage()));
            percentage.getStyleClass().add("investment-cash-value");
            allocation.add(marketValue, 1, row);
            allocation.add(percentage, 2, row++);
            totalMarketValue += value.getMarketValue();
            totalPercentage += value.getPercentage();
        }
        Label totalLabel = new Label("Total");
        Label totalMarket = new Label(Money.format(totalMarketValue));
        Label totalAllocation = new Label(String.format(Locale.US, "%.0f%%", totalPercentage));
        totalLabel.getStyleClass().add("investment-allocation-total-cell");
        totalMarket.getStyleClass().addAll("investment-allocation-total-cell", "investment-cash-ending-value");
        totalAllocation.getStyleClass().add("investment-allocation-total-cell");
        allocation.add(totalLabel, 0, row);
        allocation.add(totalMarket, 1, row);
        allocation.add(totalAllocation, 2, row);
        return new VBox(14, valueEquation, gainBasis, allocation);
    }

    private VBox equationMetric(String label, double value) {
        Label name = new Label(label);
        name.getStyleClass().add("monthly-card-line");
        Label amount = new Label(Money.format(value));
        amount.getStyleClass().add("monthly-card-value-strong");
        VBox box = new VBox(5, name, amount);
        box.getStyleClass().add("summary-card");
        box.setPrefWidth(190);
        return box;
    }

    private VBox summaryMetric(String label, double value, boolean ending) {
        Label amount = new Label(Money.format(value));
        amount.getStyleClass().add(ending ? "investment-cash-ending-value" : "investment-cash-value");
        Label name = new Label(label);
        name.getStyleClass().add("monthly-card-line");
        VBox box = new VBox(5, amount, name);
        box.getStyleClass().add("summary-card");
        box.setPrefWidth(190);
        return box;
    }

    private VBox cashMetric(String label, double value, boolean ending) {
        Label amount = new Label(Money.format(value));
        amount.getStyleClass().add(ending ? "investment-cash-ending-value" : "investment-cash-value");
        Label name = new Label(label);
        name.getStyleClass().add("monthly-card-line");
        VBox box = new VBox(5, amount, name);
        box.getStyleClass().add("summary-card");
        box.setPrefWidth(150);
        return box;
    }

    private VBox positionMetric(String label, double value) {
        Label amount = new Label(Money.format(value));
        amount.getStyleClass().add("investment-cash-value");
        Label name = new Label(label);
        name.getStyleClass().add("monthly-card-line");
        VBox box = new VBox(5, amount, name);
        box.getStyleClass().add("summary-card");
        box.setPrefWidth(190);
        return box;
    }

    private Label operator(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("monthly-card-value-strong");
        return label;
    }

    private double cashActivity(InvestmentStatement statement) {
        return statement.getEndingValue()
            - statement.getBeginningValue()
            - statement.getDeposits()
            - statement.getWithdrawals()
            - statement.getDividendsInterest()
            - statement.getMarketChange();
    }

    private Node positionsTab(
        InvestmentStatement statement,
        List<InvestmentPosition> values,
        BiConsumer<InvestmentStatement, List<InvestmentPosition>> savePositions
    ) {
        ObservableList<InvestmentPosition> rows = FXCollections.observableArrayList(values);
        ObservableList<InvestmentPosition> displayRows = FXCollections.observableArrayList(positionRowsWithTotal(rows));
        HBox totals = new HBox(12);
        totals.getStyleClass().add("statement-section");
        Runnable refreshTotals = () -> {
            totals.getChildren().setAll(positionEquation(rows));
            displayRows.setAll(positionRowsWithTotal(rows));
        };
        TableView<InvestmentPosition> table = positionTable(displayRows, refreshTotals);
        refreshTotals.run();

        Button add = new Button("Añadir posición");
        add.setOnAction(event -> {
            InvestmentPosition position = new InvestmentPosition();
            position.setStatementId(statement.getId());
            position.setAssetType("Manual");
            position.setSymbol("");
            position.setDescription("");
            rows.add(position);
            table.getSelectionModel().select(position);
            refreshTotals.run();
        });
        Button delete = new Button("Eliminar posición");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> {
            InvestmentPosition selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isTotal()) {
                rows.remove(selected);
                refreshTotals.run();
            }
        });
        Button save = new Button("Guardar posiciones");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> savePositions.accept(statement, List.copyOf(rows)));

        HBox actions = new HBox(10, add, delete, save);
        actions.getStyleClass().add("investment-tab-actions");
        uniformButtons(add, delete, save);
        actions.setPadding(new Insets(14, 0, 10, 0));
        VBox footer = new VBox(8, totals);
        footer.getStyleClass().add("statement-section");
        VBox content = new VBox(10, actions, dynamicTableScroll(table), footer);
        content.getStyleClass().add("investment-tab-content");
        return content;
    }

    private TableView<InvestmentPosition> positionTable(ObservableList<InvestmentPosition> values, Runnable refreshTotals) {
        TableView<InvestmentPosition> table = new TableView<>();
        table.setEditable(true);
        table.setItems(values);
        table.setRowFactory(view -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(InvestmentPosition item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("investment-subtotal-row");
                if (!empty && item != null && item.isTotal()) {
                    getStyleClass().add("investment-subtotal-row");
                }
            }
        });
        table.getColumns().add(editableColumn("Symbol", InvestmentPosition::getSymbol, InvestmentPosition::setSymbol, 90, refreshTotals));
        table.getColumns().add(editableColumn("Description", InvestmentPosition::getDescription, InvestmentPosition::setDescription, 260, refreshTotals));
        table.getColumns().add(editableColumn("Type", InvestmentPosition::getAssetType, InvestmentPosition::setAssetType, 120, refreshTotals));
        table.getColumns().add(editableColumn("Quantity", value -> number(value.getQuantity(), 4), (value, text) -> value.setQuantity(decimal(text)), 100, refreshTotals));
        table.getColumns().add(editableColumn("Price", value -> Money.format(value.getPrice()), (value, text) -> value.setPrice(decimal(text)), 110, refreshTotals));
        table.getColumns().add(editableColumn("Market value", value -> Money.format(value.getMarketValue()), (value, text) -> value.setMarketValue(decimal(text)), 125, refreshTotals));
        table.getColumns().add(editableColumn("Cost basis", value -> Money.format(value.getCostBasis()), (value, text) -> value.setCostBasis(decimal(text)), 125, refreshTotals));
        table.getColumns().add(editableColumn("Unrealized gain/loss", value -> Money.format(value.getUnrealizedGainLoss()), (value, text) -> value.setUnrealizedGainLoss(decimal(text)), 155, refreshTotals));
        table.setPlaceholder(new Label("No positions found in this statement."));
        return table;
    }

    private List<InvestmentPosition> positionRowsWithTotal(List<InvestmentPosition> values) {
        List<InvestmentPosition> result = new ArrayList<>(values);
        InvestmentPosition total = new InvestmentPosition();
        total.setTotal(true);
        total.setSymbol("Total");
        total.setDescription("Total Unrealized Gain/Loss");
        total.setQuantity(0);
        total.setPrice(0);
        total.setMarketValue(values.stream().mapToDouble(InvestmentPosition::getMarketValue).sum());
        total.setCostBasis(values.stream().mapToDouble(InvestmentPosition::getCostBasis).sum());
        total.setUnrealizedGainLoss(values.stream().mapToDouble(InvestmentPosition::getUnrealizedGainLoss).sum());
        result.add(total);
        return result;
    }

    private List<Node> positionEquation(List<InvestmentPosition> values) {
        double marketValue = values.stream().mapToDouble(InvestmentPosition::getMarketValue).sum();
        double costBasis = values.stream().mapToDouble(InvestmentPosition::getCostBasis).sum();
        double gainLoss = values.stream().mapToDouble(InvestmentPosition::getUnrealizedGainLoss).sum();
        return List.of(
            positionMetric("Total Unrealized\nGain/(Loss)", gainLoss),
            operator("="),
            positionMetric("Total Market Value", marketValue),
            operator("-"),
            positionMetric("Total Cost Basis", costBasis)
        );
    }

    private double totalPurchase(List<InvestmentTransaction> values) {
        return values.stream()
            .filter(value -> value.getAction() != null && value.getAction().equalsIgnoreCase("Purchase"))
            .mapToDouble(InvestmentTransaction::getAmount)
            .sum();
    }

    private Comparator<InvestmentTransaction> transactionGroupOrder() {
        return Comparator
            .comparingInt((InvestmentTransaction value) -> groupRank(transactionCategory(value)))
            .thenComparing(value -> transactionCategory(value))
            .thenComparing(value -> value.getTransactionDate() == null ? LocalDate.MAX : value.getTransactionDate())
            .thenComparing(value -> value.getDescription() == null ? "" : value.getDescription());
    }

    private int groupRank(String group) {
        return switch (group) {
            case "Purchases" -> 1;
            case "Dividends/Interest" -> 2;
            case "Sales/Redemptions" -> 3;
            case "Deposits" -> 4;
            case "Withdrawals" -> 5;
            case "Expenses/Fees" -> 6;
            default -> 7;
        };
    }

    private List<InvestmentTransaction> groupedTransactionRows(List<InvestmentTransaction> values) {
        Map<String, List<InvestmentTransaction>> grouped = new LinkedHashMap<>();
        values.stream().map(this::withDefaultCategory).sorted(transactionGroupOrder()).forEach(value ->
            grouped.computeIfAbsent(transactionCategory(value), ignored -> new ArrayList<>()).add(value)
        );
        List<InvestmentTransaction> result = new ArrayList<>();
        for (Map.Entry<String, List<InvestmentTransaction>> entry : grouped.entrySet()) {
            result.addAll(entry.getValue());
            result.add(subtotalRow(entry.getKey(), entry.getValue()));
        }
        result.add(totalRow(values));
        return result;
    }

    private InvestmentTransaction subtotalRow(String category, List<InvestmentTransaction> values) {
        InvestmentTransaction subtotal = new InvestmentTransaction();
        subtotal.setSubtotal(true);
        subtotal.setCategory(category);
        subtotal.setAction("Subtotal");
        subtotal.setDescription("Subtotal " + category);
        subtotal.setAmount(values.stream().mapToDouble(InvestmentTransaction::getAmount).sum());
        subtotal.setRealizedGainLoss(values.stream().mapToDouble(InvestmentTransaction::getRealizedGainLoss).sum());
        return subtotal;
    }

    private InvestmentTransaction totalRow(List<InvestmentTransaction> values) {
        InvestmentTransaction total = new InvestmentTransaction();
        total.setSubtotal(true);
        total.setCategory("Total");
        total.setAction("Total");
        total.setDescription("Total Amount");
        total.setAmount(values.stream().mapToDouble(InvestmentTransaction::getAmount).sum());
        total.setRealizedGainLoss(values.stream().mapToDouble(InvestmentTransaction::getRealizedGainLoss).sum());
        return total;
    }

    private InvestmentTransaction withDefaultCategory(InvestmentTransaction value) {
        if (value.getCategory() == null || value.getCategory().isBlank()) {
            value.setCategory(categoryFromAction(value));
        }
        return value;
    }

    private String transactionCategory(InvestmentTransaction value) {
        return value.getCategory() == null || value.getCategory().isBlank()
            ? categoryFromAction(value)
            : value.getCategory().trim();
    }

    private String categoryFromAction(InvestmentTransaction value) {
        String action = value.getAction() == null ? "" : value.getAction().trim().toLowerCase(Locale.ROOT);
        if (action.equals("purchase")) {
            return "Purchases";
        }
        if (action.equals("dividend") || action.equals("interest") || action.equals("reinvest")) {
            return "Dividends/Interest";
        }
        if (action.equals("sale") || action.equals("redemption")) {
            return "Sales/Redemptions";
        }
        if (action.equals("deposit")) {
            return "Deposits";
        }
        if (action.equals("withdrawal")) {
            return "Withdrawals";
        }
        if (action.equals("expense") || action.equals("fee")) {
            return "Expenses/Fees";
        }
        return "Other Activity";
    }

    private Label totalOnly(double value) {
        Label label = new Label(signedMoney(value));
        label.getStyleClass().add("monthly-card-value-strong");
        return label;
    }

    private Node transactionsTab(
        InvestmentStatement statement,
        List<InvestmentAllocation> allocations,
        List<InvestmentTransaction> values,
        BiConsumer<InvestmentStatement, List<InvestmentTransaction>> saveTransactions
    ) {
        ObservableList<InvestmentTransaction> rows = FXCollections.observableArrayList(
            values.stream().map(this::withDefaultCategory).sorted(transactionGroupOrder()).toList()
        );
        ObservableList<InvestmentTransaction> displayRows = FXCollections.observableArrayList(groupedTransactionRows(rows));
        Runnable[] refresh = new Runnable[1];
        TableView<InvestmentTransaction> table = transactionTable(displayRows, () -> refresh[0].run());
        refresh[0] = () -> {
            displayRows.setAll(groupedTransactionRows(rows));
            applyDynamicTableHeight(table);
        };
        refresh[0].run();

        Button add = new Button("Añadir movimiento");
        add.setOnAction(event -> {
            InvestmentTransaction transaction = new InvestmentTransaction();
            transaction.setStatementId(statement.getId());
            transaction.setTransactionDate(statement.getPeriodEnd());
            transaction.setCategory("Other Activity");
            transaction.setAction("Manual");
            transaction.setSymbol("");
            transaction.setDescription("");
            rows.add(transaction);
            table.getSelectionModel().select(transaction);
            refresh[0].run();
        });
        Button edit = new Button("Editar");
        edit.setOnAction(event -> {
            int selected = table.getSelectionModel().getSelectedIndex();
            InvestmentTransaction row = table.getSelectionModel().getSelectedItem();
            if (selected >= 0 && row != null && !row.isSubtotal() && !table.getColumns().isEmpty()) {
                table.requestFocus();
                table.getFocusModel().focus(selected, table.getColumns().get(0));
                table.edit(selected, table.getColumns().get(0));
            }
        });
        Button delete = new Button("Eliminar movimiento");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> {
            InvestmentTransaction selected = table.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isSubtotal()) {
                rows.remove(selected);
                refresh[0].run();
            }
        });
        Button save = new Button("Guardar movimientos");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> saveTransactions.accept(statement, List.copyOf(rows)));

        HBox actions = new HBox(10, add, edit, delete, save);
        actions.getStyleClass().add("investment-tab-actions");
        uniformButtons(add, edit, delete, save);
        actions.setPadding(new Insets(14, 0, 10, 0));
        VBox cashSummary = cashTransactionSummary(statement, allocations, rows);
        VBox content = new VBox(10, actions, dynamicTableScroll(table), cashSummary);
        content.getStyleClass().add("investment-tab-content");
        return content;
    }

    private TableView<InvestmentTransaction> transactionTable(ObservableList<InvestmentTransaction> values, Runnable refreshTotal) {
        TableView<InvestmentTransaction> table = new TableView<>();
        table.setEditable(true);
        table.setItems(values);
        table.setRowFactory(view -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(InvestmentTransaction item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("investment-subtotal-row", "investment-total-row");
                if (!empty && item != null && item.isSubtotal()) {
                    getStyleClass().add("Total".equals(item.getCategory()) ? "investment-total-row" : "investment-subtotal-row");
                }
            }
        });
        table.getColumns().add(editableColumn("Date", value -> value.getTransactionDate() == null ? "" : value.getTransactionDate().format(DATE), (value, text) -> value.setTransactionDate(date(text)), 100, refreshTotal));
        table.getColumns().add(editableColumn("Category", InvestmentTransaction::getCategory, InvestmentTransaction::setCategory, 150, refreshTotal));
        table.getColumns().add(editableColumn("Action", InvestmentTransaction::getAction, InvestmentTransaction::setAction, 100, refreshTotal));
        table.getColumns().add(editableColumn("Symbol", InvestmentTransaction::getSymbol, InvestmentTransaction::setSymbol, 90, refreshTotal));
        table.getColumns().add(editableColumn("Description", InvestmentTransaction::getDescription, InvestmentTransaction::setDescription, 360, refreshTotal));
        table.getColumns().add(editableColumn("Quantity", value -> number(value.getQuantity(), 4), (value, text) -> value.setQuantity(decimal(text)), 100, refreshTotal));
        table.getColumns().add(editableColumn("Price", value -> Money.format(value.getPrice()), (value, text) -> value.setPrice(decimal(text)), 110, refreshTotal));
        table.getColumns().add(transactionAmountColumn(refreshTotal));
        table.getColumns().add(editableColumn("Realized gain/loss", value -> Money.format(value.getRealizedGainLoss()), (value, text) -> value.setRealizedGainLoss(decimal(text)), 145, refreshTotal));
        table.setPlaceholder(new Label("No transactions found in this statement."));
        return table;
    }

    private VBox cashTransactionSummary(
        InvestmentStatement statement,
        List<InvestmentAllocation> allocations,
        List<InvestmentTransaction> transactions
    ) {
        double endingCash = endingCash(allocations);
        double deposits = totalByCategory(transactions, "Deposits");
        double withdrawals = totalByCategory(transactions, "Withdrawals");
        double purchases = totalByCategory(transactions, "Purchases");
        double salesRedemptions = totalByCategory(transactions, "Sales/Redemptions");
        double dividendsInterest = totalByCategory(transactions, "Dividends/Interest");
        double expenses = totalByCategory(transactions, "Expenses/Fees");
        double beginningCash = endingCash - deposits - withdrawals - purchases - salesRedemptions - dividendsInterest - expenses;
        double otherActivity = totalByCategory(transactions, "Other Activity");

        HBox cashEquation = new HBox(12,
            cashMetric("Beginning Cash*\nas of " + statement.getPeriodStart().format(DATE), beginningCash, false),
            operator("+"),
            cashMetric("Deposits", deposits, false),
            operator("+"),
            cashMetric("Withdrawals", withdrawals, false),
            operator("+"),
            cashMetric("Purchases", purchases, false),
            operator("+"),
            cashMetric("Sales/Redemptions", salesRedemptions, false),
            operator("+"),
            cashMetric("Dividends/Interest", dividendsInterest, false),
            operator("+"),
            cashMetric("Expenses", expenses, false),
            operator("="),
            cashMetric("Ending Cash*\nas of " + statement.getPeriodEnd().format(DATE), endingCash, true)
        );
        cashEquation.getStyleClass().add("statement-section");

        Label cashTitle = new Label("Cash");
        cashTitle.getStyleClass().add("statement-section-title");

        HBox other = new HBox(12, cashMetric("Other Activity", otherActivity, false));
        Label note = new Label("Other Activity includes transactions which don't affect the cash balance, such as stock transfers, splits, etc.");
        note.getStyleClass().add("monthly-card-line");
        HBox otherRow = new HBox(14, other, note);
        otherRow.getStyleClass().add("statement-section");
        return new VBox(8, cashTitle, cashEquation, otherRow);
    }

    private double endingCash(List<InvestmentAllocation> allocations) {
        return allocations.stream()
            .filter(value -> value.getCategory() != null && value.getCategory().toLowerCase(Locale.ROOT).contains("cash"))
            .mapToDouble(InvestmentAllocation::getMarketValue)
            .sum();
    }

    private double totalByCategory(List<InvestmentTransaction> values, String category) {
        return values.stream()
            .filter(value -> !value.isSubtotal())
            .filter(value -> transactionCategory(value).equals(category))
            .mapToDouble(InvestmentTransaction::getAmount)
            .sum();
    }

    private Node cashSweep(List<InvestmentAllocation> allocations, List<InvestmentTransaction> transactions) {
        double cash = endingCash(allocations);
        Label value = new Label(Money.format(cash));
        value.getStyleClass().add("monthly-card-value-strong");
        VBox summary = new VBox(6, new Label("Ending cash and cash investments"), value);
        summary.getStyleClass().add("statement-section");
        List<InvestmentTransaction> cashTransactions = transactions.stream()
            .filter(item -> item.getAction().equals("Deposit")
                || item.getAction().equals("Withdrawal")
                || item.getAction().equals("Interest"))
            .toList();
        return new VBox(14, summary, tableScroll(readOnlyTransactionTable(cashTransactions)));
    }

    private TableView<InvestmentTransaction> readOnlyTransactionTable(List<InvestmentTransaction> values) {
        TableView<InvestmentTransaction> table = new TableView<>();
        table.getItems().setAll(values);
        table.getColumns().add(column("Date", value -> value.getTransactionDate() == null ? "" : value.getTransactionDate().format(DATE), 100));
        table.getColumns().add(column("Action", InvestmentTransaction::getAction, 100));
        table.getColumns().add(column("Symbol", InvestmentTransaction::getSymbol, 90));
        table.getColumns().add(column("Description", InvestmentTransaction::getDescription, 360));
        table.getColumns().add(column("Quantity", value -> number(value.getQuantity(), 4), 100));
        table.getColumns().add(column("Price", value -> Money.format(value.getPrice()), 110));
        table.getColumns().add(column("Amount", value -> signedMoney(value.getAmount()), 120));
        table.getColumns().add(column("Realized gain/loss", value -> Money.format(value.getRealizedGainLoss()), 145));
        table.setPlaceholder(new Label("No transactions found in this statement."));
        return table;
    }

    private ScrollPane tableScroll(TableView<?> table) {
        table.setPrefHeight(430);
        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private ScrollPane dynamicTableScroll(TableView<?> table) {
        applyDynamicTableHeight(table);
        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void applyDynamicTableHeight(TableView<?> table) {
        int rows = Math.max(1, table.getItems().size());
        table.setPrefHeight(Math.min(430, Math.max(115, 32 + rows * 30)));
    }

    private <T> TableColumn<T, String> column(String title, java.util.function.Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<InvestmentTransaction, String> transactionAmountColumn(Runnable refreshTotal) {
        TableColumn<InvestmentTransaction, String> column = column("Amount", value -> signedMoney(value.getAmount()), 120);
        Label header = new Label("Amount (i)");
        Tooltip tooltip = new Tooltip(
            "Muestra el dinero que entra o sale de la cuenta con cada movimiento.\n\n"
                + "Positivo (+): Entro dinero en la cuenta.\n\n"
                + "Negativo (-): Salio dinero de la cuenta.\n\n"
                + "Las reinversiones de dividendos pueden mostrar primero una entrada y despues una salida por el mismo importe, "
                + "ya que el dividendo recibido se utiliza automaticamente para comprar mas participaciones."
        );
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(360);
        Tooltip.install(header, tooltip);
        column.setGraphic(header);
        column.setText("");
        column.setCellFactory(item -> new TableCell<>() {
            private final javafx.scene.control.TextField editor = new javafx.scene.control.TextField();

            {
                editor.setOnAction(event -> commitEdit(editor.getText()));
                editor.focusedProperty().addListener((observable, oldValue, focused) -> {
                    if (!focused && isEditing()) {
                        commitEdit(editor.getText());
                    }
                });
            }

            @Override
            protected void updateItem(String amount, boolean empty) {
                super.updateItem(amount, empty);
                getStyleClass().removeAll("investment-card-value-positive", "investment-card-value-negative");
                if (empty || amount == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (isEditing()) {
                    editor.setText(amount);
                    setText(null);
                    setGraphic(editor);
                } else {
                    setText(amount);
                    setGraphic(null);
                }
                InvestmentTransaction transaction = getTableRow() == null ? null : getTableRow().getItem();
                double value = transaction == null ? 0 : transaction.getAmount();
                getStyleClass().add(value < 0 ? "investment-card-value-negative" : "investment-card-value-positive");
            }

            @Override
            public void startEdit() {
                InvestmentTransaction transaction = getTableRow() == null ? null : getTableRow().getItem();
                if (transaction != null && transaction.isSubtotal()) {
                    return;
                }
                super.startEdit();
                editor.setText(getItem());
                setText(null);
                setGraphic(editor);
                editor.requestFocus();
                editor.selectAll();
            }

            @Override
            public void commitEdit(String value) {
                InvestmentTransaction transaction = getTableRow() == null ? null : getTableRow().getItem();
                if (transaction != null && transaction.isSubtotal()) {
                    cancelEdit();
                    return;
                }
                if (transaction != null) {
                    transaction.setAmount(decimal(value));
                    refreshTotal.run();
                }
                super.commitEdit(signedMoney(transaction == null ? 0 : transaction.getAmount()));
                getTableView().refresh();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setGraphic(null);
                setText(getItem());
            }
        });
        return column;
    }

    private <T> TableColumn<T, String> editableColumn(
        String title,
        java.util.function.Function<T, String> value,
        BiConsumer<T, String> setter,
        double width,
        Runnable afterEdit
    ) {
        TableColumn<T, String> column = column(title, value, width);
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            if (event.getRowValue() instanceof InvestmentTransaction transaction && transaction.isSubtotal()) {
                event.getTableView().refresh();
                return;
            }
            if (event.getRowValue() instanceof InvestmentPosition position && position.isTotal()) {
                event.getTableView().refresh();
                return;
            }
            setter.accept(event.getRowValue(), event.getNewValue());
            afterEdit.run();
            event.getTableView().refresh();
        });
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

    private void addInvestmentCardLine(
        GridPane grid,
        int row,
        String labelText,
        String valueText,
        String helpText,
        String valueStyle
    ) {
        Label label = new Label(labelText);
        label.getStyleClass().add("monthly-card-line");
        Label help = new Label("(i)");
        help.getStyleClass().add("investment-help-icon");
        Tooltip tooltip = new Tooltip(helpText);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(320);
        Tooltip.install(help, tooltip);
        ContextMenu helpMenu = helpMenu(helpText);
        help.setOnMouseClicked(event -> {
            event.consume();
            helpMenu.show(help, Side.BOTTOM, 0, 0);
        });
        HBox labelBox = new HBox(4, label, help);
        labelBox.setOnMouseClicked(event -> event.consume());

        Label value = new Label(valueText);
        value.getStyleClass().addAll("monthly-card-value", valueStyle);
        grid.add(labelBox, 0, row);
        grid.add(value, 1, row);
    }

    private ContextMenu helpMenu(String helpText) {
        Label content = new Label(helpText);
        content.setWrapText(true);
        content.setMaxWidth(320);
        content.setPadding(new Insets(10));
        CustomMenuItem item = new CustomMenuItem(content, false);
        ContextMenu menu = new ContextMenu(item);
        menu.setAutoHide(true);
        return menu;
    }

    private String number(double value, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", value);
    }

    private String signedMoney(double value) {
        return value > 0 ? "+" + Money.format(value) : Money.format(value);
    }

    private String signedStyle(double value) {
        return value < 0 ? "investment-card-value-negative" : "investment-card-value-positive";
    }

    private double decimal(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Money.parse(value.replace("$", "").trim());
    }

    private void uniformButtons(Button... buttons) {
        for (Button button : buttons) {
            if (!button.getStyleClass().contains("investment-action-button")) {
                button.getStyleClass().add("investment-action-button");
            }
            button.setMinHeight(34);
            button.setPrefHeight(34);
            button.setMaxHeight(34);
            button.setMinWidth(170);
            button.setPrefWidth(170);
        }
    }

    private LocalDate date(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.contains("/") ? LocalDate.parse(trimmed, DATE) : LocalDate.parse(trimmed);
    }

    private String ending(String value) {
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
