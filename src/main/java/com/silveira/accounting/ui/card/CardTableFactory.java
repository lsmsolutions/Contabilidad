package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardReviewApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import java.time.LocalDate;
import java.util.function.Function;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class CardTableFactory {
    private final CardReviewApplicationService reviews;
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;
    private final Config config;

    public CardTableFactory(
        CardReviewApplicationService reviews,
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions,
        Config config
    ) {
        this.reviews = reviews;
        this.statements = statements;
        this.transactions = transactions;
        this.config = config;
    }

    public TableView<CreditCardStatement> statementTable() {
        return new CardStatementTableView().build(
            reviews::updateStatement,
            statement -> {
                if (statement.getId() > 0) {
                    statements.delete(statement.getId());
                }
            },
            config.parseDate()
        );
    }

    public TableView<CreditCardTransaction> transactionTable() {
        return new CardTransactionTableView().build(
            reviews::updateMovement,
            movement -> {
                if (movement.getId() > 0) {
                    transactions.delete(movement.getId());
                }
            },
            config.parseDate(),
            config.stringCellFactory(),
            config.moneyCellFactory()
        );
    }

    public record Config(
        Function<String, LocalDate> parseDate,
        Callback<TableColumn<CreditCardTransaction, String>, TableCell<CreditCardTransaction, String>> stringCellFactory,
        Callback<TableColumn<CreditCardTransaction, Double>, TableCell<CreditCardTransaction, Double>> moneyCellFactory
    ) {}
}
