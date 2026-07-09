package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.application.card.service.CardImportApplicationService;
import com.silveira.accounting.application.card.service.CardLedgerApplicationService;
import com.silveira.accounting.application.card.service.CardReviewApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.services.ExcelExportService;
import java.io.File;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class CardWorkflow {
    private final CardAccountApplicationService accounts;
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;
    private final CardImportApplicationService imports;
    private final CardReviewApplicationService reviews;
    private final ExcelExportService excelExportService;
    private final Config config;
    private Runnable showCardMovementsTabAction = () -> {};

    public CardWorkflow(
        CardAccountApplicationService accounts,
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions,
        CardImportApplicationService imports,
        CardReviewApplicationService reviews,
        ExcelExportService excelExportService,
        Config config
    ) {
        this.accounts = accounts;
        this.statements = statements;
        this.transactions = transactions;
        this.imports = imports;
        this.reviews = reviews;
        this.excelExportService = excelExportService;
        this.config = config;
    }

    public void showCards() {
        cardAccountWorkflow().showHub();
    }

    public void showCardAccount(String alias) {
        cardShellWorkflow().showAccount(alias);
    }

    public void showCardLedger() {
        new CardLedgerWorkflow(
            new CardLedgerApplicationService(accounts, statements),
            new CardLedgerWorkflow.Config(
                config.setPage(),
                config.backButton(),
                this::showCards,
                config.selectedYear(),
                config.selectedMonth(),
                config.selectedPeriodChanged()
            )
        ).showLedger();
    }

    public void showCardPeriodDetail(String alias, int year, int month) {
        cardShellWorkflow().showPeriodDetail(alias, year, month);
    }

    private CardAccountWorkflow cardAccountWorkflow() {
        return new CardAccountWorkflow(
            accounts,
            new CardAccountWorkflow.Config(
                (title, nodes) -> config.setPage().accept(config.page().build(title, nodes)),
                config.darkHub(),
                this::showCardAccount,
                config.rebuildSidebar(),
                (title, message, confirmText) -> config.confirm().confirm(title, message, confirmText),
                config.alert()
            )
        );
    }

    private CardShellWorkflow cardShellWorkflow() {
        return new CardShellWorkflow(
            statements,
            transactions,
            new CardShellWorkflow.Config(
                config.setPage(),
                config.page(),
                config.backButton(),
                this::showCards,
                cardTableFactory()::statementTable,
                cardTableFactory()::transactionTable,
                config.selectedYear(),
                config.selectedMonth(),
                config.selectedYearValue(),
                config.selectedMonthValue(),
                (year, month) -> config.selectedPeriodChanged().accept(year, month),
                cardTotalsView()::accumulatedNodes,
                cardTotalsView()::periodActivityNodes,
                cardPeriodWorkflow()::build,
                cardStatementCardsWorkflow()::refresh,
                cardImportAnalysisWorkflow()::importPdf,
                cardImportAnalysisWorkflow()::showAnalysis,
                cardEditWorkflow()::addManualStatement,
                config.helperNote(),
                cardEditWorkflow()::saveVisibleStatements,
                cardEditWorkflow()::addManualMovement,
                cardEditWorkflow()::saveVisibleMovements,
                action -> showCardMovementsTabAction = action
            )
        );
    }

    private CardPeriodWorkflow cardPeriodWorkflow() {
        return new CardPeriodWorkflow(
            statements,
            transactions,
            excelExportService,
            new CardPeriodWorkflow.Config(
                (alias, year, month) -> config.reviewMarkLabel().apply("card", alias, year, month),
                this::showCardPeriodDetail,
                cardEditWorkflow()::showPeriodDialog,
                (year, month) -> config.selectedPeriodChanged().accept(year, month),
                config.selectedYear(),
                config.selectedMonth(),
                cardTotalsView()::accumulatedNodes,
                cardStatementCardsWorkflow()::refresh,
                (title, message, confirmText) -> config.confirm().confirm(title, message, confirmText),
                config.chooseExcel(),
                config.safeFileName(),
                (title, message) -> config.alert().show(Alert.AlertType.INFORMATION, title, message)
            )
        );
    }

    private CardEditWorkflow cardEditWorkflow() {
        return new CardEditWorkflow(
            statements,
            transactions,
            new CardEditWorkflow.Config(
                (type, title, message) -> config.alert().show(type, title, message),
                config.rootCauseMessage(),
                cardStatementTitleFormatter()::title
            )
        );
    }

    private CardStatementCardsWorkflow cardStatementCardsWorkflow() {
        return new CardStatementCardsWorkflow(
            reviews,
            transactions,
            cardEditWorkflow(),
            new CardStatementCardsWorkflow.Config(
                cardStatementTitleFormatter()::title,
                () -> showCardMovementsTabAction
            )
        );
    }

    private CardImportAnalysisWorkflow cardImportAnalysisWorkflow() {
        return new CardImportAnalysisWorkflow(
            imports,
            statements,
            new CardImportAnalysisWorkflow.Config(
                config.choosePdf(),
                config.showProcessing(),
                config.importingChanged(),
                (title, message) -> config.alert().show(Alert.AlertType.ERROR, title, message),
                this::showCardAccount,
                config.rootCauseMessage(),
                config.setPage()
            )
        );
    }

    private CardTableFactory cardTableFactory() {
        return new CardTableFactory(
            reviews,
            statements,
            transactions,
            new CardTableFactory.Config(
                config.parseDate(),
                config.stringCellFactory(),
                config.moneyCellFactory()
            )
        );
    }

    private CardTotalsView cardTotalsView() {
        return new CardTotalsView(statements);
    }

    private CardStatementTitleFormatter cardStatementTitleFormatter() {
        return new CardStatementTitleFormatter();
    }

    public record Config(
        Consumer<Parent> setPage,
        CardShellWorkflow.PageFactory page,
        CardAccountWorkflow.PagePresenter darkHub,
        BiFunction<String, Runnable, Node> backButton,
        Runnable rebuildSidebar,
        Supplier<Integer> selectedYear,
        Supplier<Integer> selectedMonth,
        Supplier<Integer> selectedYearValue,
        Supplier<Integer> selectedMonthValue,
        BiConsumer<Integer, Integer> selectedPeriodChanged,
        Function<String, Label> helperNote,
        ReviewMarkLabelFactory reviewMarkLabel,
        Supplier<File> choosePdf,
        Function<String, File> chooseExcel,
        CardImportWorkflow.ProcessingPresenter showProcessing,
        Consumer<Boolean> importingChanged,
        Function<Throwable, String> rootCauseMessage,
        Function<String, LocalDate> parseDate,
        Function<String, String> safeFileName,
        Callback<TableColumn<CreditCardTransaction, String>, TableCell<CreditCardTransaction, String>> stringCellFactory,
        Callback<TableColumn<CreditCardTransaction, Double>, TableCell<CreditCardTransaction, Double>> moneyCellFactory,
        CardAccountWorkflow.ConfirmAction confirm,
        CardAccountWorkflow.AlertSink alert
    ) {
    }

    @FunctionalInterface
    public interface ReviewMarkLabelFactory {
        Node apply(String source, String accountAlias, int year, int month);
    }
}
