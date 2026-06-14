package com.silveira.accounting.application.card.service;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import java.util.List;

public class CardReviewApplicationService {
    private final CardFieldReviewApplicationService fieldReviews;
    private final CardStatementApplicationService statements;
    private final CardTransactionApplicationService transactions;

    public CardReviewApplicationService(
        CardFieldReviewApplicationService fieldReviews,
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions
    ) {
        this.fieldReviews = fieldReviews;
        this.statements = statements;
        this.transactions = transactions;
    }

    public boolean isFieldReviewed(CreditCardStatement statement, String fieldName, boolean defaultReviewed) {
        return fieldReviews.isReviewed(statement.getId(), fieldName, defaultReviewed);
    }

    public void updateField(CreditCardStatement statement, String fieldName, boolean reviewed, List<String> fieldKeys) {
        fieldReviews.setReviewed(statement.getId(), fieldName, reviewed);
        updateStatement(statement, allFieldsReviewed(statement, fieldKeys));
    }

    public void updateAllFields(CreditCardStatement statement, List<String> fieldKeys, boolean reviewed) {
        fieldReviews.setReviewed(statement.getId(), fieldKeys, reviewed);
        updateStatement(statement, reviewed);
    }

    public void updateStatement(CreditCardStatement statement, boolean reviewed) {
        statement.setPendingReview(!reviewed);
        statement.setReviewRequired(!reviewed);
        if (reviewed && (statement.getReviewNotes() == null || statement.getReviewNotes().isBlank()
            || statement.getReviewNotes().startsWith("Revisar"))) {
            statement.setReviewNotes("Revisado");
        } else if (!reviewed && "Revisado".equalsIgnoreCase(statement.getReviewNotes())) {
            statement.setReviewNotes("Revisar contra el PDF original");
        }
        if (statement.getId() > 0) {
            statements.updateRecord(statement);
        }
    }

    public void updateMovement(CreditCardTransaction movement, boolean reviewed) {
        movement.setPendingReview(!reviewed);
        movement.setReviewRequired(!reviewed);
        if (reviewed && (movement.getReviewNotes() == null || movement.getReviewNotes().isBlank()
            || movement.getReviewNotes().startsWith("Revisar"))) {
            movement.setReviewNotes("Revisado");
        }
        if (movement.getId() > 0) {
            transactions.update(movement);
        }
    }

    private boolean allFieldsReviewed(CreditCardStatement statement, List<String> fieldKeys) {
        boolean defaultReviewed = !statement.isPendingReview();
        return fieldKeys.stream()
            .allMatch(fieldName -> fieldReviews.isReviewed(statement.getId(), fieldName, defaultReviewed));
    }
}
