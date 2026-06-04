package com.silveira.accounting.ui.bank;

import com.silveira.accounting.controllers.bank.BankAccountController;
import com.silveira.accounting.controllers.bank.BankAccountWorkflow;
import com.silveira.accounting.models.bank.BankAccount;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.Alert;

public class BankAccountWorkflowView {
    private final BankAccountController accounts;
    private final BankAccountWorkflow workflow;
    private final Config config;

    public BankAccountWorkflowView(BankAccountController accounts, BankAccountWorkflow workflow, Config config) {
        this.accounts = accounts;
        this.workflow = workflow;
        this.config = config;
    }

    public void showHub() {
        BankAccountsHubView.Hub hub = new BankAccountsHubView().build(
            accounts.list(),
            this::showAddAccount,
            this::editFromHub,
            this::deleteFromHub,
            config.showAccount()
        );
        config.darkHub().show("Banco", hub.content(), hub.actions());
    }

    public void showAddAccount() {
        BankAccountFormView.Form form = new BankAccountFormView().buildForCreate();
        form.save().setOnAction(event -> {
            BankAccount account = accounts.create(
                form.alias().getText(),
                form.number().getText(),
                form.bank().getText(),
                form.type().getText(),
                form.notes().getText()
            );
            config.rebuildSidebar().run();
            config.showAccount().accept(account.getAlias());
        });
        config.page().show("Añadir cuenta bancaria", config.backButton().apply(this::showHub), form.node(), form.save());
    }

    private void deleteFromHub() {
        Optional<String> input = config.prompt().prompt("Eliminar cuenta bancaria", "Escribe el alias exacto de la cuenta que quieres eliminar.");
        if (input.isEmpty() || input.get().isBlank()) {
            return;
        }
        String alias = input.get().trim();
        if (!accounts.exists(alias)) {
            config.alert().accept(Alert.AlertType.WARNING, "Cuenta no encontrada", "No existe una cuenta bancaria con alias: " + alias);
            return;
        }
        boolean proceed = config.confirm().confirm(
            "Confirmar eliminacion",
            "Se eliminara la cuenta '" + alias + "' y todas sus transacciones/conciliaciones asociadas.\n\nEsta accion no se puede deshacer.",
            "Eliminar cuenta"
        );
        if (!proceed) {
            return;
        }
        workflow.delete(alias);
        config.rebuildSidebar().run();
        showHub();
    }

    private void editFromHub() {
        Optional<String> currentInput = config.prompt().prompt("Editar cuenta bancaria", "Escribe el alias actual de la cuenta que quieres editar.");
        if (currentInput.isEmpty() || currentInput.get().isBlank()) {
            return;
        }
        String currentAlias = currentInput.get().trim();
        Optional<BankAccount> current = workflow.findForEdit(currentAlias);
        if (current.isEmpty()) {
            config.alert().accept(Alert.AlertType.WARNING, "Cuenta no encontrada", "No existe una cuenta bancaria con alias: " + currentAlias);
            return;
        }
        showEditAccount(current.get());
    }

    private void showEditAccount(BankAccount current) {
        BankAccountFormView.Form form = new BankAccountFormView().buildForEdit(current);
        form.save().setOnAction(event -> {
            BankAccountWorkflow.UpdateResult result = workflow.update(current, form.toBankAccount(current.getId()));
            if (result.status() == BankAccountWorkflow.Status.ALIAS_REQUIRED) {
                config.alert().accept(Alert.AlertType.WARNING, "Alias requerido", "La cuenta necesita un alias.");
                return;
            }
            if (result.status() == BankAccountWorkflow.Status.DUPLICATE) {
                config.alert().accept(Alert.AlertType.WARNING, "Nombre ya existe", "Ya existe una cuenta bancaria con alias: " + result.alias());
                return;
            }
            config.rebuildSidebar().run();
            showHub();
        });
        config.page().show("Editar cuenta bancaria", config.backButton().apply(this::showHub), form.node(), form.save());
    }

    public record Config(
        PagePresenter page,
        PagePresenter darkHub,
        Function<Runnable, Node> backButton,
        PromptAction prompt,
        AlertSink alert,
        ConfirmAction confirm,
        Runnable rebuildSidebar,
        Consumer<String> showAccount
    ) {}

    @FunctionalInterface
    public interface PagePresenter {
        void show(String title, Node... nodes);
    }

    @FunctionalInterface
    public interface PromptAction {
        Optional<String> prompt(String title, String message);
    }

    @FunctionalInterface
    public interface AlertSink {
        void accept(Alert.AlertType type, String title, String message);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }
}
