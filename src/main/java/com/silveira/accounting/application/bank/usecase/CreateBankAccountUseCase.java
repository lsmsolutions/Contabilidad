package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.repositories.BankAccountRepository;

public class CreateBankAccountUseCase {
    private final BankAccountRepository repository;

    public CreateBankAccountUseCase(BankAccountRepository repository) {
        this.repository = repository;
    }

    public void execute(BankAccount account) {
        repository.save(account);
    }
}
