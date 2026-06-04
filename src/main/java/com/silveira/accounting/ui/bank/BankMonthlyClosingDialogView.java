package com.silveira.accounting.ui.bank;

import com.silveira.accounting.models.bank.BankMonthlyClosing;
import com.silveira.accounting.utils.Money;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Optional;

public class BankMonthlyClosingDialogView {
    public Optional<EditedClosing> show(String accountAlias, int year, int month, double netMovement, BankMonthlyClosing current) {
        TextField opening = new TextField(String.format(Locale.US, "%.2f", current.openingBalance()));
        TextField statementEnding = new TextField(String.format(Locale.US, "%.2f", current.statementEndingBalance()));
        double calculated = current.calculatedEndingBalance(netMovement);

        Label calculatedLabel = new Label("Saldo final calculado actual: " + Money.format(calculated));
        Label help = helperNote("Introduce el saldo inicial y el saldo final que aparece en el PDF del banco. No se modifican las transacciones ya revisadas.");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("Cuenta"), 0, 0);
        form.add(new Label(accountAlias == null || accountAlias.isBlank() ? "sin_cuenta" : accountAlias), 1, 0);
        form.add(new Label("Mes"), 0, 1);
        form.add(new Label(monthName(month) + " " + year), 1, 1);
        form.add(new Label("Saldo inicial"), 0, 2);
        form.add(opening, 1, 2);
        form.add(new Label("Saldo final PDF"), 0, 3);
        form.add(statementEnding, 1, 3);
        form.add(calculatedLabel, 1, 4);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Editar saldos bancarios");
        dialog.setHeaderText("Editar saldos del mes");
        dialog.getDialogPane().setContent(new VBox(12, help, form));
        dialog.getDialogPane().setMinWidth(520);

        return dialog.showAndWait()
            .filter(ButtonType.OK::equals)
            .map(result -> new EditedClosing(
                Money.parse(opening.getText()),
                Money.parse(statementEnding.getText())
            ));
    }

    private Label helperNote(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-subtitle");
        label.setWrapText(true);
        return label;
    }

    private String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> "Mes " + month;
        };
    }

    public record EditedClosing(
        double openingBalance,
        double statementEndingBalance
    ) {}
}
