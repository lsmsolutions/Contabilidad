package com.silveira.accounting.application.card.service;

import com.silveira.accounting.repositories.card.CreditCardStatementFieldReviewRepository;
import java.util.Collection;
import java.util.Map;

public class CardFieldReviewApplicationService {
    private final CreditCardStatementFieldReviewRepository repository;

    public CardFieldReviewApplicationService(CreditCardStatementFieldReviewRepository repository) {
        this.repository = repository;
    }

    public boolean isReviewed(long statementId, String fieldName, boolean defaultReviewed) {
        return repository.isReviewed(statementId, fieldName, defaultReviewed);
    }

    public Map<String, Boolean> findByStatement(long statementId) {
        return repository.findByStatement(statementId);
    }

    public void setReviewed(long statementId, String fieldName, boolean reviewed) {
        repository.setReviewed(statementId, fieldName, reviewed);
    }

    public void setReviewed(long statementId, Collection<String> fieldNames, boolean reviewed) {
        repository.setReviewed(statementId, fieldNames, reviewed);
    }
}
