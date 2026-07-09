package com.silveira.accounting.ui.nyl;

import com.silveira.accounting.application.nyl.NylGateway;
import com.silveira.accounting.application.nyl.NylImportGateway;
import com.silveira.accounting.controllers.nyl.NylController;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.services.ExcelExportService;
import com.silveira.accounting.ui.common.PdfImportModeDialog;
import com.silveira.accounting.utils.Fingerprint;
import com.silveira.accounting.utils.Money;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class NylWorkflow {
    private static final List<String> DEDUCTION_SECTION_ORDER = List.of(
        "Tax Withholding",
        "Group Plan Contributions",
        "Office Expenses",
        "Technology Expense",
        "Deferred Compensation",
        "Other Deductions"
    );

    private final NylController controller;
    private final ExcelExportService excelExportService;
    private final Config config;
    private final NylTableView tableView;

    public NylWorkflow(NylController controller, ExcelExportService excelExportService, Config config) {
        this.controller = controller;
        this.excelExportService = excelExportService;
        this.config = config;
        tableView = new NylTableView(controller, config.alert()::show);
    }

    public void showNylHub() {
        HBox cards = new HBox(
            14,
            hubCard("Resumen NYL", this::showNyl),
            hubCard("Análisis NYL", this::showAnalysis),
            hubCard("Agent Ledger", config.showAgentLedger())
        );
        cards.getStyleClass().add("monthly-card-row");
        VBox page = page("New York Life", cards);
        page.getStyleClass().add("dark-hub-page");
        config.setPage().accept(page);
    }

    public void showNyl() {
        TableView<NylRecord> table = tableView.build();
        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        Runnable refreshTotals = () -> totals.getChildren().setAll(nylTotalsNodes(controller.totals(selectedYear(), null)));
        refreshTotals.run();
        VBox monthlyCards = new VBox(10);
        Runnable refresh = () -> {
            table.setItems(FXCollections.observableArrayList(controller.find(selectedYear(), selectedMonth(), null, null)));
            refreshTotals.run();
            monthlyCards.getChildren().setAll(monthlyNylCards(table, totals));
        };
        refresh.run();

        ComboBox<Integer> year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(config.selectedYearValue().get());
        year.setPromptText("Año");
        year.getStyleClass().add("compact-combo");
        ComboBox<Integer> month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(config.selectedMonthValue().get());
        month.setPromptText("Mes");
        month.getStyleClass().add("compact-combo");
        TextField concept = new TextField();
        concept.setPromptText("Concepto");
        concept.getStyleClass().add("concept-field");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("", "comision", "credito", "deduccion", "withdrawal", "ajuste", "otro"));
        type.setPromptText("Tipo");
        type.getStyleClass().add("type-combo");
        Button filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");
        filter.setOnAction(event -> {
            config.setSelectedYearValue().accept(year.getValue());
            config.setSelectedMonthValue().accept(month.getValue());
            table.setItems(FXCollections.observableArrayList(controller.find(selectedYear(), selectedMonth(), concept.getText(), type.getValue())));
            refreshTotals.run();
            monthlyCards.getChildren().setAll(monthlyNylCards(table, totals));
        });
        Button importPdf = new Button("Importar PDF");
        importPdf.getStyleClass().add("primary");
        importPdf.setOnAction(event -> importNyl(refresh));
        Button manual = new Button("Entrada manual");
        manual.setOnAction(event -> manualNyl(refresh));
        Button pending = new Button("Pendientes por revisar");
        pending.setOnAction(event -> showPendingNylReview(refresh));
        Button saveProgress = new Button("Guardar progreso");
        saveProgress.setOnAction(event -> saveVisibleRows(table, refresh, false));
        Button saveReviewed = new Button("Guardar revisados");
        saveReviewed.getStyleClass().add("primary");
        saveReviewed.setOnAction(event -> saveVisibleRows(table, refresh, true));
        Button addRecord = new Button("Añadir registro");
        addRecord.setOnAction(event -> addMissingRow(table));
        VBox actions = actionHeader(
            new HBox(10, new Label("Año"), year, new Label("Mes"), month, concept, type, filter),
            new HBox(10, importPdf, manual, pending, saveProgress, saveReviewed)
        );
        HBox tableActions = new HBox(10, addRecord);
        tableActions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(420);
        table.setPrefHeight(520);
        config.setPage().accept(page(
            "New York Life",
            config.backButton().create("Volver a New York Life", this::showNylHub),
            actions,
            totals,
            monthlyCards,
            tableActions,
            table
        ));
    }

    public void showAnalysis() {
        BarChart<String, Number> monthly = new BarChart<>(new CategoryAxis(), new NumberAxis());
        monthly.setTitle("Comisiones vs Deducciones por mes");
        monthly.setLegendVisible(true);
        monthly.getStyleClass().add("nyl-monthly-chart");
        List<NylRecord> records = controller.find(selectedYear(), null, null, null).stream()
            .filter(record -> !record.isPendingReview())
            .toList();
        XYChart.Series<String, Number> credits = new XYChart.Series<>();
        credits.setName("Comisiones/Créditos");
        XYChart.Series<String, Number> deductions = new XYChart.Series<>();
        deductions.setName("Deducciones");
        Map<Integer, Double> monthlyCredits = records.stream()
            .filter(record -> record.getRecordType().equals("comision") || record.getRecordType().equals("credito"))
            .collect(Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(NylRecord::getAmount)));
        Map<Integer, Double> monthlyDeductions = records.stream()
            .filter(record -> record.getRecordType().equals("deduccion"))
            .collect(Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(record -> Math.abs(record.getAmount()))));
        for (int month = 1; month <= 12; month++) {
            String label = monthName(month);
            credits.getData().add(new XYChart.Data<>(label, monthlyCredits.getOrDefault(month, 0.0)));
            deductions.getData().add(new XYChart.Data<>(label, monthlyDeductions.getOrDefault(month, 0.0)));
        }
        monthly.getData().setAll(credits, deductions);
        VBox.setVgrow(monthly, Priority.ALWAYS);
        config.setPage().accept(page(
            "Análisis NYL",
            config.backButton().create("Volver a New York Life", this::showNylHub),
            monthly
        ));
    }

    private void importNyl(Runnable refresh) {
        File file = config.choosePdf().choose();
        if (file == null) {
            return;
        }
        PdfImportModeDialog.Mode mode = new PdfImportModeDialog().show(null).orElse(null);
        if (mode == null) {
            return;
        }
        if (mode == PdfImportModeDialog.Mode.AI) {
            processAi(file.toPath(), refresh);
            return;
        }
        try {
            NylImportGateway.ParsedDocument parsed = controller.parse(file.toPath());
            showReview(parsed.records(), parsed.text(), refresh);
        } catch (RuntimeException exception) {
            handleImportFailure(file.toPath(), refresh, exception);
        }
    }

    private void processOcr(Path pdf, Runnable refresh) {
        showProcessing("Procesando OCR", "Leyendo el PDF escaneado y corrigiendo orientacion. Esto puede tardar unos minutos.");
        Task<NylImportGateway.ParsedDocument> task = new Task<>() {
            @Override
            protected NylImportGateway.ParsedDocument call() {
                return controller.parseWithOcr(pdf);
            }
        };
        task.setOnSucceeded(event -> showReview(task.getValue().records(), task.getValue().text(), refresh));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            boolean openManual = config.confirm().confirm(
                "OCR no disponible",
                error.getMessage() + "\n\nPuedes cargarlo manualmente para no detener el cierre contable.",
                "Abrir entrada manual"
            );
            if (openManual) {
                manualNyl(refresh);
            } else {
                showNyl();
            }
        });
        Thread thread = new Thread(task, "silveira-nyl-ocr");
        thread.setDaemon(true);
        thread.start();
    }

    private void processAi(Path pdf, Runnable refresh) {
        showProcessing("Procesando IA", "La IA intentara leer el PDF de NYL. Todo quedara marcado para revision.");
        Task<NylImportGateway.ParsedDocument> task = new Task<>() {
            @Override
            protected NylImportGateway.ParsedDocument call() {
                return controller.parseWithAi(pdf);
            }
        };
        task.setOnSucceeded(event -> showReview(task.getValue().records(), task.getValue().text(), refresh));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            boolean openManual = config.confirm().confirm(
                "IA no disponible",
                error.getMessage() + "\n\nPuedes cargarlo manualmente para no detener el cierre contable.",
                "Abrir entrada manual"
            );
            if (openManual) {
                manualNyl(refresh);
            } else {
                showNyl();
            }
        });
        Thread thread = new Thread(task, "silveira-nyl-ai");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleImportFailure(Path pdf, Runnable refresh, RuntimeException exception) {
        ButtonType ocr = new ButtonType("Intentar OCR", ButtonBar.ButtonData.OTHER);
        ButtonType ai = new ButtonType("Intentar con IA", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
            Alert.AlertType.ERROR,
            exception.getMessage()
                + "\n\nPuedes intentar OCR si es escaneado, o IA si el formato no se reconoce. En ambos casos todo quedara pendiente de revision.",
            ocr,
            ai,
            close
        );
        alert.setTitle("No se pudo leer el PDF");
        alert.setHeaderText("No se pudo leer el PDF");
        alert.showAndWait().ifPresent(selected -> {
            if (selected == ocr) {
                processOcr(pdf, refresh);
            } else if (selected == ai) {
                processAi(pdf, refresh);
            }
        });
    }

    private void showReview(List<NylRecord> parsed, String extractedText, Runnable refresh) {
        Set<String> existing = controller.existingFingerprints(parsed);
        List<NylRecord> newRecords = parsed.stream()
            .filter(record -> !existing.contains(record.getFingerprint()))
            .sorted(Comparator.comparing(NylRecord::getSection)
                .thenComparingInt(NylRecord::getYear)
                .thenComparingInt(NylRecord::getMonth)
                .thenComparing(NylRecord::getConcept))
            .toList();
        TableView<NylRecord> review = tableView.build();
        review.setItems(FXCollections.observableArrayList(newRecords));
        double declaredTotal = existing.isEmpty() ? controller.detectDeclaredTotal(extractedText) : Double.NaN;
        List<String> warnings = controller.validate(newRecords, declaredTotal);
        if (!existing.isEmpty()) {
            warnings.add(existing.size() + " registros ya existian y se ocultaron de esta revision.");
        }
        Button addMissing = new Button("Añadir registro faltante");
        addMissing.setOnAction(event -> addMissingRow(review));
        Button showExisting = new Button("Ver ya guardados");
        showExisting.setDisable(existing.isEmpty());
        showExisting.setOnAction(event -> showExistingRecords(existing, newRecords, extractedText, refresh));
        Button savePending = new Button("Guardar progreso");
        savePending.getStyleClass().add("primary");
        savePending.setOnAction(event -> saveRows(List.copyOf(review.getItems()), review, List.of(), refresh, true));
        Button saveReviewed = new Button("Guardar revisados");
        saveReviewed.getStyleClass().add("primary");
        saveReviewed.setOnAction(event -> saveProgressRows(review, warnings, refresh));
        config.reviewPresenter().show(
            "Revisión New York Life",
            review,
            () -> saveAllReviewedRows(review, warnings, refresh),
            new VBox(
                10,
                warningBox(warnings),
                reviewSummaryBox(newRecords, existing.size()),
                monthNavigator(newRecords, review),
                monthLegend(newRecords),
                new HBox(10, addMissing, showExisting, savePending, saveReviewed)
            )
        );
    }

    private void showExistingRecords(Set<String> fingerprints, List<NylRecord> newRecords, String extractedText, Runnable refresh) {
        TableView<NylRecord> table = tableView.build();
        table.setItems(FXCollections.observableArrayList(controller.findByFingerprints(fingerprints)));
        config.setPage().accept(page(
            "NYL ya guardados",
            config.backButton().create("Volver a revisión NYL", () -> showReview(newRecords, extractedText, refresh)),
            new Label("Estos registros ya existian y por eso se ocultaron de la nueva importacion."),
            table
        ));
    }

    private void showPendingNylReview(Runnable refresh) {
        List<NylRecord> pending = controller.findPendingReview();
        TableView<NylRecord> table = tableView.build();
        table.setItems(FXCollections.observableArrayList(pending));
        Button saveReviewed = new Button("Guardar cambios de revisión");
        saveReviewed.getStyleClass().add("primary");
        saveReviewed.setOnAction(event -> {
            for (NylRecord record : table.getItems()) {
                if (record.getId() > 0) {
                    controller.updateRecord(record);
                }
            }
            refresh.run();
            showNyl();
        });
        config.setPage().accept(page(
            "NYL pendientes por revisar",
            config.backButton().create("Volver a New York Life", this::showNyl),
            reviewSummaryBox(pending, 0),
            table,
            saveReviewed
        ));
    }

    private void saveVisibleRows(TableView<NylRecord> table, Runnable refresh, boolean reviewedOnly) {
        List<NylRecord> rows = table.getItems().stream()
            .filter(record -> !reviewedOnly || !record.isPendingReview())
            .toList();
        if (rows.isEmpty()) {
            config.alert().show(Alert.AlertType.INFORMATION, "Nada para guardar", reviewedOnly
                ? "No hay filas marcadas como revisadas en esta vista."
                : "No hay filas visibles para guardar.");
            return;
        }
        for (NylRecord record : rows) {
            if (record.getId() > 0) {
                controller.updateRecord(record);
            }
        }
        List<NylRecord> newRows = rows.stream().filter(record -> record.getId() == 0).toList();
        if (!newRows.isEmpty()) {
            controller.saveAll(newRows);
        }
        refresh.run();
        config.alert().show(Alert.AlertType.INFORMATION, "NYL guardado", rows.size() + " registros actualizados.");
    }

    private void saveProgressRows(TableView<NylRecord> table, List<String> warnings, Runnable refresh) {
        if (table.getItems().isEmpty()) {
            config.alert().show(Alert.AlertType.INFORMATION, "Nada para guardar", "No hay filas en la revision.");
            return;
        }
        saveRows(List.copyOf(table.getItems()), table, warnings, refresh, true);
    }

    private void saveAllReviewedRows(TableView<NylRecord> table, List<String> warnings, Runnable refresh) {
        boolean pending = table.getItems().stream().anyMatch(NylRecord::isReviewRequired);
        if (pending && !config.confirm().confirm(
            "Quedan filas sin revisar",
            "Todavia hay filas marcadas como Revisar. Puedes guardar solo las revisadas o confirmar todo cuando termines.",
            "Guardar todo igualmente"
        )) {
            return;
        }
        saveRows(List.copyOf(table.getItems()), table, warnings, refresh, false);
    }

    private void saveRows(List<NylRecord> rows, TableView<NylRecord> table, List<String> warnings, Runnable refresh, boolean keepPending) {
        if (!keepPending && !warnings.isEmpty() && !config.confirm().confirm(
            "Guardar con alertas",
            "Hay alertas de revision en esta importacion.\n\n" + String.join("\n", warnings) + "\n\nConfirma solo si ya revisaste los datos contra el PDF original.",
            "Guardar revisado"
        )) {
            return;
        }
        for (NylRecord row : rows) {
            if (keepPending) {
                boolean remainsPending = row.isPendingReview();
                row.setPendingReview(remainsPending);
                row.setReviewRequired(remainsPending);
            }
        }
        NylGateway.SaveResult result = controller.saveAll(rows);
        if (!result.newConcepts().isEmpty()) {
            config.alert().show(Alert.AlertType.WARNING, "Conceptos nuevos", String.join(", ", result.newConcepts()));
        }
        table.getItems().removeAll(rows);
        rememberPeriod(rows);
        refresh.run();
        if (keepPending) {
            long pending = rows.stream().filter(NylRecord::isPendingReview).count();
            long reviewed = rows.size() - pending;
            config.alert().show(Alert.AlertType.INFORMATION, "Progreso guardado", reviewed + " revisados y " + pending + " pendientes quedaron guardados.");
            showNyl();
        } else {
            showImportTotals(rows, result.inserted());
        }
    }

    private void rememberPeriod(List<NylRecord> rows) {
        rows.stream()
            .max(Comparator.comparingInt(NylRecord::getYear).thenComparingInt(NylRecord::getMonth))
            .ifPresent(record -> {
                config.setSelectedYearValue().accept(record.getYear());
                config.setSelectedMonthValue().accept(record.getMonth());
            });
    }

    private void manualNyl(Runnable refresh) {
        DatePicker date = new DatePicker(LocalDate.now());
        TextField concept = new TextField();
        concept.setPromptText("Concepto");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("comision", "credito", "deduccion", "withdrawal", "ajuste", "otro"));
        type.setValue("comision");
        TextField amount = new TextField();
        amount.setPromptText("Importe");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Fecha"), date);
        form.addRow(1, new Label("Concepto"), concept);
        form.addRow(2, new Label("Tipo"), type);
        form.addRow(3, new Label("Importe"), amount);
        Button save = new Button("Guardar entrada manual");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> {
            LocalDate value = date.getValue();
            double parsedAmount = Money.parse(amount.getText());
            String fingerprint = Fingerprint.of(value.getYear() + "|" + value.getMonthValue() + "|" + concept.getText() + "|" + type.getValue() + "|" + parsedAmount);
            NylRecord record = new NylRecord(0, value.getYear(), value.getMonthValue(), concept.getText(), type.getValue(), parsedAmount, "manual", fingerprint, "manual", false, "");
            NylGateway.SaveResult result = controller.saveAll(List.of(record));
            config.alert().show(Alert.AlertType.INFORMATION, "Entrada manual", result.inserted() + " registro guardado.");
            config.setSelectedYearValue().accept(value.getYear());
            config.setSelectedMonthValue().accept(value.getMonthValue());
            refresh.run();
            showNyl();
        });
        config.setPage().accept(page(
            "Entrada manual NYL",
            config.backButton().create("Volver a New York Life", this::showNyl),
            form,
            save
        ));
    }

    private VBox monthlyNylCards(TableView<NylRecord> table, HBox totalsPanel) {
        Label title = new Label("Resumen mensual NYL");
        title.getStyleClass().add("section-title");
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");
        VBox general = config.monthlyActionCard().create("General", "Ver todos", "", "", "", () -> {
            config.setSelectedMonthValue().accept(null);
            table.setItems(FXCollections.observableArrayList(controller.find(selectedYear(), null, null, null)));
            totalsPanel.getChildren().setAll(nylTotalsNodes(controller.totals(selectedYear(), null)));
        });
        general.getStyleClass().add("monthly-card-general");
        cards.getChildren().add(general);
        for (MonthlySourceTotals total : controller.monthlyTotals(null)) {
            VBox card = config.monthlyActionCard().create(
                monthName(total.month()) + " " + total.year(),
                "", "", "", "",
                () -> {
                    config.setSelectedYearValue().accept(total.year());
                    config.setSelectedMonthValue().accept(total.month());
                    table.setItems(FXCollections.observableArrayList(controller.find(total.year(), total.month(), null, null)));
                    totalsPanel.getChildren().setAll(nylTotalsNodes(controller.totals(total.year(), total.month())));
                }
            );
            config.addReviewMark().add(card, "nyl", "", total.year(), total.month());
            config.addMonthlyCardLine().add(card, "Comisiones: " + Money.format(total.credits()), "monthly-card-line");
            if (total.year() == 2026 && total.month() >= 1 && total.month() <= 4) {
                addDeductionBreakdown(card, total.year(), total.month());
                config.addMonthlyCardLine().add(card, "Total deducciones: " + Money.format(total.deductions()), "monthly-card-value-strong");
            } else {
                config.addMonthlyCardLine().add(card, "Deducciones: " + Money.format(total.deductions()), "monthly-card-line");
            }
            config.addMonthlyCardLine().add(card, "Resultado: " + Money.format(total.net()), "monthly-card-line");
            Optional<Double> pdfResult = controller.findPdfResult(total.year(), total.month());
            Optional<Double> nylBank = controller.findNylBank(total.year(), total.month());
            config.addMonthlyCardLine().add(card, "Dif. PDF vs NYL: " + pdfResult.map(value -> Money.format(value - total.net())).orElse("-"), "monthly-card-value-danger");
            config.addMonthlyCardDivider().accept(card);
            config.addMonthlyCardLine().add(card, "Reporte-NYL: " + pdfResult.map(Money::format).orElse("Pendiente"), "monthly-card-value-strong");
            config.addMonthlyCardLine().add(card, "Ingreso NYL Banco: " + nylBank.map(Money::format).orElse("Pendiente"), "monthly-card-value-strong");
            config.addMonthlyCardLine().add(card, "Dif. Reporte vs Banco: " + pdfResult.flatMap(report -> nylBank.map(bank -> Money.format(bank - report))).orElse("-"), "monthly-card-value-danger");
            config.addMonthlyCardLine().add(card, "Pendientes: " + total.pendingCount(), "monthly-card-line");
            Button edit = new Button("Editar");
            edit.setOnAction(event -> {
                event.consume();
                showPdfResultDialog(total.year(), total.month(), total.net(), () -> {
                    table.setItems(FXCollections.observableArrayList(controller.find(total.year(), total.month(), null, null)));
                    totalsPanel.getChildren().setAll(nylTotalsNodes(controller.totals(total.year(), total.month())));
                });
            });
            HBox cardActions = new HBox(8, edit, config.monthlyExportButton().create(() -> exportMonthly(total.year(), total.month())));
            cardActions.getStyleClass().add("bank-monthly-actions");
            card.getChildren().add(cardActions);
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }
        VBox box = new VBox(10, title, config.horizontalScroll().apply(cards));
        box.getStyleClass().add("monthly-section");
        return box;
    }

    private void addDeductionBreakdown(VBox card, int year, int month) {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (String section : DEDUCTION_SECTION_ORDER) {
            totals.put(section, 0.0);
        }
        for (NylRecord record : controller.find(year, month, null, null)) {
            if (record.isPendingReview() || !"deduccion".equalsIgnoreCase(record.getRecordType())) {
                continue;
            }
            String section = record.getSection() == null || record.getSection().isBlank() ? "Other Deductions" : record.getSection();
            if (!totals.containsKey(section)) {
                section = "Other Deductions";
            }
            totals.put(section, totals.get(section) + Math.abs(record.getAmount()));
        }
        totals.entrySet().stream()
            .filter(entry -> Math.abs(entry.getValue()) > 0.004)
            .forEach(entry -> config.addMonthlyCardLine().add(card, entry.getKey() + ": " + Money.format(entry.getValue()), "monthly-card-line"));
    }

    private List<Node> nylTotalsNodes(SourceTotals totals) {
        return List.of(
            config.miniTotal().create("Comisiones", Money.format(totals.income()), "income-total"),
            config.miniTotal().create("Deducciones", Money.format(Math.abs(totals.expenses())), "expense-total"),
            flowChart(totals)
        );
    }

    private void showPdfResultDialog(int year, int month, double calculatedResult, Runnable refresh) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar datos NYL");
        dialog.setHeaderText(monthName(month) + " " + year);
        TextField pdfResult = new TextField(controller.findPdfResult(year, month).map(Money::format).orElse(""));
        TextField nylBank = new TextField(controller.findNylBank(year, month).map(Money::format).orElse(""));
        Label calculated = new Label("Resultado calculado: " + Money.format(calculatedResult));
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Reporte-NYL"), pdfResult);
        form.addRow(1, new Label("NYL-Banco"), nylBank);
        form.addRow(2, calculated);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            try {
                controller.savePdfResult(year, month, Money.parse(pdfResult.getText()));
                controller.saveNylBank(year, month, Money.parse(nylBank.getText()));
                config.setSelectedYearValue().accept(year);
                config.setSelectedMonthValue().accept(month);
                refresh.run();
                showNyl();
            } catch (NumberFormatException exception) {
                config.alert().show(Alert.AlertType.ERROR, "Importe no válido", "Revisa Reporte-NYL y NYL-Banco. Usa formato 1234.56 o $1,234.56.");
            }
        });
    }

    public void exportMonthly(int year, int month) {
        File file = config.chooseExcel().choose("nyl-" + year + "-" + String.format("%02d", month) + ".xlsx");
        if (file == null) {
            return;
        }
        excelExportService.exportNylMonthly(file.toPath(), controller.find(year, month, null, null));
        config.alert().show(Alert.AlertType.INFORMATION, "Mes exportado", file.getAbsolutePath());
    }

    private void addMissingRow(TableView<NylRecord> table) {
        NylRecord context = table.getSelectionModel().getSelectedItem();
        if (context == null && !table.getItems().isEmpty()) {
            context = table.getItems().get(0);
        }
        int year = selectedYear() != null ? selectedYear() : (context == null ? LocalDate.now().getYear() : context.getYear());
        int month = selectedMonth() != null ? selectedMonth() : (context == null ? LocalDate.now().getMonthValue() : context.getMonth());
        String fingerprint = Fingerprint.of(year + "|" + month + "|registro faltante|credito|0.0|" + System.nanoTime());
        NylRecord record = new NylRecord(0, year, month, "Registro faltante", "Creditos", "credito", 0, "manual_en_revision", fingerprint, "manual", true, true, "Añadido manualmente durante revision");
        table.getItems().add(record);
        table.scrollTo(record);
        table.getSelectionModel().select(record);
    }

    private void showImportTotals(List<NylRecord> savedRecords, int inserted) {
        Label message = new Label(inserted + " registros nuevos guardados. Estos totales ya se calculan con las cifras revisadas y confirmadas.");
        message.getStyleClass().add("review-ok");
        Button back = new Button("Volver a New York Life");
        back.getStyleClass().add("primary");
        back.setOnAction(event -> showNyl());
        config.setPage().accept(page("Totales NYL confirmados", message, monthlyTotalsBox(savedRecords), back));
    }

    private VBox monthlyTotalsBox(List<NylRecord> records) {
        Label title = new Label("Totales por apartado y mes");
        title.getStyleClass().add("section-title");
        if (records.isEmpty()) {
            Label empty = new Label("No hay registros nuevos para totalizar.");
            empty.getStyleClass().add("section-subtitle");
            VBox box = new VBox(6, title, empty);
            box.getStyleClass().add("totals-box");
            return box;
        }
        Set<Integer> months = records.stream().map(NylRecord::getMonth).collect(Collectors.toCollection(TreeSet::new));
        Map<String, Map<Integer, Double>> totals = records.stream()
            .collect(Collectors.groupingBy(
                NylRecord::getSection,
                LinkedHashMap::new,
                Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(NylRecord::getAmount))
            ));
        GridPane grid = new GridPane();
        grid.getStyleClass().add("totals-grid");
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(headerLabel("Apartado"), 0, 0);
        int column = 1;
        for (Integer month : months) {
            grid.add(headerLabel(monthName(month)), column++, 0);
        }
        grid.add(headerLabel("Total"), column, 0);
        int row = 1;
        for (Map.Entry<String, Map<Integer, Double>> section : totals.entrySet()) {
            grid.add(bodyLabel(section.getKey()), 0, row);
            double sectionTotal = 0;
            column = 1;
            for (Integer month : months) {
                double value = section.getValue().getOrDefault(month, 0.0);
                sectionTotal += value;
                grid.add(moneyLabel(value), column++, row);
            }
            grid.add(moneyLabel(sectionTotal), column, row++);
        }
        VBox box = new VBox(10, title, grid);
        box.getStyleClass().add("totals-box");
        return box;
    }

    private VBox warningBox(List<String> warnings) {
        VBox box = new VBox(4);
        if (warnings.isEmpty()) {
            Label ok = new Label("Validacion inicial sin alertas. Revise igualmente antes de guardar.");
            ok.getStyleClass().add("review-ok");
            box.getChildren().add(ok);
            return box;
        }
        box.getStyleClass().add("review-warning");
        Label title = new Label("Alertas de revision");
        title.getStyleClass().add("review-warning-title");
        box.getChildren().add(title);
        for (String warning : warnings) {
            box.getChildren().add(new Label(warning));
        }
        return box;
    }

    private HBox monthNavigator(List<NylRecord> records, TableView<NylRecord> table) {
        HBox box = new HBox(8);
        box.getStyleClass().add("month-legend");
        records.stream().map(NylRecord::getMonth).distinct().sorted().forEach(month -> {
            Button button = new Button(monthName(month));
            button.getStyleClass().addAll("month-chip", "month-" + month);
            button.setOnAction(event -> {
                for (NylRecord record : table.getItems()) {
                    if (record.getMonth() == month) {
                        table.scrollTo(record);
                        table.getSelectionModel().select(record);
                        break;
                    }
                }
            });
            box.getChildren().add(button);
        });
        return box;
    }

    private HBox monthLegend(List<NylRecord> records) {
        HBox legend = new HBox(8);
        legend.getStyleClass().add("month-legend");
        records.stream().map(NylRecord::getMonth).distinct().sorted().forEach(month -> {
            Label label = new Label(monthName(month));
            label.getStyleClass().addAll("month-chip", "month-" + month);
            legend.getChildren().add(label);
        });
        return legend;
    }

    private VBox reviewSummaryBox(List<NylRecord> records, int hiddenExisting) {
        long reviewCount = records.stream().filter(NylRecord::isReviewRequired).count();
        long creditCount = records.stream().filter(record -> "Creditos".equals(record.getSection())).count();
        long deductionCount = records.stream().filter(record -> "Deducciones".equals(record.getSection())).count();
        Label title = new Label("Revisión pendiente");
        title.getStyleClass().add("section-title");
        Label detail = new Label(records.size() + " registros nuevos: " + creditCount + " créditos, " + deductionCount + " deducciones, " + reviewCount + " marcados para revisar. " + hiddenExisting + " ya existentes ocultos.");
        detail.getStyleClass().add("section-subtitle");
        VBox box = new VBox(6, title, detail);
        box.getStyleClass().add("totals-box");
        return box;
    }

    private VBox flowChart(SourceTotals totals) {
        double commissions = Math.max(0, totals.income());
        double deductions = Math.abs(Math.min(0, totals.expenses()));
        double max = Math.max(Math.max(commissions, deductions), 1.0);
        VBox chart = new VBox(
            8,
            flowHeader(),
            flowBar("Comisiones", commissions, max, "bank-flow-income-fill"),
            flowBar("Deducciones", deductions, max, "bank-flow-expense-fill")
        );
        chart.getStyleClass().add("bank-flow-chart");
        HBox.setHgrow(chart, Priority.ALWAYS);
        return chart;
    }

    private Label flowHeader() {
        Label label = new Label("Movimiento del periodo");
        label.getStyleClass().add("bank-flow-title");
        return label;
    }

    private HBox flowBar(String title, double amount, double max, String fillStyle) {
        Label name = new Label(title);
        name.getStyleClass().add("bank-flow-label");
        StackPane track = new StackPane();
        track.getStyleClass().add("bank-flow-track");
        Region fill = new Region();
        fill.getStyleClass().add(fillStyle);
        double width = 220 * (amount / max);
        fill.setMinWidth(width);
        fill.setPrefWidth(width);
        fill.setMaxWidth(width);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        HBox.setHgrow(track, Priority.ALWAYS);
        Label value = new Label(Money.format(amount));
        value.getStyleClass().add("bank-flow-value");
        HBox row = new HBox(10, name, track, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void showProcessing(String title, String message) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(64, 64);
        Label heading = new Label(title);
        heading.getStyleClass().add("processing-title");
        Label detail = new Label(message);
        detail.getStyleClass().add("processing-detail");
        detail.setWrapText(true);
        VBox box = new VBox(16, progress, heading, detail);
        box.getStyleClass().add("processing-box");
        box.setAlignment(Pos.CENTER);
        config.setPage().accept(page(title, box));
    }

    private VBox hubCard(String title, Runnable action) {
        Label label = new Label(title);
        label.getStyleClass().add("monthly-card-title");
        VBox card = new VBox(6, label);
        card.getStyleClass().add("monthly-card");
        card.setOnMouseClicked(event -> action.run());
        return card;
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

    private Label headerLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("totals-header");
        return label;
    }

    private Label bodyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("totals-body");
        return label;
    }

    private Label moneyLabel(double value) {
        Label label = new Label(Money.format(value));
        label.getStyleClass().add(value < 0 ? "totals-money-negative" : "totals-money");
        return label;
    }

    private Integer selectedYear() {
        return config.selectedYearValue().get();
    }

    private Integer selectedMonth() {
        return config.selectedMonthValue().get();
    }

    private String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> "Mes " + month;
        };
    }

    public record Config(
        Supplier<Integer> selectedYearValue,
        Supplier<Integer> selectedMonthValue,
        Consumer<Integer> setSelectedYearValue,
        Consumer<Integer> setSelectedMonthValue,
        Consumer<Parent> setPage,
        Runnable showAgentLedger,
        BackButtonFactory backButton,
        ChooseFileAction choosePdf,
        ChooseExcelAction chooseExcel,
        AlertAction alert,
        ConfirmAction confirm,
        ReviewPresenter reviewPresenter,
        MonthlyActionCardFactory monthlyActionCard,
        AddReviewMarkAction addReviewMark,
        AddMonthlyCardLineAction addMonthlyCardLine,
        Consumer<VBox> addMonthlyCardDivider,
        MonthlyExportButtonFactory monthlyExportButton,
        MiniTotalFactory miniTotal,
        java.util.function.Function<Node, ScrollPane> horizontalScroll
    ) {
    }

    @FunctionalInterface public interface BackButtonFactory { Button create(String text, Runnable action); }
    @FunctionalInterface public interface ChooseFileAction { File choose(); }
    @FunctionalInterface public interface ChooseExcelAction { File choose(String initialFileName); }
    @FunctionalInterface public interface AlertAction { void show(Alert.AlertType type, String title, String message); }
    @FunctionalInterface public interface ConfirmAction { boolean confirm(String title, String message, String confirmText); }
    @FunctionalInterface public interface ReviewPresenter { void show(String title, TableView<?> table, Runnable confirm, Node warningNode); }
    @FunctionalInterface public interface MonthlyActionCardFactory { VBox create(String title, String line1, String line2, String line3, String line4, Runnable action); }
    @FunctionalInterface public interface AddReviewMarkAction { void add(VBox card, String source, String alias, int year, int month); }
    @FunctionalInterface public interface AddMonthlyCardLineAction { void add(VBox card, String text, String styleClass); }
    @FunctionalInterface public interface MonthlyExportButtonFactory { Button create(Runnable action); }
    @FunctionalInterface public interface MiniTotalFactory { VBox create(String title, String value, String styleClass); }
}
