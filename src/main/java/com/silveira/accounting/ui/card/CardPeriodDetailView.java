package com.silveira.accounting.ui.card;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class CardPeriodDetailView {
    public View build(
        Node statementCards,
        TableView<?> movements,
        Runnable saveStatements,
        Runnable addMovement,
        Runnable saveMovements
    ) {
        Button saveStatementButton = primaryButton("Guardar resumen");
        saveStatementButton.setOnAction(event -> saveStatements.run());
        Button addMovementButton = primaryButton("Añadir movimiento");
        addMovementButton.setOnAction(event -> addMovement.run());
        Button saveMovementButton = primaryButton("Guardar");
        saveMovementButton.setOnAction(event -> saveMovements.run());

        HBox summaryActions = new HBox(10, saveStatementButton);
        summaryActions.setPadding(new Insets(14, 0, 0, 0));
        HBox movementActions = new HBox(16, addMovementButton, saveMovementButton);
        movementActions.setPadding(new Insets(14, 0, 0, 0));
        VBox summariesTab = new VBox(14, summaryActions, statementCards);
        VBox movementsTab = new VBox(14, movementActions, movements);
        VBox.setVgrow(movements, Priority.ALWAYS);

        TabPane tabs = new TabPane(tab("Resumen", summariesTab), tab("Movimientos", movementsTab));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        Runnable showMovements = () -> {
            tabs.getSelectionModel().select(1);
            movements.requestFocus();
        };
        return new View(tabs, showMovements);
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary");
        button.setPrefWidth(220);
        return button;
    }

    private Tab tab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public record View(TabPane tabs, Runnable showMovements) {
    }
}
