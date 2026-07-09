package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import com.silveira.accounting.parsers.MortgageStatementParser;
import com.silveira.accounting.services.ExcelExportService;
import com.silveira.accounting.services.MortgageAnalysisService;
import com.silveira.accounting.services.MortgageImportService;
import com.silveira.accounting.ui.common.PdfImportModeDialog;
import com.silveira.accounting.utils.Money;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;

public class MortgageDetailWorkflow {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MortgageApplicationService mortgage;
    private final MortgageImportService mortgageImportService;
    private final MortgageAnalysisService mortgageAnalysisService;
    private final ExcelExportService excelExportService;
    private final Config config;

    public MortgageDetailWorkflow(
        MortgageApplicationService mortgage,
        MortgageImportService mortgageImportService,
        MortgageAnalysisService mortgageAnalysisService,
        ExcelExportService excelExportService,
        Config config
    ) {
        this.mortgage = mortgage;
        this.mortgageImportService = mortgageImportService;
        this.mortgageAnalysisService = mortgageAnalysisService;
        this.excelExportService = excelExportService;
        this.config = config;
    }

    public void showMortgageDetail(String alias) {
        TableView<MortgageStatement> statements = mortgageTableFactory().statementTable();
        TableView<MortgageTransaction> movements = mortgageTableFactory().transactionTable();
        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        VBox statementSummaries = new VBox(10);
        statementSummaries.getStyleClass().add("statement-card-list");
        Runnable refreshTotals = () -> totals.getChildren().setAll(mortgageTotalsNodes(statements.getItems(), movements.getItems()));
        Runnable[] refreshRef = new Runnable[1];
        Runnable saveVisible = () -> saveVisibleMortgageRows(statements, movements, refreshRef[0]);
        Runnable refresh = () -> {
            statements.setItems(FXCollections.observableArrayList(mortgage.statements().findByLoan(alias, selectedYear(), selectedMonth())));
            movements.setItems(FXCollections.observableArrayList(mortgage.transactions().findByLoan(alias, selectedYear(), selectedMonth())));
            refreshTotals.run();
            mortgageStatementSummaryWorkflow().refresh(statements, statementSummaries, refreshTotals, saveVisible);
        };
        refreshRef[0] = refresh;
        refresh.run();
        ComboBox<Integer> year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(config.selectedYearValue().get());
        ComboBox<Integer> month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(config.selectedMonthValue().get());
        Button filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");
        filter.setOnAction(event -> {
            config.setSelectedYearValue().accept(year.getValue());
            config.setSelectedMonthValue().accept(month.getValue());
            refresh.run();
        });
        Button importPdf = new Button("Importar PDF");
        importPdf.getStyleClass().add("primary");
        importPdf.setOnAction(event -> importMortgagePdf(alias));
        Button analysis = new Button("Ver analisis");
        analysis.setOnAction(event -> showMortgageAnalysis(alias));
        Button addStatement = new Button("Entrada manual");
        addStatement.setOnAction(event -> addManualMortgageStatement(alias, statements, refresh));
        Button save = new Button("Guardar visibles");
        save.setOnAction(event -> saveVisible.run());
        VBox actions = actionHeader(
            new HBox(10, new Label("Ano"), year, new Label("Mes"), month, filter),
            new HBox(10, importPdf, analysis, addStatement, save)
        );
        Button saveMovements = new Button("Guardar");
        saveMovements.getStyleClass().add("primary");
        saveMovements.setOnAction(event -> saveVisible.run());
        HBox movementActions = new HBox(10, saveMovements);
        movementActions.getStyleClass().add("mortgage-tab-actions");
        movementActions.setAlignment(Pos.CENTER_LEFT);
        VBox statementTabContent = new VBox(12, statementSummaries);
        statementTabContent.getStyleClass().add("mortgage-tab-content");
        VBox movementTabContent = new VBox(12, movementActions, movements);
        movementTabContent.getStyleClass().add("mortgage-tab-content");
        TabPane tabs = new TabPane(
            tab("Statements", statementTabContent),
            tab("Movimientos", movementTabContent)
        );
        VBox.setVgrow(tabs, Priority.ALWAYS);
        Button expenses = new Button("Go to Expenses");
        expenses.setOnAction(event -> config.showHouseExpenses().accept(alias));
        config.setPage().accept(page(
            "Hipoteca - " + alias,
            new HBox(10, config.backButton().create("Volver a Hipotecas", config.showMortgages()), expenses),
            actions,
            totals,
            monthlyMortgageCards(alias, statements, movements, totals, statementSummaries),
            tabs
        ));
    }

    public LineChart<String, Number> debtChart(List<MortgageStatement> statements) {
        return mortgageAnalysisPageView().debtChart(statements);
    }

    private MortgageStatementSummaryWorkflow mortgageStatementSummaryWorkflow() {
        return new MortgageStatementSummaryWorkflow(
            mortgage,
            new MortgageStatementSummaryWorkflow.Config(
                config.confirm(),
                this::showMortgageStatementDialog,
                this::showMortgageDetail
            )
        );
    }

    private void importMortgagePdf(String alias) {
        File file = config.choosePdf().choose();
        if (file == null) {
            return;
        }
        PdfImportModeDialog.Mode mode = new PdfImportModeDialog().show(null).orElse(null);
        if (mode == null) {
            return;
        }
        if (mode == PdfImportModeDialog.Mode.AI) {
            importMortgagePdfWithAi(alias, file);
            return;
        }
        try {
            var parsed = mortgageImportService.importPdf(file.toPath());
            saveImportedMortgage(alias, parsed);
        } catch (RuntimeException exception) {
            handleMortgageImportFailure(alias, file, exception);
        }
    }

    private void handleMortgageImportFailure(String alias, File file, RuntimeException exception) {
        ButtonType ai = new ButtonType("Intentar con IA", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
            Alert.AlertType.ERROR,
            config.rootCauseMessage().apply(exception)
                + "\n\nPuedes intentar leer este PDF con IA. El resultado quedara pendiente de revision.",
            ai,
            close
        );
        alert.setTitle("No se pudo importar hipoteca");
        alert.setHeaderText("No se pudo importar hipoteca");
        alert.showAndWait().filter(selected -> selected == ai).ifPresent(selected -> importMortgagePdfWithAi(alias, file));
    }

    private void importMortgagePdfWithAi(String alias, File file) {
        config.alert().show(
            Alert.AlertType.INFORMATION,
            "Lectura con IA",
            "La IA intentara leer el PDF. Puedes revisar el resultado antes de marcarlo como revisado."
        );
        Task<MortgageStatementParser.ParsedMortgageStatement> task = new Task<>() {
            @Override
            protected MortgageStatementParser.ParsedMortgageStatement call() {
                return mortgageImportService.importPdfWithAi(file.toPath());
            }
        };
        task.setOnSucceeded(event -> {
            saveImportedMortgage(alias, task.getValue());
            config.alert().show(
                Alert.AlertType.INFORMATION,
                "Hipoteca leida con IA",
                "El statement fue extraido con IA y quedo pendiente de revision."
            );
        });
        task.setOnFailed(event -> config.alert().show(
            Alert.AlertType.ERROR,
            "No se pudo leer con IA",
            config.rootCauseMessage().apply(task.getException())
        ));
        Thread worker = new Thread(task, "mortgage-ai-import");
        worker.setDaemon(true);
        worker.start();
    }

    private void saveImportedMortgage(String alias, MortgageStatementParser.ParsedMortgageStatement parsed) {
        MortgageStatement statement = parsed.statement();
        statement.setLoanAlias(alias);
        long statementId = mortgage.statements().save(statement);
        mortgage.transactions().saveAll(statementId, parsed.transactions());
        mortgage.alerts().saveAll(statementId, mortgageAnalysisService.analyze(statement).alerts());
        if (statement.getStatementDate() != null) {
            config.setSelectedYearValue().accept(statement.getStatementDate().getYear());
            config.setSelectedMonthValue().accept(statement.getStatementDate().getMonthValue());
        }
        config.rebuildSidebar().run();
        showMortgageDetail(alias);
    }

    private void addManualMortgageStatement(String alias, TableView<MortgageStatement> table, Runnable refresh) {
        MortgageStatement statement = new MortgageStatement();
        statement.setLoanAlias(alias);
        statement.setStatementDate(LocalDate.now());
        statement.setPaymentDueDate(LocalDate.now().plusDays(21));
        statement.setPendingReview(true);
        statement.setReviewRequired(true);
        statement.setReviewNotes("Anadido manualmente");
        table.getItems().add(statement);
        table.getSelectionModel().select(statement);
        refresh.run();
    }

    private void updateMortgageTransactionIfSaved(MortgageTransaction movement) {
        if (movement.getId() > 0) {
            mortgage.transactions().update(movement);
        }
    }

    private void showMortgageStatementDialog(MortgageStatement statement, Runnable refresh) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar statement de hipoteca");
        TextField statementDate = new TextField(dateText(statement.getStatementDate()));
        TextField dueDate = new TextField(dateText(statement.getPaymentDueDate()));
        TextField totalDue = moneyField(mortgageDueAmount(statement));
        TextField principal = moneyField(statement.getPrincipalDue());
        TextField interest = moneyField(statement.getInterestDue());
        TextField escrow = moneyField(statement.getEscrowDue());
        TextField original = moneyField(statement.getOriginalPrincipalBalance());
        TextField outstanding = moneyField(statement.getOutstandingPrincipalBalance());
        TextField escrowBalance = moneyField(statement.getEscrowBalance());
        TextField paidPrincipal = moneyField(statement.getPastPaidPrincipalSinceLastStatement());
        TextField paidInterest = moneyField(statement.getPastPaidInterestSinceLastStatement());
        TextField paidEscrow = moneyField(statement.getPastPaidEscrowSinceLastStatement());
        TextField paidTotal = moneyField(statement.getPastPaidTotalSinceLastStatement());
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Statement Date"), statementDate);
        form.addRow(1, new Label("Payment Due Date"), dueDate);
        form.addRow(2, new Label("Total Due"), totalDue);
        form.addRow(3, new Label("Principal"), principal);
        form.addRow(4, new Label("Interest"), interest);
        form.addRow(5, new Label("Escrow"), escrow);
        form.addRow(6, new Label("Original Principal Balance"), original);
        form.addRow(7, new Label("Outstanding Principal Balance"), outstanding);
        form.addRow(8, new Label("Escrow Balance"), escrowBalance);
        form.addRow(9, new Label("Past Principal"), paidPrincipal);
        form.addRow(10, new Label("Past Interest"), paidInterest);
        form.addRow(11, new Label("Past Escrow"), paidEscrow);
        form.addRow(12, new Label("Past Total"), paidTotal);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            statement.setStatementDate(parseDateOrNull(statementDate.getText()));
            statement.setPaymentDueDate(parseDateOrNull(dueDate.getText()));
            double due = Money.parse(totalDue.getText());
            statement.setTotalDue(due);
            statement.setPaymentAmountDue(due);
            statement.setPrincipalDue(Money.parse(principal.getText()));
            statement.setInterestDue(Money.parse(interest.getText()));
            statement.setEscrowDue(Money.parse(escrow.getText()));
            statement.setOriginalPrincipalBalance(Money.parse(original.getText()));
            statement.setOutstandingPrincipalBalance(Money.parse(outstanding.getText()));
            statement.setEscrowBalance(Money.parse(escrowBalance.getText()));
            statement.setPastPaidPrincipalSinceLastStatement(Money.parse(paidPrincipal.getText()));
            statement.setPastPaidInterestSinceLastStatement(Money.parse(paidInterest.getText()));
            statement.setPastPaidEscrowSinceLastStatement(Money.parse(paidEscrow.getText()));
            statement.setPastPaidTotalSinceLastStatement(Money.parse(paidTotal.getText()));
            if (statement.getId() > 0) {
                mortgage.statements().updateRecord(statement);
            }
            refresh.run();
        });
    }

    private void showMortgageTransactionDialog(MortgageTransaction movement, Runnable refresh) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar movimiento de hipoteca");
        TextField date = new TextField(dateText(movement.getTransactionDate()));
        TextField description = new TextField(text(movement.getDescription()));
        TextField total = moneyField(movement.getTotal());
        TextField principal = moneyField(movement.getPrincipal());
        TextField interest = moneyField(movement.getInterest());
        TextField escrow = moneyField(movement.getEscrow());
        TextField fees = moneyField(movement.getFees());
        TextField unapplied = moneyField(movement.getUnapplied());
        TextField other = moneyField(movement.getOther());
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Fecha"), date);
        form.addRow(1, new Label("Descripcion"), description);
        form.addRow(2, new Label("Total"), total);
        form.addRow(3, new Label("Principal"), principal);
        form.addRow(4, new Label("Interest"), interest);
        form.addRow(5, new Label("Escrow"), escrow);
        form.addRow(6, new Label("Fees"), fees);
        form.addRow(7, new Label("Unapplied"), unapplied);
        form.addRow(8, new Label("Other"), other);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            movement.setTransactionDate(parseDateOrNull(date.getText()));
            movement.setDescription(description.getText());
            movement.setTotal(Money.parse(total.getText()));
            movement.setPrincipal(Money.parse(principal.getText()));
            movement.setInterest(Money.parse(interest.getText()));
            movement.setEscrow(Money.parse(escrow.getText()));
            movement.setFees(Money.parse(fees.getText()));
            movement.setUnapplied(Money.parse(unapplied.getText()));
            movement.setOther(Money.parse(other.getText()));
            updateMortgageTransactionIfSaved(movement);
            refresh.run();
        });
    }

    private TextField moneyField(double amount) {
        TextField field = new TextField(Money.format(amount));
        field.setPrefWidth(150);
        return field;
    }

    private String dateText(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private double mortgageDueAmount(MortgageStatement statement) {
        return statement.getTotalDue() > 0 ? statement.getTotalDue() : statement.getPaymentAmountDue();
    }

    private void saveVisibleMortgageRows(
        TableView<MortgageStatement> statements,
        TableView<MortgageTransaction> movements,
        Runnable refresh
    ) {
        for (MortgageStatement statement : statements.getItems()) {
            if (statement.getId() > 0) {
                mortgage.statements().updateRecord(statement);
            } else {
                statement.setId(mortgage.statements().save(statement));
            }
        }
        for (MortgageTransaction movement : movements.getItems()) {
            long statementId = statements.getItems().isEmpty() ? 0 : statements.getItems().get(0).getId();
            if (movement.getId() <= 0 && statementId > 0) {
                movement.setId(mortgage.transactions().save(statementId, movement));
            }
        }
        refresh.run();
        config.alert().show(Alert.AlertType.INFORMATION, "Hipoteca guardada", "Cambios visibles guardados.");
    }

    private void showMortgageAnalysis(String alias) {
        MortgageAnalysisPageView.Content content = mortgageAnalysisPageView().build(alias);
        config.setPage().accept(page(
            "Analisis de hipoteca - " + alias,
            config.backButton().create("Volver al detalle", () -> showMortgageDetail(alias)),
            content.totals(),
            content.debtChart(),
            content.paymentChart()
        ));
    }

    private MortgageAnalysisPageView mortgageAnalysisPageView() {
        return new MortgageAnalysisPageView(
            mortgage,
            new MortgageAnalysisPageView.Config(
                this::mortgageReviewedOrAll,
                this::mortgageAnalysisTotalsNodes,
                this::mortgageInitialDebt,
                config.monthName()
            )
        );
    }

    private List<Node> mortgageTotalsNodes(List<MortgageStatement> statements, List<MortgageTransaction> movements) {
        List<MortgageStatement> source = mortgageReviewedOrAll(statements);
        MortgageStatement latest = source.stream()
            .max(Comparator
                .comparing(MortgageStatement::getStatementDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(MortgageStatement::getId))
            .orElse(null);
        double debtPaid = source.stream().mapToDouble(MortgageStatement::getPastPaidPrincipalSinceLastStatement).sum();
        double outstandingDebt = latest == null ? 0 : latest.getOutstandingPrincipalBalance();
        return List.of(
            miniTotal("Initial Debt", Money.format(mortgageInitialDebt(source)), "neutral-total"),
            miniTotal("Debt Paid", Money.format(debtPaid), "income-total"),
            miniTotal("Outstanding Debt", Money.format(outstandingDebt), "expense-total")
        );
    }

    private List<Node> mortgageAnalysisTotalsNodes(List<MortgageStatement> statements) {
        List<Node> totals = new ArrayList<>(mortgageTotalsNodes(statements, List.of()));
        totals.add(miniTotal("Interest", Money.format(mortgagePaidInterest(statements)), "expense-total"));
        totals.add(miniTotal("Escrow", Money.format(mortgagePaidEscrow(statements)), "pending-total"));
        return totals;
    }

    private List<MortgageStatement> mortgageReviewedOrAll(List<MortgageStatement> statements) {
        List<MortgageStatement> reviewed = statements.stream().filter(statement -> !statement.isPendingReview()).toList();
        return reviewed.isEmpty() ? statements : reviewed;
    }

    private double mortgageInitialDebt(List<MortgageStatement> statements) {
        return statements.stream()
            .max(Comparator
                .comparing(MortgageStatement::getStatementDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(MortgageStatement::getId))
            .map(MortgageStatement::getOriginalPrincipalBalance)
            .orElse(0.0);
    }

    private VBox monthlyMortgageCards(
        String alias,
        TableView<MortgageStatement> table,
        TableView<MortgageTransaction> movementTable,
        HBox totalsPanel,
        VBox statementSummaries
    ) {
        Label title = new Label("Resumen mensual Hipoteca");
        title.getStyleClass().add("section-title");
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");

        VBox general = monthlyActionCard("General", "Ver todos", "", "", "", () -> {
            table.setItems(FXCollections.observableArrayList(mortgage.statements().findByLoan(alias, selectedYear(), null)));
            movementTable.setItems(FXCollections.observableArrayList(mortgage.transactions().findByLoan(alias, selectedYear(), null)));
            totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems()));
            mortgageStatementSummaryWorkflow().refresh(
                table,
                statementSummaries,
                () -> totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems())),
                () -> saveVisibleMortgageRows(table, movementTable, () -> {})
            );
        });
        general.getStyleClass().add("monthly-card-general");
        cards.getChildren().add(general);

        for (MonthlySourceTotals total : mortgage.statements().monthlyTotals(alias, null)) {
            List<MortgageStatement> monthStatements = mortgage.statements().findByLoan(alias, total.year(), total.month());
            VBox card = monthlyActionCard(
                config.monthName().apply(total.month()) + " " + total.year(),
                "Principal: " + Money.format(mortgagePaidPrincipal(monthStatements)),
                "Interest: " + Money.format(mortgagePaidInterest(monthStatements)),
                "Escrow: " + Money.format(mortgagePaidEscrow(monthStatements)),
                "Deuda pendiente: " + Money.format(mortgageOutstandingPrincipal(monthStatements)),
                () -> {
                    table.setItems(FXCollections.observableArrayList(mortgage.statements().findByLoan(alias, total.year(), total.month())));
                    movementTable.setItems(FXCollections.observableArrayList(mortgage.transactions().findByLoan(alias, total.year(), total.month())));
                    totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems()));
                    mortgageStatementSummaryWorkflow().refresh(
                        table,
                        statementSummaries,
                        () -> totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems())),
                        () -> saveVisibleMortgageRows(table, movementTable, () -> {})
                    );
                }
            );
            config.addReviewMark().add(card, "mortgage", alias, total.year(), total.month());
            card.getChildren().add(monthlyExportButton(() -> exportMonthlyMortgage(alias, total.year(), total.month())));
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }

        VBox box = new VBox(10, title, cards);
        box.getStyleClass().add("monthly-section");
        return box;
    }

    private double mortgagePaidPrincipal(List<MortgageStatement> statements) {
        return statements.stream()
            .mapToDouble(MortgageStatement::getPastPaidPrincipalSinceLastStatement)
            .sum();
    }

    private double mortgagePaidInterest(List<MortgageStatement> statements) {
        return statements.stream()
            .mapToDouble(MortgageStatement::getPastPaidInterestSinceLastStatement)
            .sum();
    }

    private double mortgagePaidEscrow(List<MortgageStatement> statements) {
        return statements.stream()
            .mapToDouble(MortgageStatement::getPastPaidEscrowSinceLastStatement)
            .sum();
    }

    private double mortgageOutstandingPrincipal(List<MortgageStatement> statements) {
        return statements.stream()
            .max(Comparator
                .comparing(MortgageStatement::getStatementDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(MortgageStatement::getId))
            .map(MortgageStatement::getOutstandingPrincipalBalance)
            .orElse(0.0);
    }

    private void exportMonthlyMortgage(String alias, int year, int month) {
        File target = config.chooseExcel().choose(
            "hipoteca_" + alias + "_" + year + "_" + String.format("%02d", month) + ".xlsx"
        );
        if (target == null) {
            return;
        }
        excelExportService.exportMortgageMonthly(
            target.toPath(),
            mortgage.statements().findByLoan(alias, year, month),
            mortgage.transactions().findByLoan(alias, year, month)
        );
        config.alert().show(Alert.AlertType.INFORMATION, "Exportacion lista", "Se descargo el resumen mensual de hipoteca.");
    }

    private MortgageTableFactory mortgageTableFactory() {
        return new MortgageTableFactory(
            mortgage,
            new MortgageTableFactory.Config(
                (statement, reviewed) -> mortgageStatementSummaryWorkflow().updateStatementReview(statement, reviewed),
                (transaction, reviewed) -> mortgageStatementSummaryWorkflow().updateTransactionReview(transaction, reviewed),
                this::updateMortgageTransactionIfSaved,
                this::showMortgageTransactionDialog,
                () -> config.alert().show(Alert.AlertType.ERROR, "Valor no valido", "Revisa el valor introducido antes de guardar.")
            )
        );
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

    private VBox actionHeader(Node... rows) {
        VBox header = new VBox(10);
        header.getStyleClass().add("action-header");
        for (Node row : rows) {
            if (row instanceof HBox hBox) {
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getStyleClass().add("action-row");
            }
            header.getChildren().add(row);
        }
        return header;
    }

    private Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private VBox monthlyActionCard(
        String title,
        String line1,
        String line2,
        String line3,
        String line4,
        Runnable action
    ) {
        Label heading = new Label(title);
        heading.getStyleClass().add("monthly-card-title");
        GridPane lines = new GridPane();
        lines.getStyleClass().add("monthly-card-grid");
        int row = 0;
        for (String line : List.of(line1, line2, line3, line4)) {
            if (!line.isBlank()) {
                row = addMonthlyCardGridLine(lines, row, line);
            }
        }
        VBox box = new VBox(0, heading, lines);
        box.setOnMouseClicked(event -> action.run());
        return box;
    }

    private int addMonthlyCardGridLine(GridPane grid, int row, String text) {
        int separator = text.indexOf(": ");
        if (separator <= 0) {
            Label label = new Label(text);
            label.getStyleClass().add("monthly-card-line");
            grid.add(label, 0, row, 2, 1);
            return row + 1;
        }
        Label label = new Label(text.substring(0, separator));
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(text.substring(separator + 2));
        value.getStyleClass().add("monthly-card-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }

    private Button monthlyExportButton(Runnable action) {
        Button button = new Button("Descargar mes");
        button.setOnAction(event -> {
            event.consume();
            action.run();
        });
        return button;
    }

    private VBox miniTotal(String title, String value, String styleClass) {
        Label label = new Label(title);
        label.getStyleClass().add("mini-total-title");
        Label amount = new Label(value);
        amount.getStyleClass().add("mini-total-value");
        VBox box = new VBox(4, label, amount);
        box.getStyleClass().addAll("mini-total", styleClass);
        return box;
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        DateTimeFormatter[] formats = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            SHORT_DATE_FORMAT,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yy")
        };
        RuntimeException last = null;
        for (DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (RuntimeException exception) {
                last = exception;
            }
        }
        throw last == null ? new IllegalArgumentException("Fecha no valida: " + value) : last;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private Integer selectedYear() {
        return config.selectedYearValue().get();
    }

    private Integer selectedMonth() {
        return config.selectedMonthValue().get();
    }

    public record Config(
        Supplier<Integer> selectedYearValue,
        Supplier<Integer> selectedMonthValue,
        Consumer<Integer> setSelectedYearValue,
        Consumer<Integer> setSelectedMonthValue,
        Runnable rebuildSidebar,
        Runnable showMortgages,
        Consumer<String> showHouseExpenses,
        Consumer<Parent> setPage,
        BackButtonFactory backButton,
        ChooseFileAction choosePdf,
        ChooseExcelAction chooseExcel,
        AlertAction alert,
        Function<Throwable, String> rootCauseMessage,
        MortgageStatementSummaryWorkflow.ConfirmAction confirm,
        AddReviewMarkAction addReviewMark,
        IntFunction<String> monthName
    ) {
    }

    @FunctionalInterface
    public interface BackButtonFactory {
        Button create(String text, Runnable action);
    }

    @FunctionalInterface
    public interface ChooseFileAction {
        File choose();
    }

    @FunctionalInterface
    public interface ChooseExcelAction {
        File choose(String initialFileName);
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }

    @FunctionalInterface
    public interface AddReviewMarkAction {
        void add(VBox card, String source, String accountAlias, int year, int month);
    }
}
