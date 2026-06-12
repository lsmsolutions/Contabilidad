package com.silveira.accounting.application.card.service;

import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.repositories.card.CreditCardTransactionRepository;
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

    public List<CreditCardTransaction> findByAccount(String alias, Integer year, Integer month) {
        return repository.findByAccount(alias, year, month);
    }

    public void update(CreditCardTransaction transaction) {
        repository.update(transaction);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
