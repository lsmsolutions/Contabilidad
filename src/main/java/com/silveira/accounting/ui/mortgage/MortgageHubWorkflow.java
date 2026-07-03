package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
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
        Button add = new Button("+ A\u00f1adir hipoteca");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> showAddMortgageLoan());
        Button edit = new Button("Editar hipoteca");
        edit.setOnAction(event -> showEditMortgageLoan(aliases));
        Button delete = new Button("Eliminar hipoteca");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> showDeleteMortgageLoan(aliases));
        sizeActionButton(add);
        sizeActionButton(edit);
        sizeActionButton(delete);
        HBox actions = new HBox(10, add, edit, delete);
        actions.getStyleClass().add("mortgage-hub-actions");

        HBox cards = new HBox(14);
        cards.getStyleClass().add("monthly-card-row");
        for (String alias : aliases) {
            VBox card = mortgageHubCard(alias);
            card.getStyleClass().add("monthly-card");
            HBox cardActions = new HBox(8);
            cardActions.getStyleClass().add("mortgage-card-actions");
            Button info = new Button("Informaci\u00f3n");
            Button expenses = new Button("Gastos");
            sizeCardButton(info);
            sizeCardButton(expenses);
            info.setOnAction(event -> {
                event.consume();
                config.showMortgageDetail().show(alias);
            });
            expenses.setOnAction(event -> {
                event.consume();
                config.showHouseExpenses().accept(alias);
            });
            cardActions.getChildren().addAll(info, expenses);
            card.getChildren().add(cardActions);
            cards.getChildren().add(card);
        }
        config.setDarkHubPage().set("Hipotecas", actions, cards);
    }

    private VBox mortgageHubCard(String alias) {
        Label heading = new Label(alias);
        heading.getStyleClass().add("monthly-card-title");
        GridPane lines = new GridPane();
        lines.getStyleClass().add("monthly-card-grid");
        Label label = new Label("Statements");
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(String.valueOf(mortgage.statements().findByLoan(alias, null, null).size()));
        value.getStyleClass().add("monthly-card-line");
        lines.add(label, 0, 0);
        lines.add(value, 1, 0);
        VBox card = new VBox(0, heading, lines);
        card.setOnMouseClicked(event -> config.showMortgageDetail().show(alias));
        return card;
    }

    private void sizeActionButton(Button button) {
        button.setMinWidth(180);
        button.setPrefWidth(180);
        button.setMinHeight(36);
        button.setPrefHeight(36);
    }

    private void sizeCardButton(Button button) {
        button.setMinWidth(128);
        button.setPrefWidth(128);
        button.setMinHeight(32);
        button.setPrefHeight(32);
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

    private void showEditMortgageLoan(List<String> aliases) {
        Optional<String> current = config.promptText().prompt("Editar hipoteca", "Escribe el alias actual de la hipoteca.");
        if (current.isEmpty() || current.get().isBlank()) {
            return;
        }
        String oldAlias = current.get().trim();
        if (!aliases.contains(oldAlias)) {
            return;
        }
        Optional<String> updated = config.promptText().prompt("Editar hipoteca", "Nuevo alias para " + oldAlias);
        if (updated.isEmpty() || updated.get().isBlank()) {
            return;
        }
        String newAlias = updated.get().trim();
        if (oldAlias.equals(newAlias)) {
            return;
        }
        mortgage.renameLoan(oldAlias, newAlias);
        config.rebuildSidebar().run();
        config.showMortgageDetail().show(newAlias);
    }

    private void showDeleteMortgageLoan(List<String> aliases) {
        Optional<String> alias = config.promptText().prompt("Eliminar hipoteca", "Escribe el alias exacto de la hipoteca que quieres eliminar.");
        if (alias.isEmpty() || alias.get().isBlank()) {
            return;
        }
        String value = alias.get().trim();
        if (!aliases.contains(value)) {
            return;
        }
        if (!config.confirm().confirm(
            "Eliminar hipoteca",
            "Se eliminar\u00e1 la hipoteca " + value + ", sus statements, movimientos y gastos asociados.\n\nEsta acci\u00f3n no se puede deshacer.",
            "Eliminar"
        )) {
            return;
        }
        mortgage.deleteLoan(value);
        config.rebuildSidebar().run();
        showMortgages();
    }

    public record Config(
        PromptTextAction promptText,
        Runnable rebuildSidebar,
        ShowMortgageDetailAction showMortgageDetail,
        Consumer<String> showHouseExpenses,
        SetDarkHubPageAction setDarkHubPage,
        ConfirmAction confirm
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
    public interface SetDarkHubPageAction {
        void set(String title, Node... nodes);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }
}
