package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.controllers.bank.BankAccountDetailController;
import com.silveira.accounting.controllers.bank.BankPeriodController;
import com.silveira.accounting.services.ExcelExportService;
import java.io.File;
import java.util.function.Function;
import javafx.scene.control.Alert;

public class BankPeriodWorkflowView {
    private final BankPeriodController periods;
    private final BankAccountDetailController details;
    private final ExcelExportService exports;
    private final Config config;

    public BankPeriodWorkflowView(
        BankPeriodController periods,
        BankAccountDetailController details,
        ExcelExportService exports,
        Config config
    ) {
        this.periods = periods;
        this.details = details;
        this.exports = exports;
        this.config = config;
    }

    public void showPeriodDialog(BankPeriodSummary summary, Runnable refresh) {
        try {
            new BankPeriodDialogView().show(summary).ifPresent(edited -> {
                periods.updatePeriod(
                    edited.accountAlias(),
                    edited.sourcePdf(),
                    edited.start(),
                    edited.end(),
                    edited.openingBalance(),
                    edited.statementEndingBalance()
                );
                refresh.run();
            });
        } catch (RuntimeException exception) {
            config.alert().accept(Alert.AlertType.ERROR, "No se pudo guardar el periodo", config.rootCauseMessage().apply(exception));
        }
    }

    public void exportMonthly(String accountAlias, int year, int month) {
        File file = config.chooseExcel().apply("banco-" + safeFileName(accountAlias) + "-" + year + "-" + String.format("%02d", month) + ".xlsx");
        if (file == null) {
            return;
        }
        exports.exportBankMonthly(file.toPath(), details.findRows(year, month, null, null, accountAlias));
        config.alert().accept(Alert.AlertType.INFORMATION, "Mes exportado", file.getAbsolutePath());
    }

    private String safeFileName(String value) {
        return value == null || value.isBlank() ? "general" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public record Config(
        AlertSink alert,
        Function<Throwable, String> rootCauseMessage,
        Function<String, File> chooseExcel
    ) {}

    @FunctionalInterface
    public interface AlertSink {
        void accept(Alert.AlertType type, String title, String message);
    }
}
