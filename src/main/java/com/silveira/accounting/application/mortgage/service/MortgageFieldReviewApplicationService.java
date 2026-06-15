package com.silveira.accounting.application.mortgage.service;

import com.silveira.accounting.repositories.mortgage.MortgageStatementFieldReviewRepository;
import java.util.Collection;

public class MortgageFieldReviewApplicationService {
    private final MortgageStatementFieldReviewRepository repository;

    public MortgageFieldReviewApplicationService(MortgageStatementFieldReviewRepository repository) {
        this.repository = repository;
    }

    public boolean isReviewed(long statementId, String fieldName, boolean defaultReviewed) {
        return repository.isReviewed(statementId, fieldName, defaultReviewed);
    }

    public boolean hasReviews(long statementId) {
        return repository.hasReviews(statementId);
    }

    public void setReviewed(long statementId, String fieldName, boolean reviewed) {
        repository.setReviewed(statementId, fieldName, reviewed);
    }

    public void setReviewed(long statementId, Collection<String> fieldNames, boolean reviewed) {
        repository.setReviewed(statementId, fieldNames, reviewed);
    }
}
