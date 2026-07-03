package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardImportApplicationService;
import com.silveira.accounting.parsers.CreditCardStatementParser;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public class CardImportWorkflow {
    private final CardImportApplicationService imports;
    private final Config config;

    public CardImportWorkflow(CardImportApplicationService imports, Config config) {
        this.imports = imports;
        this.config = config;
    }

    public void importPdf(String alias) {
        File file = config.choosePdf().get();
        if (file == null) {
            return;
        }
        config.importingChanged().accept(true);
        config.showProcessing().accept(
            "Importando tarjeta",
            "Leyendo el PDF de la tarjeta. Si es escaneado, se usara OCR y puede tardar unos minutos."
        );
        Task<CreditCardStatementParser.ParsedCreditCardStatement> task = new Task<>() {
            @Override
            protected CreditCardStatementParser.ParsedCreditCardStatement call() {
                return imports.importPdf(file.toPath());
            }
        };
        task.setOnSucceeded(event -> {
            config.importingChanged().accept(false);
            imports.saveImported(alias, task.getValue());
            config.showAccount().accept(alias);
        });
        task.setOnFailed(event -> {
            config.importingChanged().accept(false);
            handleImportFailure(alias, file, task.getException());
        });
        Thread thread = new Thread(task, "silveira-card-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void importPdfWithAi(String alias, File file) {
        config.importingChanged().accept(true);
        config.showProcessing().accept(
            "Leyendo tarjeta con IA",
            "La IA intentara leer el PDF. El resultado quedara pendiente de revision."
        );
        Task<CreditCardStatementParser.ParsedCreditCardStatement> task = new Task<>() {
            @Override
            protected CreditCardStatementParser.ParsedCreditCardStatement call() {
                return imports.importPdfWithAi(file.toPath());
            }
        };
        task.setOnSucceeded(event -> {
            config.importingChanged().accept(false);
            imports.saveImported(alias, task.getValue());
            config.showAccount().accept(alias);
        });
        task.setOnFailed(event -> {
            config.importingChanged().accept(false);
            config.showError().accept(config.rootCauseMessage().apply(task.getException()));
            config.showAccount().accept(alias);
        });
        Thread thread = new Thread(task, "silveira-card-ai-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleImportFailure(String alias, File file, Throwable exception) {
        ButtonType ai = new ButtonType("Intentar con IA", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
            Alert.AlertType.ERROR,
            config.rootCauseMessage().apply(exception)
                + "\n\nPuedes intentar leer este PDF con IA. El resultado quedara pendiente de revision.",
            ai,
            close
        );
        alert.setTitle("No se pudo importar tarjeta");
        alert.setHeaderText("No se pudo importar tarjeta");
        alert.showAndWait().ifPresentOrElse(selected -> {
            if (selected == ai) {
                importPdfWithAi(alias, file);
            } else {
                config.showAccount().accept(alias);
            }
        }, () -> config.showAccount().accept(alias));
    }

    public record Config(
        Supplier<File> choosePdf,
        BiConsumer<String, String> showProcessing,
        Consumer<Boolean> importingChanged,
        Consumer<String> showError,
        Consumer<String> showAccount,
        Function<Throwable, String> rootCauseMessage
    ) {
    }
}
