package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.repositories.BankAccountRepository;

public class UpdateBankAccountUseCase {
    private final BankAccountRepository repository;

    public UpdateBankAccountUseCase(BankAccountRepository repository) {
        this.repository = repository;
    }

    public void execute(String oldAlias, BankAccount account) {
        repository.update(oldAlias, account);
    }
}
