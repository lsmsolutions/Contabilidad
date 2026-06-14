package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.dto.CardAnalysisSummary;
import com.silveira.accounting.utils.Money;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CardAnalysisView {
    public VBox build(String alias, CardAnalysisSummary summary, Runnable backAction) {
        Label heading = new Label("Analisis de tarjeta - " + alias);
        heading.getStyleClass().add("heading");
        Button back = new Button("\u2190 Volver al detalle");
        back.getStyleClass().add("back-button");
        back.setOnAction(event -> backAction.run());
        HBox totals = new HBox(12,
            total("Deuda cierre", Money.format(summary.closingDebt()), "expense-total"),
            total("Pagos", Money.format(summary.payments()), "income-total"),
            total("Compras", Money.format(summary.purchases()), "expense-total"),
            total("Intereses", Money.format(summary.interest()), "urgent-total")
        );
        totals.getStyleClass().add("totals-panel");
        VBox page = new VBox(18, heading, back, totals);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        return page;
    }

    private VBox total(String title, String value, String styleClass) {
        Label label = new Label(title);
        label.getStyleClass().add("total-label");
        Label amount = new Label(value);
        amount.getStyleClass().add("total-value");
        VBox card = new VBox(6, label, amount);
        card.getStyleClass().addAll("total-card", styleClass);
        return card;
    }
}
