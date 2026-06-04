package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.repositories.BankAccountRepository;

public class DeleteBankAccountUseCase {
    private final BankAccountRepository repository;

    public DeleteBankAccountUseCase(BankAccountRepository repository) {
        this.repository = repository;
    }

    public void execute(String alias) {
        repository.delete(alias);
    }
}
