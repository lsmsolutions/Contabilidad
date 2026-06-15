package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.dto.CardActivityTotals;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.utils.Money;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CardTotalsView {
    private final CardStatementApplicationService statements;

    public CardTotalsView(CardStatementApplicationService statements) {
        this.statements = statements;
    }

    public List<Node> accumulatedNodes(String alias, Integer year, Integer throughMonth) {
        return activityNodes(statements.accumulatedActivityTotals(alias, year, throughMonth));
    }

    public List<Node> periodActivityNodes(List<CreditCardStatement> statementList) {
        return activityNodes(statements.activityTotals(statementList));
    }

    private List<Node> activityNodes(CardActivityTotals totals) {
        return List.of(
            miniTotal("Pagos (al Banco)", Money.format(totals.payments()), "income-total"),
            miniTotal("Compras", Money.format(totals.purchases()), "expense-total"),
            miniTotal("Intereses", Money.format(totals.interest()), "urgent-total")
        );
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
}
