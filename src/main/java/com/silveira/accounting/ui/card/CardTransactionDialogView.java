package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class CardTransactionDialogView {
    public Optional<FormData> show(CreditCardStatement statement) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("A\u00f1adir movimiento");
        DatePicker transactionDate = new DatePicker(
            statement.getStatementEndDate() == null ? LocalDate.now() : statement.getStatementEndDate()
        );
        DatePicker postDate = new DatePicker(transactionDate.getValue());
        TextField description = new TextField();
        description.setPromptText("Description");
        TextField amount = new TextField(String.format(Locale.US, "%.2f", 0.0));
        TextField type = new TextField("gasto");
        TextField category = new TextField("manual");
        CheckBox reviewed = new CheckBox("Revisado");
        TextArea notes = new TextArea("A\u00f1adido manualmente");
        notes.setPrefRowCount(2);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Date"), transactionDate);
        form.addRow(1, new Label("Posted Date"), postDate);
        form.addRow(2, new Label("Description"), description);
        form.addRow(3, new Label("Amount"), amount);
        form.addRow(4, new Label("Type"), type);
        form.addRow(5, new Label("Category"), category);
        form.addRow(6, new Label("Review"), reviewed);
        form.addRow(7, new Label("Notes"), notes);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        return dialog.showAndWait()
            .filter(ButtonType.OK::equals)
            .map(result -> new FormData(
                transactionDate.getValue(),
                postDate.getValue(),
                description.getText(),
                amount.getText(),
                type.getText(),
                category.getText(),
                reviewed.isSelected(),
                notes.getText()
            ));
    }

    public record FormData(
        LocalDate transactionDate,
        LocalDate postDate,
        String description,
        String amount,
        String type,
        String category,
        boolean reviewed,
        String notes
    ) {
    }
}
