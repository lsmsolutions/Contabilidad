package com.silveira.accounting;

import com.silveira.accounting.composition.ApplicationComposition;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.ui.AppView;
import com.silveira.accounting.ui.ZoomPane;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final double MIN_ZOOM = 0.80;
    private static final double MAX_ZOOM = 1.40;
    private static final double ZOOM_STEP = 0.10;

    @Override
    public void start(Stage stage) {
        System.setProperty("pdfbox.fontcache", java.nio.file.Path.of("data").toAbsolutePath().toString());
        DatabaseManager databaseManager = new DatabaseManager();
        databaseManager.initialize();

        AppView appView = new ApplicationComposition(databaseManager).appView();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1180, bounds.getWidth() * 0.86);
        double height = Math.min(760, bounds.getHeight() * 0.84);
        ZoomPane zoomPane = new ZoomPane(appView.root());
        Scene scene = new Scene(zoomPane, width, height);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
        installZoomShortcuts(scene, zoomPane);

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

    private void installZoomShortcuts(Scene scene, ZoomPane zoomPane) {
        double[] zoom = {1.0};
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!event.isControlDown()) {
                return;
            }
            KeyCode code = event.getCode();
            if (code == KeyCode.ADD || code == KeyCode.PLUS || code == KeyCode.EQUALS) {
                zoom[0] = Math.min(MAX_ZOOM, zoom[0] + ZOOM_STEP);
            } else if (code == KeyCode.SUBTRACT || code == KeyCode.MINUS) {
                zoom[0] = Math.max(MIN_ZOOM, zoom[0] - ZOOM_STEP);
            } else if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
                zoom[0] = 1.0;
            } else {
                return;
            }
            zoomPane.setZoom(zoom[0]);
            event.consume();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
