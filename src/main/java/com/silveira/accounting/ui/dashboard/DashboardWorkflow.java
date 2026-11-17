package com.silveira.accounting.ui.dashboard;

import com.silveira.accounting.application.dashboard.DashboardSnapshot;
import com.silveira.accounting.controllers.dashboard.DashboardController;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.ui.bank.BankDashboardPanelView;
import java.time.LocalDate;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

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
            dashboardLatestStatementsPanel(snapshot),
            dashboardBankPanel(snapshot),
            dashboardCharts
        );
        compactDashboard.getStyleClass().add("dashboard-dark-panel");
        VBox compactDashboardPage = page("Dashboard", compactDashboard);
        compactDashboardPage.getStyleClass().add("dashboard-page");
        config.setPage().accept(compactDashboardPage);
    }

    private VBox dashboardLatestStatementsPanel(DashboardSnapshot snapshot) {
        Label title = new Label("Statements to request");
        title.getStyleClass().add("dashboard-panel-title");
        GridPane table = new GridPane();
        table.getStyleClass().add("dashboard-latest-table");
        table.getColumnConstraints().setAll(
            dashboardLatestColumn(13),
            dashboardLatestColumn(22),
            dashboardLatestColumn(15),
            dashboardLatestColumn(50)
        );
        dashboardLatestHeaderCell(table, 0, 0, "Type", "dashboard-table-header");
        dashboardLatestHeaderCell(table, 1, 0, "Account", "dashboard-table-header");
        dashboardLatestHeaderCell(table, 2, 0, "Last statement", "dashboard-table-header");
        dashboardLatestHeaderCell(table, 3, 0, "Statements to request", "dashboard-table-header");
        int row = 1;
        for (DashboardSnapshot.LatestStatement statement : snapshot.latestStatements()) {
            dashboardLatestCell(table, 0, row, statement.source(), "dashboard-table-muted");
            dashboardLatestCell(table, 1, row, statement.account(), "dashboard-table-text");
            dashboardLatestCell(table, 2, row, statement.date() == null ? "None" : shortDate(statement.date()), "dashboard-table-muted");
            dashboardLatestCell(table, 3, row, statement.missingPeriods(), "dashboard-table-muted");
            row++;
        }
        if (row == 1) {
            dashboardLatestHeaderCell(table, 0, row, "No accounts registered.", "dashboard-table-muted");
        }
        Button download = new Button("Download Excel");
        download.getStyleClass().add("dashboard-export-button");
        download.setOnAction(event -> exportStatements(snapshot, download));
        Button pdf = new Button("Download PDF");
        pdf.getStyleClass().add("dashboard-export-button");
        pdf.setOnAction(event -> exportStatementsPdf(snapshot, pdf));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(10, title, spacer, download, pdf);
        heading.setAlignment(Pos.CENTER_LEFT);
        Label subtitle = new Label("Statements needed since the latest recorded document, including this month.");
        subtitle.getStyleClass().add("dashboard-statements-subtitle");
        VBox panel = new VBox(6, heading, subtitle, table);
        panel.getStyleClass().addAll("dashboard-panel", "dashboard-latest-panel");
        return panel;
    }

    private void exportStatementsPdf(DashboardSnapshot snapshot, Button button) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save statements to request");
        chooser.setInitialFileName("statements-to-request.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF document", "*.pdf"));
        File file = chooser.showSaveDialog(button.getScene().getWindow());
        if (file == null) return;
        try {
            StatementsPdfExporter.export(file.toPath(), snapshot.latestStatements());
        } catch (IOException | IllegalArgumentException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not save the file: " + exception.getMessage());
            alert.initOwner(button.getScene().getWindow());
            alert.setHeaderText("Export failed");
            alert.showAndWait();
        }
    }

    private void exportStatements(DashboardSnapshot snapshot, Button button) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save statements to request");
        chooser.setInitialFileName("statements-to-request.xlsx");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel workbook", "*.xlsx"));
        File file = chooser.showSaveDialog(button.getScene().getWindow());
        if (file == null) return;
        try (var workbook = new XSSFWorkbook(); var output = Files.newOutputStream(file.toPath())) {
            var sheet = workbook.createSheet("Statements to request");
            sheet.setDisplayGridlines(false);
            sheet.createFreezePane(0, 3);
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 25 * 256);
            sheet.setColumnWidth(2, 19 * 256);
            sheet.setColumnWidth(3, 65 * 256);
            var titleStyle = workbook.createCellStyle();
            titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            titleStyle.setFont(titleFont);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            var titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(32);
            var titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Statements to request");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            var subtitle = sheet.createRow(1);
            subtitle.createCell(0).setCellValue("Prepared on " + shortDate(LocalDate.now()) + " | Includes the current month");
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
            var headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            var header = sheet.createRow(2);
            header.setHeightInPoints(24);
            String[] titles = {"Type", "Account", "Last statement", "Statements to request"};
            for (int i = 0; i < titles.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(titles[i]);
                cell.setCellStyle(headerStyle);
            }
            var regular = workbook.createCellStyle();
            regular.setVerticalAlignment(VerticalAlignment.CENTER);
            regular.setWrapText(true);
            var alternate = workbook.createCellStyle();
            alternate.cloneStyleFrom(regular);
            alternate.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            alternate.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            int index = 3;
            for (var statement : snapshot.latestStatements()) {
                var row = sheet.createRow(index++);
                String[] values = {statement.source(), statement.account(), statement.date() == null ? "None" : shortDate(statement.date()), statement.missingPeriods()};
                for (int i = 0; i < values.length; i++) {
                    var cell = row.createCell(i);
                    cell.setCellValue(values[i]);
                    cell.setCellStyle(index % 2 == 0 ? regular : alternate);
                }
                int lines = Math.max(1, (statement.missingPeriods().length() + 64) / 65);
                row.setHeightInPoints(Math.min(409, Math.max(24, lines * 16)));
            }
            sheet.setAutoFilter(new CellRangeAddress(2, Math.max(2, index - 1), 0, 3));
            workbook.write(output);
        } catch (IOException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Could not save the file: " + exception.getMessage());
            alert.initOwner(button.getScene().getWindow());
            alert.setHeaderText("Export failed");
            alert.showAndWait();
        }
    }

    private void dashboardLatestHeaderCell(GridPane table, int column, int row, String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(label, true);
        table.add(label, column, row);
    }

    private void dashboardLatestCell(GridPane table, int column, int row, String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClass, "dashboard-latest-row-cell");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(label, true);
        table.add(label, column, row);
    }

    private ColumnConstraints dashboardLatestColumn(double percent) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percent);
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
