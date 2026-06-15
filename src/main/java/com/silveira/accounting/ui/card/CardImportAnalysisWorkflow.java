package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardImportApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Parent;

public class CardImportAnalysisWorkflow {
    private final CardImportApplicationService imports;
    private final CardStatementApplicationService statements;
    private final Config config;

    public CardImportAnalysisWorkflow(
        CardImportApplicationService imports,
        CardStatementApplicationService statements,
        Config config
    ) {
        this.imports = imports;
        this.statements = statements;
        this.config = config;
    }

    public void importPdf(String alias) {
        new CardImportWorkflow(imports, new CardImportWorkflow.Config(
            config.choosePdf(),
            config.showProcessing(),
            config.importingChanged(),
            message -> config.showError().accept("No se pudo importar tarjeta", message),
            config.showAccount(),
            config.rootCauseMessage()
        )).importPdf(alias);
    }

    public void showAnalysis(String alias) {
        config.setPage().accept(new CardAnalysisView().build(
            alias,
            statements.analysis(alias),
            () -> config.showAccount().accept(alias)
        ));
    }

    public record Config(
        Supplier<File> choosePdf,
        BiConsumer<String, String> showProcessing,
        Consumer<Boolean> importingChanged,
        BiConsumer<String, String> showError,
        Consumer<String> showAccount,
        Function<Throwable, String> rootCauseMessage,
        Consumer<Parent> setPage
    ) {}
}
