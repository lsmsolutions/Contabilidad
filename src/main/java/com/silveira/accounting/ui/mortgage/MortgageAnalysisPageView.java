package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.models.MortgageStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;

public class MortgageAnalysisPageView {
    private final MortgageApplicationService mortgage;
    private final Config config;

    public MortgageAnalysisPageView(MortgageApplicationService mortgage, Config config) {
        this.mortgage = mortgage;
        this.config = config;
    }

    public Content build(String alias) {
        List<MortgageStatement> statements = mortgage.statements().findByLoan(alias, null, null).stream()
            .filter(statement -> statement.getStatementDate() != null)
            .sorted(Comparator.comparing(MortgageStatement::getStatementDate))
            .toList();
        List<MortgageStatement> source = config.reviewedOrAll().apply(statements);
        HBox totals = new HBox(12);
        totals.getChildren().setAll(config.analysisTotals().nodes(source));
        totals.getStyleClass().add("totals-panel");
        return new Content(totals, debtChart(source), paymentChart(source));
    }

    public LineChart<String, Number> debtChart(List<MortgageStatement> statements) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Mes");
        yAxis.setLabel("Deuda principal pendiente");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Evolucion de deuda pendiente");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Double> debtValues = new ArrayList<>();
        double initialDebt = config.initialDebt().applyAsDouble(statements);
        double debt = initialDebt;
        if (initialDebt > 0) {
            series.getData().add(new XYChart.Data<>("Initial Debt", initialDebt));
            debtValues.add(initialDebt);
        }
        for (MortgageStatement statement : statements) {
            if (statement.getStatementDate() == null) {
                continue;
            }
            debt -= statement.getPastPaidPrincipalSinceLastStatement();
            double value = debt > 0 ? debt : statement.getOutstandingPrincipalBalance();
            series.getData().add(new XYChart.Data<>(config.monthName().apply(statement.getStatementDate().getMonthValue()) + " " + statement.getStatementDate().getYear(), value));
            debtValues.add(value);
        }
        configureDebtAxis(yAxis, debtValues);
        chart.getData().add(series);
        chart.setMinHeight(320);
        return chart;
    }

    private void configureDebtAxis(NumberAxis yAxis, List<Double> values) {
        if (values.isEmpty()) {
            return;
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double spread = Math.max(max - min, Math.max(max * 0.002, 1000));
        double padding = spread * 0.25;
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(Math.max(0, min - padding));
        yAxis.setUpperBound(max + padding);
        yAxis.setTickUnit(Math.max(100, spread / 5));
    }

    private StackedBarChart<String, Number> paymentChart(List<MortgageStatement> statements) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Mes");
        yAxis.setLabel("Importe");
        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Payment breakdown by month");
        XYChart.Series<String, Number> principal = new XYChart.Series<>();
        principal.setName("Principal");
        XYChart.Series<String, Number> interest = new XYChart.Series<>();
        interest.setName("Interest");
        XYChart.Series<String, Number> escrow = new XYChart.Series<>();
        escrow.setName("Escrow");
        for (MortgageStatement statement : statements) {
            if (statement.getStatementDate() == null) {
                continue;
            }
            String label = config.monthName().apply(statement.getStatementDate().getMonthValue()) + " " + statement.getStatementDate().getYear();
            principal.getData().add(new XYChart.Data<>(label, statement.getPastPaidPrincipalSinceLastStatement()));
            interest.getData().add(new XYChart.Data<>(label, statement.getPastPaidInterestSinceLastStatement()));
            escrow.getData().add(new XYChart.Data<>(label, statement.getPastPaidEscrowSinceLastStatement()));
        }
        chart.getData().addAll(principal, interest, escrow);
        chart.setMinHeight(340);
        return chart;
    }

    public record Content(Node totals, Node debtChart, Node paymentChart) {
    }

    public record Config(
        Function<List<MortgageStatement>, List<MortgageStatement>> reviewedOrAll,
        AnalysisTotals analysisTotals,
        ToDoubleFunction<List<MortgageStatement>> initialDebt,
        IntFunction<String> monthName
    ) {
    }

    @FunctionalInterface
    public interface AnalysisTotals {
        List<Node> nodes(List<MortgageStatement> statements);
    }
}
