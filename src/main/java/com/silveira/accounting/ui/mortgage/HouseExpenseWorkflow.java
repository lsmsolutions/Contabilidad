package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.service.HouseExpenseApplicationService;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.bank.BankAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class HouseExpenseWorkflow {
    private final HouseExpenseApplicationService houseExpenses;
    private final Config config;

    public HouseExpenseWorkflow(HouseExpenseApplicationService houseExpenses, Config config) {
        this.houseExpenses = houseExpenses;
        this.config = config;
    }

    public void showHouseExpenses() {
        showHouseExpenses(null);
    }

    public void showHouseExpenses(String loanAlias) {
        HouseExpensePageView.Content content = new HouseExpensePageView(
            houseExpenses,
            new HouseExpensePageView.Config(
                config.owner(),
                this::paymentSourceOptions,
                config.alert()::show,
                config.rootCauseMessage()::apply
            )
        ).build(loanAlias);
        config.setPage().accept(page(
            loanAlias == null || loanAlias.isBlank() ? "Casa - Gastos" : loanAlias + " - Gastos",
            config.backButton().create("Volver a Hipotecas", config.showMortgages()),
            content.actions(),
            content.table()
        ));
    }

    private List<String> paymentSourceOptions() {
        List<String> options = new ArrayList<>();
        options.add("");
        for (BankAccount account : config.bankAccounts().get()) {
            options.add("Cuenta: " + account.getAlias());
        }
        for (CreditCardAccount account : config.cardAccounts().get()) {
            options.add("Tarjeta: " + account.getAlias());
        }
        return options;
    }

    private VBox page(String title, javafx.scene.Node... nodes) {
        Label heading = new Label(title);
        heading.getStyleClass().add("heading");
        VBox box = new VBox(18);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);
        box.setPadding(new javafx.geometry.Insets(28));
        box.getStyleClass().add("page");
        return box;
    }

    public record Config(
        Supplier<Window> owner,
        Supplier<List<BankAccount>> bankAccounts,
        Supplier<List<CreditCardAccount>> cardAccounts,
        Consumer<Parent> setPage,
        Runnable showMortgages,
        BackButtonFactory backButton,
        AlertAction alert,
        Function<Throwable, String> rootCauseMessage
    ) {
    }

    @FunctionalInterface
    public interface BackButtonFactory {
        Button create(String text, Runnable action);
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }
}
