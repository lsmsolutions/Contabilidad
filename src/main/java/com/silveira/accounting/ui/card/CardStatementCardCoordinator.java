package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.utils.Money;
import java.util.List;
import java.util.Locale;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class CardStatementCardCoordinator {
    public VBox build(CreditCardStatement statement, Actions actions) {
        if (isBestBuyStatement(statement)) {
            return bestBuyCard(statement, actions);
        }
        if (isDiscoverStatement(statement)) {
            return discoverCard(statement, actions);
        }
        if (isCitiStatement(statement)) {
            return citiCard(statement, actions);
        }
        if (!isCapitalOneStatement(statement)) {
            return genericCard(statement, actions);
        }
        return capitalOneCard(statement, actions);
    }

    private VBox capitalOneCard(CreditCardStatement statement, Actions actions) {
        CreditCardStatementSummaryView summaryView = new CreditCardStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        VBox card = summaryView.build(
            statement,
            fieldName -> actions.isFieldReviewed(statement, fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                actions.updateFieldReview(statement, fieldName, reviewed, fieldKeys);
                actions.refreshAll();
            },
            reviewed -> {
                actions.updateAllFieldReviews(statement, fieldKeys, reviewed);
                actions.refreshAll();
            },
            () -> actions.edit(statement)
        );
        card.setOnMouseClicked(event -> actions.select(statement));
        return card;
    }

    private VBox bestBuyCard(CreditCardStatement statement, Actions actions) {
        BestBuyStatementSummaryView summaryView = new BestBuyStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        List<CreditCardTransaction> transactions = actions.transactionsFor(statement);
        VBox card = summaryView.build(
            statement,
            transactions,
            fieldName -> actions.isFieldReviewed(statement, fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                actions.updateFieldReview(statement, fieldName, reviewed, fieldKeys);
                actions.refreshAll();
            },
            reviewed -> {
                actions.updateAllFieldReviews(statement, fieldKeys, reviewed);
                for (CreditCardTransaction transaction : transactions) {
                    actions.updateMovementReview(transaction, reviewed);
                }
                actions.refreshAll();
            },
            (transaction, reviewed) -> {
                actions.updateMovementReview(transaction, reviewed);
                actions.refreshTotalsAndCards();
            },
            () -> actions.edit(statement)
        );
        card.setOnMouseClicked(event -> actions.select(statement));
        return card;
    }

    private VBox discoverCard(CreditCardStatement statement, Actions actions) {
        DiscoverStatementSummaryView summaryView = new DiscoverStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        List<CreditCardTransaction> transactions = actions.transactionsFor(statement);
        VBox card = summaryView.build(
            statement,
            transactions,
            fieldName -> actions.isFieldReviewed(statement, fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                actions.updateFieldReview(statement, fieldName, reviewed, fieldKeys);
                actions.refreshTableAndTotals();
            },
            reviewed -> {
                actions.updateAllFieldReviews(statement, fieldKeys, reviewed);
                for (CreditCardTransaction transaction : transactions) {
                    actions.updateMovementReview(transaction, reviewed);
                }
                actions.refreshAll();
            },
            (transaction, reviewed) -> {
                actions.updateMovementReview(transaction, reviewed);
                actions.refreshTotalsThenTable();
            },
            actions::showMovements,
            () -> actions.edit(statement)
        );
        card.setOnMouseClicked(event -> actions.select(statement));
        return card;
    }

    private VBox citiCard(CreditCardStatement statement, Actions actions) {
        CitiStatementSummaryView summaryView = new CitiStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        VBox card = summaryView.build(
            statement,
            fieldName -> actions.isFieldReviewed(statement, fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                actions.updateFieldReview(statement, fieldName, reviewed, fieldKeys);
                actions.refreshAll();
            },
            reviewed -> {
                actions.updateAllFieldReviews(statement, fieldKeys, reviewed);
                actions.refreshAll();
            },
            () -> actions.edit(statement)
        );
        card.setOnMouseClicked(event -> actions.select(statement));
        return card;
    }

    private VBox genericCard(CreditCardStatement statement, Actions actions) {
        VBox card = monthlyActionCard(
            actions.title(statement),
            "Saldo anterior: " + Money.format(statement.getPreviousBalance()),
            "Saldo usado: " + Money.format(statement.getNewBalance()),
            "Pago minimo: " + Money.format(statement.getMinimumPaymentDue()),
            "Pendiente revision: " + (statement.isPendingReview() ? "Si" : "No"),
            () -> actions.select(statement)
        );
        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> actions.edit(statement));
        card.getChildren().add(edit);
        card.getStyleClass().add("monthly-card");
        return card;
    }

    private boolean isCapitalOneStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(Locale.ROOT);
        return bank.contains("capital one") || alias.contains("capitalone") || card.contains("capital one");
    }

    private boolean isBestBuyStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(Locale.ROOT);
        return bank.contains("best buy") || alias.contains("bestbuy") || alias.contains("best_buy") || card.contains("best buy");
    }

    private boolean isCitiStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(Locale.ROOT);
        return bank.contains("citi") || alias.contains("citi") || card.contains("citi");
    }

    private boolean isDiscoverStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(Locale.ROOT);
        return bank.contains("discover") || alias.contains("discover") || card.contains("discover");
    }

    private VBox monthlyActionCard(String title, String line1, String line2, String line3, String line4, Runnable action) {
        Label heading = new Label(title);
        heading.getStyleClass().add("monthly-card-title");
        GridPane lines = new GridPane();
        lines.getStyleClass().add("monthly-card-grid");
        int row = 0;
        for (String line : List.of(line1, line2, line3, line4)) {
            if (!line.isBlank()) {
                row = addMonthlyCardGridLine(lines, row, line);
            }
        }
        VBox box = new VBox(0, heading, lines);
        box.setOnMouseClicked(event -> action.run());
        return box;
    }

    private int addMonthlyCardGridLine(GridPane grid, int row, String text) {
        int separator = text.indexOf(": ");
        if (separator <= 0) {
            Label label = new Label(text);
            label.getStyleClass().add("monthly-card-line");
            grid.add(label, 0, row, 2, 1);
            return row + 1;
        }
        Label label = new Label(text.substring(0, separator));
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(text.substring(separator + 2));
        value.getStyleClass().add("monthly-card-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    public interface Actions {
        boolean isFieldReviewed(CreditCardStatement statement, String fieldName, boolean defaultReviewed);

        void updateFieldReview(CreditCardStatement statement, String fieldName, boolean reviewed, List<String> fieldKeys);

        void updateAllFieldReviews(CreditCardStatement statement, List<String> fieldKeys, boolean reviewed);

        void updateMovementReview(CreditCardTransaction transaction, boolean reviewed);

        List<CreditCardTransaction> transactionsFor(CreditCardStatement statement);

        void edit(CreditCardStatement statement);

        void select(CreditCardStatement statement);

        String title(CreditCardStatement statement);

        void showMovements();

        void refreshAll();

        void refreshTableAndTotals();

        void refreshTotalsThenTable();

        void refreshTotalsAndCards();
    }
}
