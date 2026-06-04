package com.silveira.accounting.ui.bank;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BankPendingReviewPageView {
    public Page build(Node backButton, TableView<?> table) {
        table.setMinHeight(560);
        table.setPrefHeight(680);
        VBox.setVgrow(table, Priority.ALWAYS);

        Label heading = new Label("Banco pendientes por revisar");
        heading.getStyleClass().add("heading");

        Label note = new Label("Marca el check Revisado cuando hayas comprobado la línea contra el PDF. Guarda progreso para conservar revisados y pendientes.");
        note.getStyleClass().add("section-subtitle");
        note.setWrapText(true);

        Button save = new Button("Guardar progreso");
        save.getStyleClass().add("primary");

        VBox page = new VBox(18);
        page.getChildren().addAll(heading, backButton, note, table, save);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");

        return new Page(page, save);
    }

    public record Page(VBox node, Button save) {}
}
