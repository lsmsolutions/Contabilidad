package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankImportSaveResult;
import com.silveira.accounting.application.bank.dto.BankImportAccountResolution;
import com.silveira.accounting.controllers.bank.BankImportController;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.ui.common.PdfImportModeDialog;
import com.silveira.accounting.utils.Money;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.control.TableView;
import javafx.collections.FXCollections;

public class BankImportWorkflowView {
    private final BankApplicationService bank;
    private final BankImportController imports;
    private final Config config;

    public BankImportWorkflowView(BankApplicationService bank, BankImportController imports, Config config) {
        this.bank = bank;
        this.imports = imports;
        this.config = config;
    }

    public void importPdf(Runnable refresh) {
        File file = config.choosePdf().get();
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
        List<BankTransaction> parsed;
        try {
            parsed = imports.parsePdf(file.toPath());
        } catch (RuntimeException exception) {
            if (handleImportFailure(file.toPath(), refresh, exception)) {
                return;
            }
            boolean runOcr = config.confirm().confirm(
                "No se pudo leer el PDF",
                exception.getMessage() + "\n\nEl archivo parece ser un PDF escaneado. La app puede intentar OCR para prellenar movimientos, pero todo quedará pendiente de revisión.",
                "Intentar OCR"
            );
            if (runOcr) {
                processOcr(file.toPath(), refresh);
            }
            return;
        }
        showImportReview(parsed, refresh);
    }

    public void showPendingReview(Runnable refresh) {
        showPendingReview(refresh, imports.findPendingReviewRows());
    }

    public void showPendingReview(Runnable refresh, List<BankTransaction> pendingRows) {
        String returnAccountAlias = config.selectedAccountAlias().get();
        Runnable back = () -> {
            if (returnAccountAlias == null || returnAccountAlias.isBlank()) {
                config.showBank().run();
            } else {
                config.showBankAccount().accept(returnAccountAlias);
            }
        };
        TableView<BankTransaction> table = new BankTransactionTableView(bank).build();
        table.setItems(FXCollections.observableArrayList(pendingRows));
        BankPendingReviewPageView.Page page = new BankPendingReviewPageView().build(config.backButton().apply(back), table);
        page.save().setOnAction(event -> {
            imports.savePendingReviewRows(List.copyOf(table.getItems()));
            refresh.run();
            back.run();
        });
        config.pageSetter().accept(page.node());
    }

    public void showManualPeriodDialog(String accountAlias, Runnable refresh) {
        LocalDate today = LocalDate.now();
        LocalDate initialStart = LocalDate.of(
            config.selectedYear().get() == null ? today.getYear() : config.selectedYear().get(),
            config.selectedMonth().get() == null ? today.getMonthValue() : config.selectedMonth().get(),
            1
        );
        new BankManualPeriodDialogView().show(initialStart).ifPresent(input -> {
            if (input.start() == null || input.end() == null) {
                config.alert().accept(Alert.AlertType.WARNING, "Periodo incompleto", "Indica fecha Desde y Hasta para el periodo manual.");
                return;
            }
            if (input.start().isAfter(input.end())) {
                config.alert().accept(Alert.AlertType.WARNING, "Periodo no válido", "La fecha Desde no puede ser posterior a Hasta.");
                return;
            }
            try {
                imports.saveManualPeriod(
                    accountAlias,
                    input.start(),
                    input.end(),
                    Money.parse(input.openingBalance()),
                    input.statementEndingBalance().isBlank() ? 0 : Money.parse(input.statementEndingBalance())
                );
                refresh.run();
            } catch (RuntimeException exception) {
                config.alert().accept(Alert.AlertType.ERROR, "No se pudo guardar el periodo", config.rootCauseMessage().apply(exception));
            }
        });
    }

    public void deleteSelectedPeriod(BankPeriodSummary selectedPeriod, Runnable refresh) {
        if (selectedPeriod == null) {
            config.alert().accept(Alert.AlertType.INFORMATION, "Selecciona un periodo", "Haz clic en una card de periodo antes de eliminarla.");
            return;
        }
        if (!new BankDeletePeriodConfirmationView().confirm(selectedPeriod, BankPeriodTextFormatter.title(selectedPeriod.statementPeriod()))) {
            return;
        }
        try {
            imports.deletePeriod(selectedPeriod);
            refresh.run();
        } catch (RuntimeException exception) {
            config.alert().accept(Alert.AlertType.ERROR, "No se pudo eliminar el periodo", config.rootCauseMessage().apply(exception));
        }
    }

    private void processOcr(Path pdf, Runnable refresh) {
        config.processing().accept("Procesando OCR Banco", "Leyendo el extracto escaneado. Esto puede tardar unos minutos.");
        Task<List<BankTransaction>> task = new Task<>() {
            @Override
            protected List<BankTransaction> call() {
                return imports.parseScannedPdf(pdf);
            }
        };
        task.setOnSucceeded(event -> showImportReview(task.getValue(), refresh));
        task.setOnFailed(event -> {
            config.alert().accept(Alert.AlertType.ERROR, "OCR Banco no disponible", config.rootCauseMessage().apply(task.getException()));
            config.showBank().run();
        });
        Thread thread = new Thread(task, "silveira-bank-ocr");
        thread.setDaemon(true);
        thread.start();
    }

    private void processAi(Path pdf, Runnable refresh) {
        config.processing().accept("Procesando IA Banco", "La IA intentara leer el extracto. Todo quedara pendiente de revision.");
        Task<List<BankTransaction>> task = new Task<>() {
            @Override
            protected List<BankTransaction> call() {
                return imports.parsePdfWithAi(pdf);
            }
        };
        task.setOnSucceeded(event -> showImportReview(task.getValue(), refresh));
        task.setOnFailed(event -> {
            config.alert().accept(Alert.AlertType.ERROR, "IA Banco no disponible", config.rootCauseMessage().apply(task.getException()));
            config.showBank().run();
        });
        Thread thread = new Thread(task, "silveira-bank-ai");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean handleImportFailure(Path pdf, Runnable refresh, RuntimeException exception) {
        ButtonType ocr = new ButtonType("Intentar OCR", ButtonBar.ButtonData.OTHER);
        ButtonType ai = new ButtonType("Intentar con IA", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
            Alert.AlertType.ERROR,
            config.rootCauseMessage().apply(exception)
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
        return true;
    }

    private void showImportReview(List<BankTransaction> parsed, Runnable refresh) {
        if (parsed.isEmpty()) {
            if (showStatementWithoutTransactions(refresh)) {
                return;
            }
            config.alert().accept(Alert.AlertType.WARNING, "No se detectaron movimientos", "No se pudieron detectar transacciones ni un periodo con saldo. Puedes usar entrada manual o revisar el PDF original.");
            return;
        }
        config.rebuildSidebar().run();
        AccountChoice accountChoice = resolveAccountAliasForImport(parsed);
        if (accountChoice.cancelled()) {
            return;
        }
        BankImportReviewPageView.Page page = new BankImportReviewPageView(bank).build(parsed, accountChoice.overrideAlias());
        page.saveProgress().setOnAction(event -> saveRows(List.copyOf(page.table().getItems()), page.table(), refresh, true));
        config.reviewPresenter().show("Revisión Banco", page.table(), () -> {
            saveRows(List.copyOf(page.table().getItems()), page.table(), refresh, false);
        }, page.warningNode());
    }

    private boolean showStatementWithoutTransactions(Runnable refresh) {
        var pending = imports.pendingStatementPeriod();
        String accountAlias = config.selectedAccountAlias().get();
        if (pending.isEmpty() || accountAlias == null || accountAlias.isBlank()) {
            return false;
        }
        var period = pending.get();
        String symbol = accountAlias.toUpperCase(Locale.ROOT).contains("EUR") ? "€" : "$";
        DateTimeFormatter date = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
        String message = "This statement has no account activity, but contains a valid balance."
            + "\n\nAccount: " + accountAlias
            + "\nPeriod: " + period.periodStart().format(date) + " – " + period.periodEnd().format(date)
            + "\nOpening balance: " + currency(symbol, period.openingBalance())
            + "\nMoney out: " + currency(symbol, 0)
            + "\nMoney in: " + currency(symbol, 0)
            + "\nClosing balance: " + currency(symbol, period.statementEndingBalance())
            + "\nTransactions: 0";
        ButtonType save = new ButtonType("Save Statement", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, save, cancel);
        alert.setTitle("Statement without transactions");
        alert.setHeaderText("No transactions found");
        if (alert.showAndWait().orElse(cancel) != save) {
            return true;
        }
        try {
            imports.savePendingStatementPeriod(accountAlias);
            config.selectedPeriodChanged().accept(period.periodEnd().getYear(), period.periodEnd().getMonthValue());
            config.rebuildSidebar().run();
            refresh.run();
            config.showBankAccount().accept(accountAlias);
        } catch (RuntimeException exception) {
            config.alert().accept(Alert.AlertType.ERROR, "Could not save statement", config.rootCauseMessage().apply(exception));
        }
        return true;
    }

    private String currency(String symbol, double amount) {
        return symbol + String.format(Locale.US, "%,.2f", amount);
    }

    private AccountChoice resolveAccountAliasForImport(List<BankTransaction> parsed) {
        String selectedAccountAlias = config.selectedAccountAlias().get();
        if (selectedAccountAlias == null || selectedAccountAlias.isBlank()) {
            return new AccountChoice(false, selectedAccountAlias);
        }

        List<String> detectedAliases = parsed.stream()
            .map(BankTransaction::getAccountAlias)
            .filter(alias -> alias != null && !alias.isBlank())
            .distinct()
            .filter(alias -> !alias.equals(selectedAccountAlias))
            .toList();

        if (detectedAliases.isEmpty()) {
            return new AccountChoice(false, selectedAccountAlias);
        }

        BankImportAccountResolution resolution = bank.imports().resolveAccount(selectedAccountAlias, detectedAliases);
        if (resolution.status() == BankImportAccountResolution.Status.MATCH) {
            return new AccountChoice(false, selectedAccountAlias);
        }

        ButtonType useDetected = new ButtonType(
            detectedAliases.size() == 1 ? "Importar en cuenta nueva" : "Importar en cuentas nuevas",
            ButtonBar.ButtonData.OK_DONE
        );
        ButtonType keepSelected = new ButtonType("Continuar con " + selectedAccountAlias, ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("Cancelar importacion", ButtonBar.ButtonData.CANCEL_CLOSE);
        String configured = resolution.configuredEnding().isBlank() ? "no disponible" : "termina en " + resolution.configuredEnding();
        String detected = resolution.detectedEndings().stream().allMatch(String::isBlank)
            ? String.join("\n", resolution.detectedAliases())
            : resolution.detectedEndings().stream().map(ending -> ending.isBlank() ? "no disponible" : "termina en " + ending).distinct().reduce((a, b) -> a + "\n" + b).orElse("no disponible");
        String message = "Cuenta seleccionada:\n" + selectedAccountAlias + " (" + configured + ")"
            + "\n\nCuenta indicada por el PDF:\n" + detected
            + "\n\nLos numeros no se pudieron confirmar como la misma cuenta. Elige donde guardar los movimientos.";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", keepSelected, useDetected, cancel);
        Label content = new Label(message);
        content.setWrapText(true);
        content.setMaxWidth(520);
        alert.setTitle("Comprobar cuenta del PDF");
        alert.setHeaderText(resolution.status() == BankImportAccountResolution.Status.MISMATCH
            ? "La terminacion del PDF no coincide"
            : "No se pudo verificar automaticamente la cuenta");
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinWidth(560);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);
        ButtonType selected = alert.showAndWait().orElse(cancel);
        if (selected == cancel) return new AccountChoice(true, selectedAccountAlias);
        return new AccountChoice(false, selected == useDetected ? null : selectedAccountAlias);
    }

    private record AccountChoice(boolean cancelled, String overrideAlias) {}

    private void saveRows(List<BankTransaction> rows, TableView<BankTransaction> table, Runnable refresh, boolean keepProgress) {
        if (rows.isEmpty()) {
            config.alert().accept(Alert.AlertType.INFORMATION, "Nada para guardar", "No hay transacciones para guardar.");
            return;
        }
        for (BankTransaction row : rows) {
            if (!keepProgress && row.isPendingReview()) {
                boolean proceed = config.confirm().confirm(
                    "Quedan pendientes",
                    "Hay transacciones sin revisar. Puedes guardar progreso para conservarlas como pendientes.",
                    "Guardar todo igualmente"
                );
                if (!proceed) {
                    return;
                }
                break;
            }
        }
        BankImportSaveResult result = imports.saveRows(rows);
        table.getItems().removeAll(rows);
        config.alert().accept(Alert.AlertType.INFORMATION, "Banco guardado", result.inserted() + " nuevas. " + result.reviewed() + " revisadas y " + result.pending() + " pendientes guardadas.");
        openMonthAfterReview(rows);
    }

    private void openMonthAfterReview(List<BankTransaction> rows) {
        rows.stream()
            .map(BankTransaction::getDate)
            .filter(date -> date != null)
            .max(LocalDate::compareTo)
            .ifPresent(date -> config.selectedPeriodChanged().accept(date.getYear(), date.getMonthValue()));
        String alias = rows.stream()
            .map(BankTransaction::getAccountAlias)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse(config.selectedAccountAlias().get());
        if (alias != null && !alias.isBlank()) {
            config.selectedAccountAliasChanged().accept(alias);
            config.showBankAccount().accept(alias);
        } else {
            config.showBank().run();
        }
    }

    public record Config(
        Supplier<File> choosePdf,
        AlertSink alert,
        ConfirmAction confirm,
        BiConsumer<String, String> processing,
        Function<Throwable, String> rootCauseMessage,
        ReviewPresenter reviewPresenter,
        Runnable rebuildSidebar,
        Consumer<javafx.scene.Parent> pageSetter,
        Function<Runnable, Node> backButton,
        Runnable showBank,
        Consumer<String> showBankAccount,
        Supplier<String> selectedAccountAlias,
        Consumer<String> selectedAccountAliasChanged,
        BiConsumer<Integer, Integer> selectedPeriodChanged,
        Supplier<Integer> selectedYear,
        Supplier<Integer> selectedMonth
    ) {}

    @FunctionalInterface
    public interface AlertSink {
        void accept(Alert.AlertType type, String title, String message);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }

    @FunctionalInterface
    public interface ReviewPresenter {
        void show(String title, TableView<BankTransaction> table, Runnable confirm, Node warningNode);
    }
}
