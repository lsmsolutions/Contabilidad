package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardReviewApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class CardStatementCardsWorkflow {
    private final CardReviewApplicationService reviews;
    private final CardTransactionApplicationService transactions;
    private final CardEditWorkflow edits;
    private final Config config;

    public CardStatementCardsWorkflow(
        CardReviewApplicationService reviews,
        CardTransactionApplicationService transactions,
        CardEditWorkflow edits,
        Config config
    ) {
        this.reviews = reviews;
        this.transactions = transactions;
        this.edits = edits;
        this.config = config;
    }

    public void refresh(TableView<CreditCardStatement> table, VBox cards, Runnable refreshTotals) {
        cards.getChildren().clear();
        for (CreditCardStatement statement : table.getItems()) {
            cards.getChildren().add(horizontalScroll(build(statement, table, cards, refreshTotals)));
        }
    }

    private VBox build(
        CreditCardStatement statement,
        TableView<CreditCardStatement> table,
        VBox cards,
        Runnable refreshTotals
    ) {
        return new CardStatementCardCoordinator().build(statement, new CardStatementCardCoordinator.Actions() {
            @Override
            public boolean isFieldReviewed(CreditCardStatement current, String fieldName, boolean defaultReviewed) {
                return reviews.isFieldReviewed(current, fieldName, defaultReviewed);
            }

            @Override
            public void updateFieldReview(
                CreditCardStatement current,
                String fieldName,
                boolean reviewed,
                List<String> fieldKeys
            ) {
                reviews.updateField(current, fieldName, reviewed, fieldKeys);
            }

            @Override
            public void updateAllFieldReviews(
                CreditCardStatement current,
                List<String> fieldKeys,
                boolean reviewed
            ) {
                reviews.updateAllFields(current, fieldKeys, reviewed);
            }

            @Override
            public void updateMovementReview(CreditCardTransaction transaction, boolean reviewed) {
                reviews.updateMovement(transaction, reviewed);
            }

            @Override
            public List<CreditCardTransaction> transactionsFor(CreditCardStatement current) {
                return transactions.findByStatement(current);
            }

            @Override
            public void edit(CreditCardStatement current) {
                edits.showPeriodDialog(List.of(current), this::refreshAll);
            }

            @Override
            public void select(CreditCardStatement current) {
                table.getSelectionModel().select(current);
            }

            @Override
            public String title(CreditCardStatement current) {
                return config.statementTitle().apply(current);
            }

            @Override
            public void showMovements() {
                config.showMovements().get().run();
            }

            @Override
            public void refreshAll() {
                table.refresh();
                refreshTotals.run();
                refresh(table, cards, refreshTotals);
            }

            @Override
            public void refreshTableAndTotals() {
                table.refresh();
                refreshTotals.run();
            }

            @Override
            public void refreshTotalsThenTable() {
                refreshTotals.run();
                table.refresh();
            }

            @Override
            public void refreshTotalsAndCards() {
                refreshTotals.run();
                refresh(table, cards, refreshTotals);
            }
        });
    }

    private ScrollPane horizontalScroll(Node statementCard) {
        ScrollPane scroll = new ScrollPane(statementCard);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setMaxWidth(Double.MAX_VALUE);
        return scroll;
    }

    public record Config(
        Function<CreditCardStatement, String> statementTitle,
        Supplier<Runnable> showMovements
    ) {}
}
