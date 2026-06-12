package com.silveira.accounting.application.bank.service;

import com.silveira.accounting.application.bank.usecase.CreateBankAccountUseCase;
import com.silveira.accounting.application.bank.usecase.DeleteBankAccountUseCase;
import com.silveira.accounting.application.bank.usecase.EnsureBankAccountExistsUseCase;
import com.silveira.accounting.application.bank.usecase.FindBankAccountUseCase;
import com.silveira.accounting.application.bank.usecase.ListBankAccountsUseCase;
import com.silveira.accounting.application.bank.usecase.UpdateBankAccountUseCase;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.repositories.bank.BankAccountRepository;

import java.util.List;
import java.util.Optional;

public class BankAccountApplicationService {
    private final ListBankAccountsUseCase list;
    private final FindBankAccountUseCase find;
    private final CreateBankAccountUseCase create;
    private final UpdateBankAccountUseCase update;
    private final DeleteBankAccountUseCase delete;
    private final EnsureBankAccountExistsUseCase ensureExists;

    public BankAccountApplicationService(BankAccountRepository repository) {
        list = new ListBankAccountsUseCase(repository);
        find = new FindBankAccountUseCase(repository);
        create = new CreateBankAccountUseCase(repository);
        update = new UpdateBankAccountUseCase(repository);
        delete = new DeleteBankAccountUseCase(repository);
        ensureExists = new EnsureBankAccountExistsUseCase(repository);
    }

    public List<BankAccount> list() {
        return list.execute();
    }

    public Optional<BankAccount> find(String alias) {
        return find.execute(alias);
    }

    public void create(BankAccount account) {
        create.execute(account);
    }

    public void update(String oldAlias, BankAccount account) {
        update.execute(oldAlias, account);
    }

    public void delete(String alias) {
        delete.execute(alias);
    }

    public void ensureExists(String alias, String notes) {
        ensureExists.execute(alias, notes);
    }
}
