package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardAccount;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class CardAccountFormView {
    public Optional<CreditCardAccount> show(CreditCardAccount current) {
        boolean editing = current != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Editar tarjeta" : "Anadir tarjeta");
        TextField alias = new TextField(editing ? text(current.getAlias()) : "");
        TextField bankName = new TextField(editing ? text(current.getBankName()) : "");
        TextField cardName = new TextField(editing ? text(current.getCardName()) : "");
        TextField digits = new TextField(editing ? text(current.getAccountLastDigits()) : "");
        TextArea notes = new TextArea(editing ? text(current.getNotes()) : "");
        notes.setPrefRowCount(3);
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Alias"), alias);
        form.addRow(1, new Label("Banco"), bankName);
        form.addRow(2, new Label("Tarjeta"), cardName);
        form.addRow(3, new Label("Ultimos digitos"), digits);
        form.addRow(4, new Label("Notas"), notes);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog.showAndWait()
            .filter(ButtonType.OK::equals)
            .map(result -> {
                String newAlias = alias.getText().isBlank() ? "tarjeta_" + last4(digits.getText()) : alias.getText().trim();
                return new CreditCardAccount(
                    editing ? current.getId() : 0,
                    newAlias,
                    bankName.getText(),
                    cardName.getText(),
                    digits.getText(),
                    notes.getText()
                );
            });
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private String last4(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return digits.isBlank() ? "nueva" : digits;
        }
        return digits.substring(digits.length() - 4);
    }
}
