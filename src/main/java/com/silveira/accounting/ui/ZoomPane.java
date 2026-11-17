package com.silveira.accounting.ui;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

/**
 * Lays out the application at the inverse of the selected scale and then
 * scales it to the viewport. This gives browser-like zoom without changing
 * any view data or business logic.
 */
public final class ZoomPane extends Pane {
    private final Node content;
    private final Scale scaleTransform = new Scale(1, 1, 0, 0);
    private double zoom = 1;

    public ZoomPane(Node content) {
        this.content = content;
        content.getTransforms().add(scaleTransform);
        getChildren().add(content);
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
        scaleTransform.setX(zoom);
        scaleTransform.setY(zoom);
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double logicalWidth = getWidth() / zoom;
        double logicalHeight = getHeight() / zoom;
        if (content.isResizable()) {
            content.resizeRelocate(0, 0, logicalWidth, logicalHeight);
        } else {
            content.relocate(0, 0);
        }
    }
}
