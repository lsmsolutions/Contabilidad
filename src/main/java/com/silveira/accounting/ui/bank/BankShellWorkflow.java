package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.controllers.bank.BankAccountController;
import com.silveira.accounting.controllers.bank.BankAccountDetailController;
import com.silveira.accounting.controllers.bank.BankAccountWorkflow;
import com.silveira.accounting.controllers.bank.BankImportController;
import com.silveira.accounting.controllers.bank.BankPeriodController;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.services.ExcelExportService;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class BankShellWorkflow {
    private final BankApplicationService bank;
    private final BankAccountController accounts;
    private final BankAccountDetailController details;
    private final BankAccountWorkflow accountWorkflow;
    private final BankImportController imports;
    private final BankPeriodController periods;
    private final ExcelExportService exports;
    private final Config config;

    public BankShellWorkflow(
        BankApplicationService bank,
        BankAccountController accounts,
        BankAccountDetailController details,
        BankAccountWorkflow accountWorkflow,
        BankImportController imports,
        BankPeriodController periods,
        ExcelExportService exports,
        Config config
    ) {
        this.bank = bank;
        this.accounts = accounts;
        this.details = details;
        this.accountWorkflow = accountWorkflow;
        this.imports = imports;
        this.periods = periods;
        this.exports = exports;
        this.config = config;
    }

    public void showHub() {
        config.selectedAccountAliasChanged().accept(null);
        accountWorkflow().showHub();
    }

    public void showAccount(String accountAlias) {
        config.selectedAccountAliasChanged().accept(accountAlias);
        config.setPage().accept(new BankAccountDetailScreenView(bank, details, imports).build(
            new BankAccountDetailScreenView.Config(
                accountAlias,
                accountPageTitle(accountAlias),
                config.selectedYear().get(),
                config.selectedMonth().get(),
                config.backButton().apply("Volver a Banco", this::showHub),
                (title, message) -> config.alert().accept(Alert.AlertType.INFORMATION, title, message),
                config.selectedPeriodChanged(),
                refresh -> importWorkflow().importPdf(refresh),
                refresh -> importWorkflow().showManualPeriodDialog(accountAlias, refresh),
                (period, refresh) -> importWorkflow().deleteSelectedPeriod(period, refresh),
                period -> config.reviewMarkFactory().apply(
                    "bank",
                    period.accountAlias(),
                    period.periodEnd().getYear(),
                    period.periodEnd().getMonthValue()
                ),
                period -> periodWorkflow().showPeriodDialog(period, () -> showAccount(config.selectedAccountAlias().get())),
                period -> periodWorkflow().exportMonthly(config.selectedAccountAlias().get(), period.periodEnd().getYear(), period.periodEnd().getMonthValue()),
                () -> showAccount(config.selectedAccountAlias().get())
            )
        ));
    }

    public void exportAllMonthly(int year, int month) {
        periodWorkflow().exportAllMonthly(year, month);
    }

    private BankAccountWorkflowView accountWorkflow() {
        return new BankAccountWorkflowView(
            accounts,
            accountWorkflow,
            new BankAccountWorkflowView.Config(
                config.page(),
                config.darkHub(),
                back -> config.backButton().apply("Volver a Banco", back),
                config.prompt(),
                (type, title, message) -> config.alert().accept(type, title, message),
                (title, message, confirmText) -> config.confirm().confirm(title, message, confirmText),
                config.rebuildSidebar(),
                this::showAccount
            )
        );
    }

    private BankImportWorkflowView importWorkflow() {
        return new BankImportWorkflowView(bank, imports, new BankImportWorkflowView.Config(
            config.choosePdf(),
            config.alert(),
            config.confirm(),
            config.processing(),
            config.rootCauseMessage(),
            config.reviewPresenter(),
            config.rebuildSidebar(),
            config.setPage(),
            back -> config.backButton().apply("Volver atras", back),
            this::showHub,
            this::showAccount,
            config.selectedAccountAlias(),
            config.selectedAccountAliasChanged(),
            config.selectedPeriodChanged(),
            config.selectedYear(),
            config.selectedMonth()
        ));
    }

    private BankPeriodWorkflowView periodWorkflow() {
        return new BankPeriodWorkflowView(
            periods,
            details,
            exports,
            new BankPeriodWorkflowView.Config(
                (type, title, message) -> config.alert().accept(type, title, message),
                config.rootCauseMessage(),
                config.chooseExcel()
            )
        );
    }

    private String accountPageTitle(String accountAlias) {
        return accounts.find(accountAlias)
            .map(BankAccountTextFormatter::pageTitle)
            .orElse(BankAccountTextFormatter.pageTitle(accountAlias));
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
        BiConsumer<String, String> processing,
        Function<Throwable, String> rootCauseMessage,
        BankImportWorkflowView.ReviewPresenter reviewPresenter,
        Runnable rebuildSidebar,
        Supplier<String> selectedAccountAlias,
        Consumer<String> selectedAccountAliasChanged,
        BiConsumer<Integer, Integer> selectedPeriodChanged,
        Supplier<Integer> selectedYear,
        Supplier<Integer> selectedMonth,
        ReviewMarkFactory reviewMarkFactory
    ) {}

    @FunctionalInterface
    public interface ReviewMarkFactory {
        Label apply(String source, String accountAlias, int year, int month);
    }
}
