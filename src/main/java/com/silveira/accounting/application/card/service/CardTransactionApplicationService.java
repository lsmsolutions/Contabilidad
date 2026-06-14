package com.silveira.accounting.application.card.service;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.repositories.card.CreditCardTransactionRepository;
import java.time.LocalDate;
import java.util.List;

public class CardTransactionApplicationService {
    private final CreditCardTransactionRepository repository;

    public CardTransactionApplicationService(CreditCardTransactionRepository repository) {
        this.repository = repository;
    }

    public void saveAll(long statementId, List<CreditCardTransaction> transactions) {
        repository.saveAll(statementId, transactions);
    }

    public long save(long statementId, CreditCardTransaction transaction) {
        return repository.save(statementId, transaction);
    }

    public CreditCardTransaction createManual(long statementId, LocalDate today) {
        CreditCardTransaction movement = new CreditCardTransaction(
            0,
            statementId,
            today,
            today,
            "Movimiento manual",
            0,
            "gasto",
            "manual"
        );
        movement.setPendingReview(true);
        movement.setReviewRequired(true);
        return movement;
    }

    public void saveVisible(long defaultStatementId, List<CreditCardTransaction> movements) {
        for (CreditCardTransaction movement : movements) {
            if (movement.getStatementId() <= 0 && defaultStatementId > 0) {
                movement.setStatementId(defaultStatementId);
            }
            if (movement.getId() > 0) {
                repository.update(movement);
            } else if (movement.getStatementId() > 0) {
                movement.setId(repository.save(movement.getStatementId(), movement));
            }
        }
    }

    public List<CreditCardTransaction> findByAccount(String alias, Integer year, Integer month) {
        return repository.findByAccount(alias, year, month);
    }

    public List<CreditCardTransaction> findByStatement(CreditCardStatement statement) {
        if (statement.getId() <= 0 || statement.getStatementEndDate() == null) {
            return List.of();
        }
        return repository.findByAccount(
                statement.getAccountAlias(),
                statement.getStatementEndDate().getYear(),
                statement.getStatementEndDate().getMonthValue()
            ).stream()
            .filter(transaction -> transaction.getStatementId() == statement.getId())
            .toList();
    }

    public void update(CreditCardTransaction transaction) {
        repository.update(transaction);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
