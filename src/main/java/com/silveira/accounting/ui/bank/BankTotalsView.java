package com.silveira.accounting.ui.bank;

import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.utils.Money;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class BankTotalsView {
    public List<Node> build(SourceTotals totals, double openingBalance) {
        return build(totals, openingBalance, "Dep\u00f3sitos", "Salidas", "Movimiento del periodo");
    }

    public List<Node> buildAccumulated(SourceTotals totals, double openingBalance) {
        return build(totals, openingBalance, "Dep\u00f3sitos acumulados", "Salidas acumuladas", "Movimiento acumulado");
    }

    private List<Node> build(SourceTotals totals, double openingBalance, String incomeTitle, String expenseTitle, String chartTitle) {
        List<Node> nodes = new ArrayList<>(List.of(
            miniTotal("Saldo inicial", Money.format(openingBalance), "pending-total"),
            miniTotal(incomeTitle, Money.format(totals.income()), "income-total"),
            miniTotal(expenseTitle, Money.format(Math.abs(totals.expenses())), "expense-total")
        ));
        nodes.add(flowChart(totals, chartTitle));
        return nodes;
    }

    private VBox miniTotal(String title, String value, String styleClass) {
        Label label = new Label(title);
        label.getStyleClass().add("mini-total-title");
        Label amount = new Label(value);
        amount.getStyleClass().add("mini-total-value");
        VBox box = new VBox(4, label, amount);
        box.getStyleClass().addAll("mini-total", styleClass);
        return box;
    }

    private VBox flowChart(SourceTotals totals, String title) {
        double deposits = Math.max(0, totals.income());
        double withdrawals = Math.abs(Math.min(0, totals.expenses()));
        double max = Math.max(Math.max(deposits, withdrawals), 1.0);
        VBox chart = new VBox(8,
            flowHeader(title),
            flowBar("Dep\u00f3sitos", deposits, max, "bank-flow-income-fill"),
            flowBar("Salidas", withdrawals, max, "bank-flow-expense-fill")
        );
        chart.getStyleClass().add("bank-flow-chart");
        HBox.setHgrow(chart, Priority.ALWAYS);
        return chart;
    }

    private Label flowHeader(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("bank-flow-title");
        return label;
    }

    private HBox flowBar(String title, double amount, double max, String fillStyle) {
        Label name = new Label(title);
        name.getStyleClass().add("bank-flow-label");
        StackPane track = new StackPane();
        track.getStyleClass().add("bank-flow-track");
        Region fill = new Region();
        fill.getStyleClass().add(fillStyle);
        double width = 220 * (amount / max);
        fill.setMinWidth(width);
        fill.setPrefWidth(width);
        fill.setMaxWidth(width);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        HBox.setHgrow(track, Priority.ALWAYS);
        Label value = new Label(Money.format(amount));
        value.getStyleClass().add("bank-flow-value");
        HBox row = new HBox(10, name, track, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
