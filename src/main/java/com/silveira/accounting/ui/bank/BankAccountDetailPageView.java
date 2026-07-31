package com.silveira.accounting.ui.bank;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BankAccountDetailPageView {
    public VBox build(String title, Node backButton, Node actions, Node reviewNote, Node totals, Node monthlyCards) {
        Label heading = new Label(title);
        heading.getStyleClass().add("heading");

        VBox stickyHeader = new VBox(18, heading, backButton);
        VBox scrollContent = new VBox(18, actions, reviewNote, totals, monthlyCards);
        ScrollPane scroll = new ScrollPane(scrollContent);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox page = new VBox(18);
        page.getChildren().addAll(stickyHeader, scroll);
        page.setPadding(new Insets(28));
        page.getStyleClass().addAll("page", "self-scroll-page");
        return page;
    }
}
