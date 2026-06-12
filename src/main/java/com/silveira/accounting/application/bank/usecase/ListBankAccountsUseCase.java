package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.repositories.bank.BankAccountRepository;

import java.util.List;

public class ListBankAccountsUseCase {
    private final BankAccountRepository repository;

    public ListBankAccountsUseCase(BankAccountRepository repository) {
        this.repository = repository;
    }

    public List<BankAccount> execute() {
        return repository.findAll();
    }
}
