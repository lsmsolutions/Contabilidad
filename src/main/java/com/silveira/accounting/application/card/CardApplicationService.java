package com.silveira.accounting.application.card;

import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.application.card.service.CardAlertApplicationService;
import com.silveira.accounting.application.card.service.CardFieldReviewApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.repositories.card.CreditCardAccountRepository;
import com.silveira.accounting.repositories.card.CreditCardStatementFieldReviewRepository;
import com.silveira.accounting.repositories.card.CreditCardStatementRepository;
import com.silveira.accounting.repositories.card.CreditCardTransactionRepository;
import com.silveira.accounting.repositories.card.FinancialAlertRepository;

public class CardApplicationService {
    private final CardAccountApplicationService accounts;
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;
    private final CardFieldReviewApplicationService fieldReviews;
    private final CardAlertApplicationService alerts;

    public CardApplicationService(
        CreditCardAccountRepository accounts,
        CreditCardStatementRepository statements,
        CreditCardTransactionRepository transactions,
        CreditCardStatementFieldReviewRepository fieldReviews,
        FinancialAlertRepository alerts
    ) {
        this.accounts = new CardAccountApplicationService(accounts);
        this.statements = new CardStatementApplicationService(statements);
        this.transactions = new CardTransactionApplicationService(transactions);
        this.fieldReviews = new CardFieldReviewApplicationService(fieldReviews);
        this.alerts = new CardAlertApplicationService(alerts);
    }

    public CardAccountApplicationService accounts() {
        return accounts;
    }

    public CardStatementApplicationService statements() {
        return statements;
    }

    public CardTransactionApplicationService transactions() {
        return transactions;
    }

    public CardFieldReviewApplicationService fieldReviews() {
        return fieldReviews;
    }

    public CardAlertApplicationService alerts() {
        return alerts;
    }
}
