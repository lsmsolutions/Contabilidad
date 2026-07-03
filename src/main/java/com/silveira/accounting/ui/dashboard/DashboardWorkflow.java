package com.silveira.accounting.ui.dashboard;

import com.silveira.accounting.application.dashboard.DashboardSnapshot;
import com.silveira.accounting.controllers.dashboard.DashboardController;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.ui.bank.BankDashboardPanelView;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DashboardWorkflow {
    private final DashboardController controller;
    private final Config config;

    public DashboardWorkflow(DashboardController controller, Config config) {
        this.controller = controller;
        this.config = config;
    }

    public void showDashboard() {
        DashboardSnapshot snapshot = controller.snapshot();
        HBox dashboardCharts = new HBox(14, dashboardNylMonthlyChartPanel(snapshot), dashboardMortgageDebtPanel(snapshot));
        dashboardCharts.getStyleClass().add("dashboard-chart-row");
        VBox compactDashboard = new VBox(
            18,
            dashboardPaymentsPanel(snapshot),
            dashboardBankPanel(snapshot),
            dashboardCharts
        );
        compactDashboard.getStyleClass().add("dashboard-dark-panel");
        VBox compactDashboardPage = page("Dashboard", compactDashboard);
        compactDashboardPage.getStyleClass().add("dashboard-page");
        config.setPage().accept(compactDashboardPage);
    }

    private VBox dashboardPaymentsPanel(DashboardSnapshot snapshot) {
        Label title = new Label("Proximos pagos");
        title.getStyleClass().add("dashboard-panel-title");
        GridPane table = new GridPane();
        table.getStyleClass().add("dashboard-payments-table");
        ColumnConstraints cardColumn = dashboardPaymentColumn(560);
        cardColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints numberColumn = dashboardPaymentColumn(130);
        ColumnConstraints dateColumn = dashboardPaymentColumn(150);
        ColumnConstraints amountColumn = dashboardPaymentColumn(140);
        amountColumn.setHalignment(HPos.LEFT);
        ColumnConstraints pdfColumn = dashboardPaymentColumn(170);
        table.getColumnConstraints().setAll(cardColumn, numberColumn, dateColumn, amountColumn, pdfColumn);
        dashboardPaymentCell(table, 0, 0, "Tarjeta", "dashboard-table-header");
        dashboardPaymentCell(table, 1, 0, "Numero", "dashboard-table-header");
        dashboardPaymentCell(table, 2, 0, "Fecha limite", "dashboard-table-header");
        dashboardPaymentCell(table, 3, 0, "Cantidad limite", "dashboard-table-header");
        dashboardPaymentCell(table, 4, 0, "PDF esperado", "dashboard-table-header");
        int row = 1;
        for (DashboardSnapshot.CardPayment payment : snapshot.cardPayments()) {
            CreditCardAccount account = payment.account();
            Optional<CreditCardStatement> latest = payment.latestStatement();
            String digits = latest.map(CreditCardStatement::getAccountLastDigits)
                .filter(value -> value != null && !value.isBlank())
                .orElse(account.getAccountLastDigits());
            String name = dashboardCardDisplayName(account);
            dashboardPaymentCell(table, 0, row, name, "dashboard-table-text");
            dashboardPaymentCell(table, 1, row, digits == null || digits.isBlank() ? "-" : "**** " + digits, "dashboard-table-muted");
            dashboardPaymentCell(
                table,
                2,
                row,
                latest.map(CreditCardStatement::getPaymentDueDate).map(this::shortDate).orElse("Sin fecha"),
                "dashboard-table-date"
            );
            dashboardPaymentCell(
                table,
                3,
                row,
                latest.map(statement -> Money.format(statement.getMinimumPaymentDue())).orElse("-"),
                "dashboard-table-amount"
            );
            dashboardPaymentCell(table, 4, row, dashboardExpectedCardPdf(digits), "dashboard-table-muted");
            row++;
        }
        if (row == 1) {
            dashboardPaymentCell(table, 0, row, "No hay tarjetas registradas.", "dashboard-table-muted");
        }
        VBox panel = new VBox(10, title, table);
        panel.getStyleClass().add("dashboard-panel");
        return panel;
    }

    private String dashboardExpectedCardPdf(String digits) {
        return switch (digits == null ? "" : digits.trim()) {
            case "0782" -> "17-18 de cada mes";
            case "2512", "2518" -> "03-04 de cada mes";
            case "9497" -> "15-18 de cada mes";
            case "5211" -> "23-24 de cada mes";
            case "5632" -> "06-07 de cada mes";
            default -> "-";
        };
    }

    private String dashboardCardDisplayName(CreditCardAccount account) {
        return account == null ? "-" : account.getAlias();
    }

    private void dashboardPaymentCell(GridPane table, int column, int row, String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(label, true);
        if (column == 3 || column == 4) {
            GridPane.setHalignment(label, HPos.LEFT);
        }
        table.add(label, column, row);
    }

    private ColumnConstraints dashboardPaymentColumn(double width) {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(width);
        column.setPrefWidth(width);
        return column;
    }

    private VBox dashboardBankPanel(DashboardSnapshot snapshot) {
        return new BankDashboardPanelView().build(
            snapshot.bankAccounts(),
            alias -> snapshot.bankPeriods().getOrDefault(alias, List.of())
        );
    }

    private VBox dashboardNylMonthlyChartPanel(DashboardSnapshot snapshot) {
        Label title = new Label("Comisiones vs deducciones");
        title.getStyleClass().add("dashboard-panel-title");
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.getStyleClass().add("nyl-monthly-chart");
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setTitle("");
        XYChart.Series<String, Number> commissions = new XYChart.Series<>();
        commissions.setName("Comisiones");
        XYChart.Series<String, Number> deductions = new XYChart.Series<>();
        deductions.setName("Deducciones");
        for (int month = 1; month <= 12; month++) {
            String label = config.monthName().apply(month);
            commissions.getData().add(new XYChart.Data<>(
                label,
                snapshot.monthlyCommissions().getOrDefault(month, 0.0)
            ));
            deductions.getData().add(new XYChart.Data<>(
                label,
                snapshot.monthlyDeductions().getOrDefault(month, 0.0)
            ));
        }
        chart.getData().setAll(commissions, deductions);
        VBox panel = new VBox(8, title, chart);
        panel.getStyleClass().add("dashboard-panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private VBox dashboardMortgageDebtPanel(DashboardSnapshot snapshot) {
        Label title = new Label("Deuda hipoteca Casa");
        title.getStyleClass().add("dashboard-panel-title");
        LineChart<String, Number> chart = config.debtChart().create(snapshot.mortgageStatements());
        chart.setTitle("");
        chart.setAnimated(false);
        VBox panel = new VBox(8, title, chart);
        panel.getStyleClass().add("dashboard-panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private String shortDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private VBox page(String title, Node... nodes) {
        Label heading = new Label(title);
        heading.getStyleClass().add("heading");
        VBox box = new VBox(18);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("page");
        return box;
    }

    public record Config(
        Consumer<Parent> setPage,
        IntFunction<String> monthName,
        DebtChartFactory debtChart
    ) {
    }

    @FunctionalInterface
    public interface DebtChartFactory {
        LineChart<String, Number> create(List<MortgageStatement> statements);
    }
}
