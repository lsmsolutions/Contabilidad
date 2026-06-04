package com.silveira.accounting.controllers.bank;

import com.silveira.accounting.models.bank.BankAccount;
import java.util.Optional;

public class BankAccountWorkflow {
    private final BankAccountController accounts;

    public BankAccountWorkflow(BankAccountController accounts) {
        this.accounts = accounts;
    }

    public Optional<BankAccount> findForEdit(String alias) {
        if (alias == null || alias.isBlank()) {
            return Optional.empty();
        }
        return accounts.find(alias.trim());
    }

    public DeleteResult delete(String alias) {
        if (alias == null || alias.isBlank()) {
            return DeleteResult.blank();
        }
        String trimmed = alias.trim();
        if (!accounts.exists(trimmed)) {
            return DeleteResult.notFound(trimmed);
        }
        accounts.delete(trimmed);
        return DeleteResult.deleted(trimmed);
    }

    public UpdateResult update(BankAccount current, BankAccount updated) {
        String newAlias = updated.getAlias() == null ? "" : updated.getAlias().trim();
        if (newAlias.isBlank()) {
            return UpdateResult.aliasRequired();
        }
        if (!newAlias.equals(current.getAlias()) && accounts.exists(newAlias)) {
            return UpdateResult.duplicate(newAlias);
        }
        accounts.update(current.getAlias(), updated);
        return UpdateResult.updated();
    }

    public record DeleteResult(Status status, String alias) {
        public static DeleteResult blank() {
            return new DeleteResult(Status.BLANK, "");
        }

        public static DeleteResult notFound(String alias) {
            return new DeleteResult(Status.NOT_FOUND, alias);
        }

        public static DeleteResult deleted(String alias) {
            return new DeleteResult(Status.DELETED, alias);
        }
    }

    public record UpdateResult(Status status, String alias) {
        public static UpdateResult aliasRequired() {
            return new UpdateResult(Status.ALIAS_REQUIRED, "");
        }

        public static UpdateResult duplicate(String alias) {
            return new UpdateResult(Status.DUPLICATE, alias);
        }

        public static UpdateResult updated() {
            return new UpdateResult(Status.UPDATED, "");
        }
    }

    public enum Status {
        BLANK,
        NOT_FOUND,
        DELETED,
        ALIAS_REQUIRED,
        DUPLICATE,
        UPDATED
    }
}
