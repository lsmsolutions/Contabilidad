package com.silveira.accounting.application.card.service;

import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.repositories.card.CreditCardAccountRepository;
import java.util.List;

public class CardAccountApplicationService {
    private final CreditCardAccountRepository repository;

    public CardAccountApplicationService(CreditCardAccountRepository repository) {
        this.repository = repository;
    }

    public List<CreditCardAccount> findAll() {
        return repository.findAll();
    }

    public void save(CreditCardAccount account) {
        repository.save(account);
    }

    public void update(String oldAlias, CreditCardAccount account) {
        repository.update(oldAlias, account);
    }

    public void delete(String alias) {
        repository.delete(alias);
    }
}
