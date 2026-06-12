package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardAccount;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CardAccountsHubView {
    public Hub build(
        List<CreditCardAccount> accounts,
        Runnable onAdd,
        Runnable onEdit,
        Runnable onDelete,
        Consumer<String> onOpenAccount
    ) {
        Button add = new Button("+ Anadir tarjeta");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> onAdd.run());

        Button edit = new Button("Editar tarjeta");
        edit.setOnAction(event -> onEdit.run());

        Button delete = new Button("Eliminar tarjeta");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> onDelete.run());

        sameSize(add, edit, delete);
        HBox actions = new HBox(10, add, edit, delete);

        if (accounts.isEmpty()) {
            Label empty = new Label("No hay tarjetas creadas. Importa un PDF o anade una tarjeta manualmente.");
            empty.getStyleClass().add("section-subtitle");
            return new Hub(empty, actions, true);
        }

        HBox cards = new HBox(14);
        cards.getStyleClass().add("bank-account-hub");
        for (CreditCardAccount account : accounts) {
            cards.getChildren().add(card(account, onOpenAccount));
        }
        return new Hub(cards, actions, false);
    }

    private VBox card(CreditCardAccount account, Consumer<String> onOpenAccount) {
        Label alias = new Label(account.getAlias());
        alias.getStyleClass().add("monthly-card-title");
        VBox card = new VBox(6, alias);
        card.getStyleClass().add("monthly-card");
        if (account.getBankName() != null && !account.getBankName().isBlank()) {
            Label bankName = new Label(account.getBankName());
            bankName.getStyleClass().add("monthly-card-line");
            card.getChildren().add(bankName);
        }
        if (account.getAccountLastDigits() != null && !account.getAccountLastDigits().isBlank()) {
            Label digits = new Label("Ending " + account.getAccountLastDigits());
            digits.getStyleClass().add("monthly-card-line");
            card.getChildren().add(digits);
        }
        card.setOnMouseClicked(event -> onOpenAccount.accept(account.getAlias()));
        return card;
    }

    private void sameSize(Button... buttons) {
        for (Button button : buttons) {
            button.setMinWidth(165);
            button.setPrefWidth(165);
        }
    }

    public record Hub(Node content, HBox actions, boolean empty) {
    }
}
