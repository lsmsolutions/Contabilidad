package com.silveira.accounting.ui.workspace;

import java.io.InputStream;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class WorkspaceView {
    public VBox build(Runnable toggleMenu, List<LinkGroup> groups) {
        Button menu = new Button("\u2630");
        menu.getStyleClass().add("workspace-menu-button");
        menu.setOnAction(event -> toggleMenu.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(menu, spacer);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox logo = new VBox(logoNode());
        logo.getStyleClass().add("workspace-logo-wrap");
        logo.setAlignment(Pos.CENTER);

        HBox linkGroups = new HBox(76);
        linkGroups.setAlignment(Pos.TOP_CENTER);
        for (LinkGroup group : groups) {
            linkGroups.getChildren().add(group(group));
        }

        VBox page = new VBox(34, top, logo, linkGroups);
        page.getStyleClass().addAll("workspace-page", "dark-hub-page");
        page.setPadding(new Insets(28, 54, 54, 54));
        page.setAlignment(Pos.TOP_CENTER);
        return page;
    }

    private VBox group(LinkGroup group) {
        Label title = new Label(group.title());
        title.getStyleClass().add("workspace-family-title");

        VBox links = new VBox(10);
        links.setAlignment(Pos.CENTER);
        for (WorkspaceLink link : group.links()) {
            Button button = new Button(link.label());
            button.getStyleClass().add("workspace-link");
            button.setOnAction(event -> link.action().run());
            links.getChildren().add(button);
        }

        VBox box = new VBox(12, title, links);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("workspace-family");
        return box;
    }

    private javafx.scene.Node logoNode() {
        try (InputStream stream = getClass().getResourceAsStream("/img/logo.png")) {
            if (stream == null) {
                Label fallback = new Label("Silveira Financial Group");
                fallback.getStyleClass().add("workspace-logo-fallback");
                return fallback;
            }
            ImageView logo = new ImageView(new Image(stream));
            logo.setFitWidth(620);
            logo.setPreserveRatio(true);
            return logo;
        } catch (Exception exception) {
            Label fallback = new Label("Silveira Financial Group");
            fallback.getStyleClass().add("workspace-logo-fallback");
            return fallback;
        }
    }

    public record LinkGroup(String title, List<WorkspaceLink> links) {
    }

    public record WorkspaceLink(String label, Runnable action) {
    }
}
