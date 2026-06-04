package com.silveira.accounting.ui.bank;

import com.silveira.accounting.models.bank.BankAccount;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class BankAccountFormView {
    public Form buildForCreate() {
        TextField alias = new TextField();
        alias.setPromptText("cta_5705");
        TextField number = new TextField();
        number.setPromptText("Número de cuenta");
        TextField bank = new TextField();
        bank.setPromptText("Banco o entidad");
        TextField type = new TextField();
        type.setPromptText("Checking, Savings...");
        TextArea notes = new TextArea();
        notes.setPromptText("Notas");
        return build(alias, number, bank, type, notes, "Guardar cuenta");
    }

    public Form buildForEdit(BankAccount account) {
        TextField alias = new TextField(account.getAlias());
        TextField number = new TextField(account.getAccountNumber() == null ? "" : account.getAccountNumber());
        TextField bank = new TextField(account.getBankName() == null ? "" : account.getBankName());
        TextField type = new TextField(account.getAccountType() == null ? "" : account.getAccountType());
        TextArea notes = new TextArea(account.getNotes() == null ? "" : account.getNotes());
        return build(alias, number, bank, type, notes, "Guardar cambios");
    }

    private Form build(TextField alias, TextField number, TextField bank, TextField type, TextArea notes,
                       String saveText) {
        notes.setPrefRowCount(3);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Alias"), alias);
        form.addRow(1, new Label("Número"), number);
        form.addRow(2, new Label("Banco"), bank);
        form.addRow(3, new Label("Tipo"), type);
        form.addRow(4, new Label("Notas"), notes);

        Button save = new Button(saveText);
        save.getStyleClass().add("primary");

        return new Form(form, save, alias, number, bank, type, notes);
    }

    public record Form(GridPane node, Button save, TextField alias, TextField number, TextField bank, TextField type,
                       TextArea notes) {
        public BankAccount toBankAccount(long id) {
            return new BankAccount(id, alias.getText().trim(), number.getText(), bank.getText(), type.getText(), notes.getText());
        }
    }
}
