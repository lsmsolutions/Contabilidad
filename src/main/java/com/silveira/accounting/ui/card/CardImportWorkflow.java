package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardImportApplicationService;
import com.silveira.accounting.parsers.CreditCardStatementParser;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.concurrent.Task;

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
            config.showError().accept(config.rootCauseMessage().apply(task.getException()));
            config.showAccount().accept(alias);
        });
        Thread thread = new Thread(task, "silveira-card-import");
        thread.setDaemon(true);
        thread.start();
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
