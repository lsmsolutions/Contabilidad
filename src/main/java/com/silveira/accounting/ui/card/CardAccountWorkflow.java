package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.models.CreditCardAccount;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Alert;

public class CardAccountWorkflow {
    private final CardAccountApplicationService accounts;
    private final Config config;

    public CardAccountWorkflow(CardAccountApplicationService accounts, Config config) {
        this.accounts = accounts;
        this.config = config;
    }

    public void showHub() {
        CardAccountsHubView.Hub hub = new CardAccountsHubView().build(
            accounts.findAll(),
            this::showAdd,
            this::showEdit,
            this::showDelete,
            config.openAccount()
        );
        if (hub.empty()) {
            config.page().show("Tarjetas", hub.content(), hub.actions());
        } else {
            config.darkHub().show("Tarjetas", hub.content(), hub.actions());
        }
    }

    private void showAdd() {
        showAccountDialog(null);
    }

    private void showEdit() {
        chooseAccount("Editar tarjeta", "No hay tarjetas para editar.")
            .ifPresent(this::showAccountDialog);
    }

    private void showDelete() {
        chooseAccount("Eliminar tarjeta", "No hay tarjetas para eliminar.").ifPresent(account -> {
            if (!config.confirm().confirm(
                "Eliminar tarjeta",
                "Se eliminaran la tarjeta, sus estados, movimientos y alertas.",
                "Eliminar"
            )) {
                return;
            }
            accounts.delete(account.getAlias());
            config.rebuildSidebar().run();
            showHub();
        });
    }

    private Optional<CreditCardAccount> chooseAccount(String title, String emptyMessage) {
        List<CreditCardAccount> available = accounts.findAll();
        if (available.isEmpty()) {
            config.alert().show(Alert.AlertType.INFORMATION, "Sin tarjetas", emptyMessage);
            return Optional.empty();
        }
        return new CardAccountSelectorDialogView().show(title, available);
    }

    private void showAccountDialog(CreditCardAccount current) {
        boolean editing = current != null;
        new CardAccountFormView().show(current).ifPresent(account -> {
            if (editing) {
                accounts.update(current.getAlias(), account);
            } else {
                accounts.save(account);
            }
            config.rebuildSidebar().run();
            config.openAccount().accept(account.getAlias());
        });
    }

    public record Config(
        PagePresenter page,
        PagePresenter darkHub,
        Consumer<String> openAccount,
        Runnable rebuildSidebar,
        ConfirmAction confirm,
        AlertSink alert
    ) {}

    @FunctionalInterface
    public interface PagePresenter {
        void show(String title, Node... nodes);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }

    @FunctionalInterface
    public interface AlertSink {
        void show(Alert.AlertType type, String title, String message);
    }
}
