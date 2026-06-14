package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.dto.CardPeriodSummary;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.ui.common.PeriodActionCardView;
import com.silveira.accounting.utils.Money;
import java.util.List;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CardMonthlyCardsView {
    public VBox build(List<CardPeriodSummary> periods, Function<CardPeriodSummary, Node> reviewMark, Actions actions) {
        Label title = new Label("Resumen mensual Tarjeta");
        title.getStyleClass().add("section-title");
        FlowPane cards = new FlowPane(12, 12);
        cards.getStyleClass().add("monthly-card-row");

        for (CardPeriodSummary period : periods) {
            VBox card = new PeriodActionCardView().build(
                period.title(),
                reviewMark.apply(period),
                () -> actions.open(period)
            );
            card.getChildren().get(0).getStyleClass().add("card-period-title-row");
            CreditCardStatement openingStatement = period.openingStatement();
            CreditCardStatement closingStatement = period.closingStatement();
            if (openingStatement != null) {
                addLine(card, "Saldo inicial: " + Money.format(openingStatement.getPreviousBalance()), null);
            }
            addLine(card, "Pagos al banco: " + Money.format(period.payments()), null);
            addLine(card, "Compras: " + Money.format(period.purchases()), null);
            addLine(card, "Intereses: " + Money.format(period.interest()), null);
            if (closingStatement != null) {
                addLine(card, "Saldo usado: " + Money.format(closingStatement.getNewBalance()), null);
                addLine(card, "L\u00edmite de cr\u00e9dito: " + Money.format(closingStatement.getCreditLimit()), null);
                addLine(card, "Cr\u00e9dito disponible: " + Money.format(closingStatement.getAvailableCredit()), "monthly-card-value-strong");
            }

            Button editPeriod = new Button("Editar datos");
            editPeriod.setOnAction(event -> {
                event.consume();
                actions.edit(period);
            });
            Button deletePeriod = new Button("Eliminar");
            deletePeriod.getStyleClass().add("danger-button");
            deletePeriod.setOnAction(event -> {
                event.consume();
                actions.delete(period);
            });
            Button download = actions.downloadButton(period);
            download.setText("Descargar");
            HBox mainActions = new HBox(8, editPeriod, download);
            mainActions.getStyleClass().add("card-monthly-actions");
            HBox deleteActions = new HBox(8, deletePeriod);
            deleteActions.getStyleClass().add("card-monthly-actions");
            deleteActions.getStyleClass().add("card-monthly-delete-actions");
            card.getChildren().addAll(mainActions, deleteActions);
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }

        VBox box = new VBox(10, title, cards);
        box.getStyleClass().add("monthly-section");
        return box;
    }

    private void addLine(VBox card, String text, String valueStyleClass) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (card.getChildren().size() < 2 || !(card.getChildren().get(1) instanceof GridPane grid)) {
            return;
        }
        int separator = text.indexOf(": ");
        int row = grid.getChildren().stream()
            .map(GridPane::getRowIndex)
            .mapToInt(index -> index == null ? 0 : index)
            .max()
            .orElse(-1) + 1;
        if (separator <= 0) {
            Label label = new Label(text);
            label.getStyleClass().add("monthly-card-line");
            grid.add(label, 0, row, 2, 1);
            return;
        }
        Label label = new Label(text.substring(0, separator));
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(text.substring(separator + 2));
        value.getStyleClass().add("monthly-card-value");
        if (valueStyleClass != null && !valueStyleClass.isBlank()) {
            value.getStyleClass().add(valueStyleClass);
        }
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    public interface Actions {
        void open(CardPeriodSummary period);

        void edit(CardPeriodSummary period);

        void delete(CardPeriodSummary period);

        Button downloadButton(CardPeriodSummary period);
    }
}
