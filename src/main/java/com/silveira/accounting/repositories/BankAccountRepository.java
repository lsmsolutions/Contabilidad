package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.bank.BankAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BankAccountRepository {
    private final DatabaseManager databaseManager;

    public BankAccountRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(BankAccount account) {
        String sql = """
            INSERT OR REPLACE INTO bank_accounts(alias, account_number, bank_name, account_type, notes)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getAlias());
            statement.setString(2, account.getAccountNumber());
            statement.setString(3, account.getBankName());
            statement.setString(4, account.getAccountType());
            statement.setString(5, account.getNotes());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar cuenta bancaria", exception);
        }
    }

    public void saveIfMissing(BankAccount account) {
        String sql = """
            INSERT OR IGNORE INTO bank_accounts(alias, account_number, bank_name, account_type, notes)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getAlias());
            statement.setString(2, account.getAccountNumber());
            statement.setString(3, account.getBankName());
            statement.setString(4, account.getAccountType());
            statement.setString(5, account.getNotes());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar cuenta bancaria", exception);
        }
    }

    public List<BankAccount> findAll() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM bank_accounts ORDER BY alias");
             ResultSet rs = statement.executeQuery()) {
            List<BankAccount> accounts = new ArrayList<>();
            while (rs.next()) {
                accounts.add(new BankAccount(
                    rs.getLong("id"),
                    rs.getString("alias"),
                    rs.getString("account_number"),
                    rs.getString("bank_name"),
                    rs.getString("account_type"),
                    rs.getString("notes")
                ));
            }
            return accounts;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar cuentas bancarias", exception);
        }
    }

    public void delete(String alias) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement reconciliations = connection.prepareStatement("""
                 DELETE FROM reconciliations
                 WHERE bank_transaction_id IN (SELECT id FROM bank_transactions WHERE account_alias=?)
                 """);
             PreparedStatement transactions = connection.prepareStatement("DELETE FROM bank_transactions WHERE account_alias=?");
             PreparedStatement account = connection.prepareStatement("DELETE FROM bank_accounts WHERE alias=?")) {
            reconciliations.setString(1, alias);
            reconciliations.executeUpdate();
            transactions.setString(1, alias);
            transactions.executeUpdate();
            account.setString(1, alias);
            account.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar cuenta bancaria", exception);
        }
    }

    public void rename(String oldAlias, String newAlias) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement account = connection.prepareStatement("UPDATE bank_accounts SET alias=? WHERE alias=?");
                 PreparedStatement transactions = connection.prepareStatement("UPDATE bank_transactions SET account_alias=? WHERE account_alias=?");
                 PreparedStatement periods = connection.prepareStatement("UPDATE bank_statement_periods SET account_alias=? WHERE account_alias=?");
                 PreparedStatement marks = connection.prepareStatement("UPDATE review_marks SET account_alias=? WHERE source='bank' AND account_alias=?");
                 PreparedStatement closings = connection.prepareStatement("UPDATE monthly_closings SET source=? WHERE source=?");
                 PreparedStatement houseExpenses = connection.prepareStatement("UPDATE house_expenses SET payment_source=? WHERE payment_source=?")) {
                account.setString(1, newAlias);
                account.setString(2, oldAlias);
                account.executeUpdate();
                transactions.setString(1, newAlias);
                transactions.setString(2, oldAlias);
                transactions.executeUpdate();
                periods.setString(1, newAlias);
                periods.setString(2, oldAlias);
                periods.executeUpdate();
                marks.setString(1, newAlias);
                marks.setString(2, oldAlias);
                marks.executeUpdate();
                closings.setString(1, bankClosingSource(newAlias));
                closings.setString(2, bankClosingSource(oldAlias));
                closings.executeUpdate();
                houseExpenses.setString(1, paymentSource(newAlias));
                houseExpenses.setString(2, paymentSource(oldAlias));
                houseExpenses.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo renombrar cuenta bancaria", exception);
        }
    }

    public void update(String oldAlias, BankAccount account) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement accountStatement = connection.prepareStatement("""
                     UPDATE bank_accounts
                     SET alias=?, account_number=?, bank_name=?, account_type=?, notes=?
                     WHERE alias=?
                     """);
                 PreparedStatement transactions = connection.prepareStatement("UPDATE bank_transactions SET account_alias=? WHERE account_alias=?");
                 PreparedStatement periods = connection.prepareStatement("UPDATE bank_statement_periods SET account_alias=? WHERE account_alias=?");
                 PreparedStatement marks = connection.prepareStatement("UPDATE review_marks SET account_alias=? WHERE source='bank' AND account_alias=?");
                 PreparedStatement closings = connection.prepareStatement("UPDATE monthly_closings SET source=? WHERE source=?");
                 PreparedStatement houseExpenses = connection.prepareStatement("UPDATE house_expenses SET payment_source=? WHERE payment_source=?")) {
                accountStatement.setString(1, account.getAlias());
                accountStatement.setString(2, account.getAccountNumber());
                accountStatement.setString(3, account.getBankName());
                accountStatement.setString(4, account.getAccountType());
                accountStatement.setString(5, account.getNotes());
                accountStatement.setString(6, oldAlias);
                accountStatement.executeUpdate();
                transactions.setString(1, account.getAlias());
                transactions.setString(2, oldAlias);
                transactions.executeUpdate();
                periods.setString(1, account.getAlias());
                periods.setString(2, oldAlias);
                periods.executeUpdate();
                marks.setString(1, account.getAlias());
                marks.setString(2, oldAlias);
                marks.executeUpdate();
                closings.setString(1, bankClosingSource(account.getAlias()));
                closings.setString(2, bankClosingSource(oldAlias));
                closings.executeUpdate();
                houseExpenses.setString(1, paymentSource(account.getAlias()));
                houseExpenses.setString(2, paymentSource(oldAlias));
                houseExpenses.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo editar cuenta bancaria", exception);
        }
    }

    private String bankClosingSource(String alias) {
        return "BANK:" + (alias == null || alias.isBlank() ? "sin_cuenta" : alias);
    }

    private String paymentSource(String alias) {
        return "Cuenta: " + (alias == null ? "" : alias);
    }
}
