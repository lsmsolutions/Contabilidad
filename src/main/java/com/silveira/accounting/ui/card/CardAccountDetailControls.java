package com.silveira.accounting.ui.card;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CardAccountDetailControls {
    private final ComboBox<Integer> year;
    private final ComboBox<Integer> month;
    private final Button filter;
    private final Button importPdf;
    private final Button analysis;
    private final Button addStatement;
    private final VBox actions;

    public CardAccountDetailControls(Integer selectedYear, Integer selectedMonth) {
        year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(selectedYear);
        year.setPromptText("Ano");
        year.getStyleClass().add("compact-combo");

        month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(selectedMonth);
        month.setPromptText("Mes");
        month.getStyleClass().add("compact-combo");

        filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");

        importPdf = new Button("Importar PDF");
        importPdf.getStyleClass().add("primary");

        analysis = new Button("Ver analisis");
        addStatement = new Button("Anadir resumen");

        actions = actionHeader(
            new HBox(10, new Label("Ano"), year, new Label("Mes"), month, filter),
            new HBox(10, importPdf, analysis, addStatement)
        );
    }

    private VBox actionHeader(Node... rows) {
        VBox header = new VBox(10);
        header.getStyleClass().add("action-header");
        for (Node row : rows) {
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

    public Button filter() {
        return filter;
    }

    public Button importPdf() {
        return importPdf;
    }

    public Button analysis() {
        return analysis;
    }

    public Button addStatement() {
        return addStatement;
    }

    public VBox actions() {
        return actions;
    }
}
