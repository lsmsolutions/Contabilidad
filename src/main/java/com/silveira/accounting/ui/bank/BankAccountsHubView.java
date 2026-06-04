package com.silveira.accounting.ui.bank;

import com.silveira.accounting.models.bank.BankAccount;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class BankAccountsHubView {
    public Hub build(List<BankAccount> accounts, Runnable onAdd, Runnable onEdit, Runnable onDelete, Consumer<String> onOpenAccount) {
        Button add = new Button("+ Añadir cuenta");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> onAdd.run());

        Button edit = new Button("Editar cuenta");
        edit.setOnAction(event -> onEdit.run());

        Button delete = new Button("Eliminar cuenta");
        delete.setOnAction(event -> onDelete.run());

        sameSize(add, edit, delete);
        HBox actions = new HBox(10, add, edit, delete);

        if (accounts.isEmpty()) {
            Label empty = new Label("No hay cuentas creadas. Importa un PDF o añade una cuenta manualmente.");
            empty.getStyleClass().add("section-subtitle");
            return new Hub(empty, actions);
        }

        HBox cards = new HBox(14);
        cards.getStyleClass().add("bank-account-hub");
        for (BankAccount account : accounts) {
            cards.getChildren().add(bankAccountCard(account, onOpenAccount));
        }
        return new Hub(cards, actions);
    }

    private VBox bankAccountCard(BankAccount account, Consumer<String> onOpenAccount) {
        Label alias = new Label(account.getAlias());
        alias.getStyleClass().add("monthly-card-title");
        VBox card = new VBox(6, alias);
        if (account.getBankName() != null && !account.getBankName().isBlank()) {
            Label bank = new Label(account.getBankName());
            bank.getStyleClass().add("monthly-card-line");
            card.getChildren().add(bank);
        }
        if (account.getAccountNumber() != null && !account.getAccountNumber().isBlank()) {
            Label number = new Label("Cuenta: " + account.getAccountNumber());
            number.getStyleClass().add("monthly-card-line");
            card.getChildren().add(number);
        }
        card.getStyleClass().add("monthly-card");
        card.setOnMouseClicked(event -> onOpenAccount.accept(account.getAlias()));
        return card;
    }

    private void sameSize(Button... buttons) {
        double width = 0;
        for (Button button : buttons) {
            width = Math.max(width, button.getText().length() * 9 + 28);
        }
        for (Button button : buttons) {
            button.setMinWidth(width);
        }
    }

    public record Hub(Node content, HBox actions) {
    }
}
