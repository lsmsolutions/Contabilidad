package com.silveira.accounting.ui.common;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PeriodActionCardView {
    public VBox build(String title, Node reviewMark, Runnable action) {
        Label heading = new Label(title);
        heading.getStyleClass().add("monthly-card-title");

        HBox titleRow = new HBox(heading, reviewMark);
        titleRow.getStyleClass().add("bank-period-title-row");
        HBox.setHgrow(heading, Priority.ALWAYS);

        GridPane lines = new GridPane();
        lines.getStyleClass().add("monthly-card-grid");

        VBox box = new VBox(0, titleRow, lines);
        box.getStyleClass().add("monthly-card");
        box.setOnMouseClicked(event -> action.run());
        return box;
    }
}
