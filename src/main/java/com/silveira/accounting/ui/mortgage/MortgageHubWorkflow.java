package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import java.util.List;
import java.util.Optional;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MortgageHubWorkflow {
    private final MortgageApplicationService mortgage;
    private final Config config;

    public MortgageHubWorkflow(MortgageApplicationService mortgage, Config config) {
        this.mortgage = mortgage;
        this.config = config;
    }

    public void showMortgages() {
        List<String> aliases = mortgage.statements().findLoanAliases();
        Button add = new Button("+ Anadir hipoteca");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> showAddMortgageLoan());
        Button houseExpenses = new Button("Casa - Gastos");
        houseExpenses.setOnAction(event -> config.showHouseExpenses().run());
        HBox actions = new HBox(10, add, houseExpenses);
        HBox cards = new HBox(14);
        cards.getStyleClass().add("monthly-card-row");
        for (String alias : aliases) {
            VBox card = config.monthlyActionCard().create(
                alias,
                "Statements: " + mortgage.statements().findByLoan(alias, null, null).size(),
                "",
                "",
                "",
                () -> config.showMortgageDetail().show(alias)
            );
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }
        config.setDarkHubPage().set("Hipotecas", cards, actions);
    }

    private void showAddMortgageLoan() {
        Optional<String> alias = config.promptText().prompt("Nueva hipoteca", "Alias de la hipoteca");
        if (alias.isEmpty() || alias.get().isBlank()) {
            return;
        }
        String value = alias.get().trim();
        mortgage.statements().saveLoan(value, "", "", "", "");
        config.rebuildSidebar().run();
        config.showMortgageDetail().show(value);
    }

    public record Config(
        PromptTextAction promptText,
        Runnable rebuildSidebar,
        ShowMortgageDetailAction showMortgageDetail,
        Runnable showHouseExpenses,
        MonthlyActionCardFactory monthlyActionCard,
        SetDarkHubPageAction setDarkHubPage
    ) {
    }

    @FunctionalInterface
    public interface PromptTextAction {
        Optional<String> prompt(String title, String header);
    }

    @FunctionalInterface
    public interface ShowMortgageDetailAction {
        void show(String alias);
    }

    @FunctionalInterface
    public interface MonthlyActionCardFactory {
        VBox create(String title, String line1, String line2, String line3, String line4, Runnable action);
    }

    @FunctionalInterface
    public interface SetDarkHubPageAction {
        void set(String title, Node... nodes);
    }
}
