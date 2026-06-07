package com.silveira.accounting.ui.bank;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class BankPeriodActionControls {
    private final Button pending;
    private final Button showAll;
    private final Button addRecord;
    private final Button save;
    private final Label selectedPeriod;
    private final HBox row;

    public BankPeriodActionControls() {
        selectedPeriod = new Label("Selecciona una card de periodo para añadir registros dentro de ese extracto.");
        selectedPeriod.getStyleClass().add("section-subtitle");
        selectedPeriod.setWrapText(true);

        addRecord = new Button("Añadir registro");
        addRecord.getStyleClass().add("primary");
        save = new Button("Guardar");
        save.getStyleClass().add("primary");
        pending = new Button("Pendientes por revisar");
        showAll = new Button("Mostrar todo");

        row = new HBox(10, pending, showAll, addRecord, save, selectedPeriod);
        row.setAlignment(Pos.CENTER_LEFT);
    }

    public Button pending() {
        return pending;
    }

    public Button showAll() {
        return showAll;
    }

    public Button addRecord() {
        return addRecord;
    }

    public Button save() {
        return save;
    }

    public Label selectedPeriod() {
        return selectedPeriod;
    }

    public HBox row() {
        return row;
    }
}
