package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardAccount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

public class CardAccountSelectorDialogView {
    public Optional<CreditCardAccount> show(String title, List<CreditCardAccount> accounts) {
        Dialog<CreditCardAccount> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Selecciona una tarjeta");
        ComboBox<CreditCardAccount> cards = new ComboBox<>(FXCollections.observableArrayList(accounts));
        cards.setMaxWidth(Double.MAX_VALUE);
        cards.setValue(accounts.get(0));
        cards.setConverter(new StringConverter<>() {
            @Override
            public String toString(CreditCardAccount account) {
                return accountLabel(account);
            }

            @Override
            public CreditCardAccount fromString(String value) {
                return accounts.stream()
                    .filter(account -> accountLabel(account).equals(value))
                    .findFirst()
                    .orElse(null);
            }
        });
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Tarjeta:"), cards);
        GridPane.setHgrow(cards, Priority.ALWAYS);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> ButtonType.OK.equals(button) ? cards.getValue() : null);
        return dialog.showAndWait();
    }

    private String accountLabel(CreditCardAccount account) {
        if (account == null) {
            return "";
        }
        List<String> details = new ArrayList<>();
        if (account.getBankName() != null && !account.getBankName().isBlank()) {
            details.add(account.getBankName().trim());
        }
        if (account.getCardName() != null && !account.getCardName().isBlank()) {
            details.add(account.getCardName().trim());
        }
        if (account.getAccountLastDigits() != null && !account.getAccountLastDigits().isBlank()) {
            details.add("Ending " + account.getAccountLastDigits().trim());
        }
        String alias = account.getAlias() == null || account.getAlias().isBlank() ? "Tarjeta" : account.getAlias().trim();
        return details.isEmpty() ? alias : alias + " - " + String.join(" - ", details);
    }
}
