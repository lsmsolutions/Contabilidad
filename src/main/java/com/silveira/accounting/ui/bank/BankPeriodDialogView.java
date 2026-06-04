package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.utils.Money;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Optional;

public class BankPeriodDialogView {
    public Optional<EditedPeriod> show(BankPeriodSummary summary) {
        BankStatementPeriod period = summary.statementPeriod();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar periodo bancario");

        DatePicker start = new DatePicker(period.periodStart());
        DatePicker end = new DatePicker(period.periodEnd());
        TextField opening = new TextField(Money.format(period.openingBalance()));
        TextField ending = new TextField(period.hasStatementEndingBalance() ? Money.format(period.statementEndingBalance()) : "");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Desde"), start);
        form.addRow(1, new Label("Hasta"), end);
        form.addRow(2, new Label("Saldo inicial"), opening);
        form.addRow(3, new Label("Saldo final PDF"), ending);

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        return dialog.showAndWait()
            .filter(ButtonType.OK::equals)
            .map(result -> new EditedPeriod(
                period.accountAlias(),
                period.sourcePdf(),
                start.getValue(),
                end.getValue(),
                Money.parse(opening.getText()),
                ending.getText().isBlank() ? 0 : Money.parse(ending.getText())
            ));
    }

    public record EditedPeriod(
        String accountAlias,
        String sourcePdf,
        LocalDate start,
        LocalDate end,
        double openingBalance,
        double statementEndingBalance
    ) {}
}
