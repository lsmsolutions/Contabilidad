package com.silveira.accounting.ui.bank;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BankAccountDetailControls {
    private final ComboBox<Integer> year;
    private final ComboBox<Integer> month;
    private final TextField provider;
    private final ComboBox<String> type;
    private final Button filter;
    private final Button importPdf;
    private final Button manualPeriod;
    private final Button deletePeriod;
    private final VBox actions;

    public BankAccountDetailControls(Integer selectedYear, Integer selectedMonth) {
        year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(selectedYear);
        year.setPromptText("Año");
        year.getStyleClass().add("compact-combo");

        month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(selectedMonth);
        month.setPromptText("Mes");
        month.getStyleClass().add("compact-combo");

        provider = new TextField();
        provider.setPromptText("Proveedor o concepto");
        provider.getStyleClass().add("concept-field");

        type = new ComboBox<>(FXCollections.observableArrayList("", "Deposito", "Retiro electronico", "Pago con tarjeta", "Fee", "Gasto"));
        type.setPromptText("Tipo");
        type.getStyleClass().add("type-combo");

        filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");

        importPdf = new Button("Importar PDF");
        importPdf.getStyleClass().add("primary");

        manualPeriod = new Button("Periodo manual");
        deletePeriod = new Button("Eliminar");

        actions = actionHeader(
            new HBox(10, new Label("Año"), year, new Label("Mes"), month, provider, type, filter),
            new HBox(10, importPdf, manualPeriod, deletePeriod)
        );
    }

    private VBox actionHeader(javafx.scene.Node... rows) {
        VBox header = new VBox(10);
        header.getStyleClass().add("action-header");
        for (javafx.scene.Node row : rows) {
            if (row instanceof HBox hBox) {
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getStyleClass().add("action-row");
            }
            header.getChildren().add(row);
        }
        return header;
    }

    public ComboBox<Integer> year() {
        return year;
    }

    public ComboBox<Integer> month() {
        return month;
    }

    public TextField provider() {
        return provider;
    }

    public ComboBox<String> type() {
        return type;
    }

    public Button filter() {
        return filter;
    }

    public Button importPdf() {
        return importPdf;
    }

    public Button manualPeriod() {
        return manualPeriod;
    }

    public Button deletePeriod() {
        return deletePeriod;
    }

    public VBox actions() {
        return actions;
    }
}
