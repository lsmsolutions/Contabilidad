package com.silveira.accounting.application.card.usecase;

import com.silveira.accounting.application.card.service.CardAlertApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.application.card.service.CreditCardAnalysisService;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.parsers.CreditCardStatementParser;

public class SaveImportedCardStatementUseCase {
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;
    private final CardAlertApplicationService alerts;
    private final CreditCardAnalysisService analysis;

    public SaveImportedCardStatementUseCase(
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions,
        CardAlertApplicationService alerts,
        CreditCardAnalysisService analysis
    ) {
        this.statements = statements;
        this.transactions = transactions;
        this.alerts = alerts;
        this.analysis = analysis;
    }

    public long execute(String accountAlias, CreditCardStatementParser.ParsedCreditCardStatement parsed) {
        CreditCardStatement statement = parsed.statement();
        statement.setAccountAlias(accountAlias);
        long statementId = statements.save(statement);
        transactions.saveAll(statementId, parsed.transactions());
        alerts.saveAll(statementId, analysis.analyze(statement).alerts());
        return statementId;
    }
}
