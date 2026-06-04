package com.silveira.accounting.ui.bank;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BankAccountDetailPageView {
    public VBox build(String title, Node backButton, Node actions, Node reviewNote, Node totals, Node monthlyCards,
                      Node tableActions, TableView<?> table) {
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(420);
        table.setPrefHeight(520);

        Label heading = new Label(title);
        heading.getStyleClass().add("heading");

        VBox page = new VBox(18);
        page.getChildren().addAll(heading, backButton, actions, reviewNote, totals, monthlyCards, tableActions, table);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        return page;
    }
}
