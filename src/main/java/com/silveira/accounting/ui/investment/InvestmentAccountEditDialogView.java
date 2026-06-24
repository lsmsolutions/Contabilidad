package com.silveira.accounting.ui.investment;

import com.silveira.accounting.models.investment.InvestmentAccount;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class InvestmentAccountEditDialogView {
    public Optional<InvestmentAccount> show(InvestmentAccount account) {
        Dialog<InvestmentAccount> dialog = new Dialog<>();
        dialog.setTitle(blank(account.getAlias()) ? "Add investment" : "Edit investment");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField alias = field(account.getAlias());
        TextField provider = field(account.getProviderName());
        TextField type = field(account.getAccountType());
        TextField accountNumber = field(account.getAccountNumber());
        TextArea notes = new TextArea(text(account.getNotes()));
        notes.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        addRow(grid, 0, "Alias", alias);
        addRow(grid, 1, "Provider", provider);
        addRow(grid, 2, "Investment type", type);
        addRow(grid, 3, "Account", accountNumber);
        grid.add(new Label("Notes"), 0, 4);
        grid.add(notes, 1, 4);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            InvestmentAccount updated = new InvestmentAccount();
            updated.setId(account.getId());
            updated.setAlias(text(alias.getText()));
            updated.setProviderName(text(provider.getText()));
            updated.setAccountType(text(type.getText()));
            updated.setAccountNumber(text(accountNumber.getText()));
            updated.setNotes(text(notes.getText()));
            return updated;
        });
        return dialog.showAndWait();
    }

    private void addRow(GridPane grid, int row, String label, TextField field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    private TextField field(String value) {
        TextField field = new TextField(text(value));
        field.setPrefWidth(340);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
