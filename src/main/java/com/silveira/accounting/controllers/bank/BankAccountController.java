package com.silveira.accounting.controllers.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.models.bank.BankAccount;
import java.util.List;
import java.util.Optional;

public class BankAccountController {
    private final BankApplicationService bank;

    public BankAccountController(BankApplicationService bank) {
        this.bank = bank;
    }

    public List<BankAccount> list() {
        return bank.accounts().list();
    }

    public Optional<BankAccount> find(String alias) {
        return bank.accounts().find(alias);
    }

    public BankAccount create(String aliasInput, String accountNumber, String bankName, String accountType, String notes) {
        String alias = aliasInput == null || aliasInput.isBlank() ? "cta_" + last4(accountNumber) : aliasInput.trim();
        BankAccount account = new BankAccount(0, alias, accountNumber, bankName, accountType, notes);
        bank.accounts().create(account);
        return account;
    }

    public void update(String oldAlias, BankAccount account) {
        bank.accounts().update(oldAlias, account);
    }

    public void delete(String alias) {
        bank.accounts().delete(alias);
    }

    public boolean exists(String alias) {
        return find(alias).isPresent();
    }

    private String last4(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return digits.isBlank() ? "nueva" : digits;
        }
        return digits.substring(digits.length() - 4);
    }
}
