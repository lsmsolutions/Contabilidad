package com.silveira.accounting.ui.bank;

import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Optional;

public class BankManualPeriodDialogView {
    public Optional<ManualPeriodInput> show(LocalDate initialStart) {
        DatePicker start = new DatePicker(initialStart);
        DatePicker end = new DatePicker(start.getValue().plusMonths(1).minusDays(1));
        TextField opening = new TextField("0.00");
        TextField ending = new TextField("");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Desde"), start);
        form.addRow(1, new Label("Hasta"), end);
        form.addRow(2, new Label("Saldo inicial"), opening);
        form.addRow(3, new Label("Saldo final PDF"), ending);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Periodo manual");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        return dialog.showAndWait()
            .filter(ButtonType.OK::equals)
            .map(result -> new ManualPeriodInput(
                start.getValue(),
                end.getValue(),
                opening.getText(),
                ending.getText()
            ));
    }

    public record ManualPeriodInput(
        LocalDate start,
        LocalDate end,
        String openingBalance,
        String statementEndingBalance
    ) {}
}
