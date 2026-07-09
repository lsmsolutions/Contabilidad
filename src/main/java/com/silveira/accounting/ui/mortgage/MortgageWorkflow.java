package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.Parent;

public class MortgageWorkflow {
    private final MortgageApplicationService mortgageApplication;
    private final MortgageDetailWorkflow mortgageDetailWorkflow;
    private final HouseExpenseWorkflow houseExpenseWorkflow;
    private final Config config;

    public MortgageWorkflow(
        MortgageApplicationService mortgageApplication,
        MortgageDetailWorkflow mortgageDetailWorkflow,
        HouseExpenseWorkflow houseExpenseWorkflow,
        Config config
    ) {
        this.mortgageApplication = mortgageApplication;
        this.mortgageDetailWorkflow = mortgageDetailWorkflow;
        this.houseExpenseWorkflow = houseExpenseWorkflow;
        this.config = config;
    }

    public void showMortgages() {
        mortgageHubWorkflow().showMortgages();
    }

    public void showMortgageDetail(String alias) {
        mortgageDetailWorkflow.showMortgageDetail(alias);
    }

    public void showMortgageLedger() {
        new MortgageLedgerWorkflow(
            mortgageApplication,
            new MortgageLedgerWorkflow.Config(
                page -> config.setPage().set(page),
                (text, action) -> config.backButton().apply(text, action),
                this::showMortgages
            )
        ).showLedger();
    }

    private MortgageHubWorkflow mortgageHubWorkflow() {
        return new MortgageHubWorkflow(
            mortgageApplication,
            new MortgageHubWorkflow.Config(
                (title, header) -> config.promptText().prompt(title, header),
                config.rebuildSidebar(),
                this::showMortgageDetail,
                houseExpenseWorkflow::showHouseExpenses,
                (title, nodes) -> config.setDarkHubPage().set(title, nodes),
                (title, message, confirmText) -> config.confirm().confirm(title, message, confirmText)
            )
        );
    }

    public record Config(
        PromptTextAction promptText,
        Runnable rebuildSidebar,
        SetPageAction setPage,
        BackButtonAction backButton,
        SetDarkHubPageAction setDarkHubPage,
        ConfirmAction confirm
    ) {
    }

    @FunctionalInterface
    public interface PromptTextAction {
        Optional<String> prompt(String title, String header);
    }

    @FunctionalInterface
    public interface SetDarkHubPageAction {
        void set(String title, Node... nodes);
    }

    @FunctionalInterface
    public interface SetPageAction {
        void set(Parent page);
    }

    @FunctionalInterface
    public interface BackButtonAction {
        Node apply(String text, Runnable action);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }
}
