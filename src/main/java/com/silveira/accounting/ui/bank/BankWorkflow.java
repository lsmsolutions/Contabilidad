package com.silveira.accounting.ui.bank;

import com.silveira.accounting.services.ExcelExportService;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;

public class BankWorkflow {
    private final BankModule bankModule;
    private final ExcelExportService excelExportService;
    private final Config config;

    public BankWorkflow(BankModule bankModule, ExcelExportService excelExportService, Config config) {
        this.bankModule = bankModule;
        this.excelExportService = excelExportService;
        this.config = config;
    }

    public void showBank() {
        bankShellWorkflow().showHub();
    }

    public void showBankAccount(String accountAlias) {
        bankShellWorkflow().showAccount(accountAlias);
    }

    private BankShellWorkflow bankShellWorkflow() {
        return new BankShellWorkflow(
            bankModule.application(),
            bankModule.accounts(),
            bankModule.accountDetails(),
            bankModule.accountWorkflow(),
            bankModule.imports(),
            bankModule.periods(),
            excelExportService,
            new BankShellWorkflow.Config(
                config.page(),
                config.darkHub(),
                config.setPage(),
                config.backButton(),
                config.prompt(),
                config.alert(),
                config.confirm(),
                config.choosePdf(),
                config.chooseExcel(),
                config.showProcessing(),
                config.rootCauseMessage(),
                config.reviewPresenter(),
                config.rebuildSidebar(),
                config.selectedAccountAlias(),
                config.selectedAccountAliasChanged(),
                config.selectedPeriodChanged(),
                config.selectedYear(),
                config.selectedMonth(),
                config.reviewMarkLabel()
            )
        );
    }

    public record Config(
        BankAccountWorkflowView.PagePresenter page,
        BankAccountWorkflowView.PagePresenter darkHub,
        Consumer<Parent> setPage,
        BiFunction<String, Runnable, Node> backButton,
        BankAccountWorkflowView.PromptAction prompt,
        BankImportWorkflowView.AlertSink alert,
        BankImportWorkflowView.ConfirmAction confirm,
        Supplier<File> choosePdf,
        Function<String, File> chooseExcel,
        BiConsumer<String, String> showProcessing,
        Function<Throwable, String> rootCauseMessage,
        BankImportWorkflowView.ReviewPresenter reviewPresenter,
        Runnable rebuildSidebar,
        Supplier<String> selectedAccountAlias,
        Consumer<String> selectedAccountAliasChanged,
        BiConsumer<Integer, Integer> selectedPeriodChanged,
        Supplier<Integer> selectedYear,
        Supplier<Integer> selectedMonth,
        BankShellWorkflow.ReviewMarkFactory reviewMarkLabel
    ) {
    }
}
