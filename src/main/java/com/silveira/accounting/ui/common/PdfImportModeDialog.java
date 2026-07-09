package com.silveira.accounting.ui.common;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public class PdfImportModeDialog {
    public enum Mode {
        NORMAL,
        AI
    }

    public Optional<Mode> show(Window owner) {
        ButtonType normal = new ButtonType("Read normally", ButtonBar.ButtonData.OK_DONE);
        ButtonType ai = new ButtonType("Read with AI", ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Choose how to read this PDF.",
            normal,
            ai,
            cancel
        );
        alert.setTitle("Import PDF");
        alert.setHeaderText("Read PDF");
        if (owner != null) {
            alert.initOwner(owner);
        }
        return alert.showAndWait()
            .flatMap(selected -> {
                if (selected == normal) {
                    return Optional.of(Mode.NORMAL);
                }
                if (selected == ai) {
                    return Optional.of(Mode.AI);
                }
                return Optional.empty();
            });
    }
}
