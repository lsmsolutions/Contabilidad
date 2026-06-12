package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.repositories.bank.BankAccountRepository;

public class EnsureBankAccountExistsUseCase {
    private final BankAccountRepository repository;

    public EnsureBankAccountExistsUseCase(BankAccountRepository repository) {
        this.repository = repository;
    }

    public void execute(String alias, String notes) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        repository.saveIfMissing(new BankAccount(0, alias, "", "", "", notes));
    }
}
