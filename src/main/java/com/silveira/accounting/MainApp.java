package com.silveira.accounting;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.ui.AppView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        System.setProperty("pdfbox.fontcache", java.nio.file.Path.of("data").toAbsolutePath().toString());
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initialize();

        AppView appView = new AppView(databaseManager);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1180, bounds.getWidth() * 0.86);
        double height = Math.min(760, bounds.getHeight() * 0.84);
        Scene scene = new Scene(appView.root(), width, height);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        stage.setTitle("Silveira Financial Group - Accounting");
        stage.setResizable(true);
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setOnCloseRequest(event -> {
            if (!appView.confirmNavigation()) {
                event.consume();
            }
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
