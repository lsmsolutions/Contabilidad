package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.repositories.BankAccountRepository;

import java.util.Optional;

public class FindBankAccountUseCase {
    private final BankAccountRepository repository;

    public FindBankAccountUseCase(BankAccountRepository repository) {
        this.repository = repository;
    }

    public Optional<BankAccount> execute(String alias) {
        return repository.findAll().stream()
            .filter(account -> alias != null && alias.equals(account.getAlias()))
            .findFirst();
    }
}
