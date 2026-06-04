package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.util.Optional;

public class BankDeletePeriodConfirmationView {
    public boolean confirm(BankPeriodSummary selectedPeriod, String periodTitle) {
        int transactionCount = selectedPeriod.transactions().size();
        String message = transactionCount == 0
            ? "Se eliminará el periodo " + periodTitle + "."
            : "Se eliminará el periodo " + periodTitle + " y " + transactionCount + " movimientos asociados.";

        ButtonType delete = new ButtonType("Eliminar periodo");
        Alert alert = new Alert(
            Alert.AlertType.WARNING,
            message + "\n\nEsta acción no se puede deshacer.",
            delete,
            ButtonType.CANCEL
        );
        alert.setTitle("Eliminar periodo");
        alert.setHeaderText("Eliminar periodo");
        alert.getDialogPane().setMinWidth(620);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == delete;
    }
}
