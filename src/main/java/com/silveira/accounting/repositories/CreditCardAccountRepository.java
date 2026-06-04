package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.CreditCardAccount;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CreditCardAccountRepository {
    private final DatabaseManager databaseManager;

    public CreditCardAccountRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(CreditCardAccount account) {
        String sql = "INSERT OR REPLACE INTO credit_card_accounts(alias, bank_name, card_name, account_last_digits, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getAlias());
            statement.setString(2, account.getBankName());
            statement.setString(3, account.getCardName());
            statement.setString(4, account.getAccountLastDigits());
            statement.setString(5, account.getNotes());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar tarjeta", exception);
        }
    }

    public List<CreditCardAccount> findAll() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM credit_card_accounts ORDER BY alias");
             ResultSet rs = statement.executeQuery()) {
            List<CreditCardAccount> accounts = new ArrayList<>();
            while (rs.next()) {
                accounts.add(new CreditCardAccount(rs.getLong("id"), rs.getString("alias"), rs.getString("bank_name"),
                    rs.getString("card_name"), rs.getString("account_last_digits"), rs.getString("notes")));
            }
            return accounts;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar tarjetas", exception);
        }
    }

    public void delete(String alias) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement alerts = connection.prepareStatement("""
                 DELETE FROM financial_alerts
                 WHERE statement_id IN (SELECT id FROM credit_card_statements WHERE account_alias=?)
                 """);
             PreparedStatement transactions = connection.prepareStatement("""
                 DELETE FROM credit_card_transactions
                 WHERE statement_id IN (SELECT id FROM credit_card_statements WHERE account_alias=?)
                 """);
             PreparedStatement statements = connection.prepareStatement("DELETE FROM credit_card_statements WHERE account_alias=?");
             PreparedStatement account = connection.prepareStatement("DELETE FROM credit_card_accounts WHERE alias=?")) {
            alerts.setString(1, alias);
            alerts.executeUpdate();
            transactions.setString(1, alias);
            transactions.executeUpdate();
            statements.setString(1, alias);
            statements.executeUpdate();
            account.setString(1, alias);
            account.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar tarjeta", exception);
        }
    }

    public void rename(String oldAlias, String newAlias) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement account = connection.prepareStatement("UPDATE credit_card_accounts SET alias=? WHERE alias=?");
                 PreparedStatement statements = connection.prepareStatement("UPDATE credit_card_statements SET account_alias=? WHERE account_alias=?");
                 PreparedStatement marks = connection.prepareStatement("UPDATE review_marks SET account_alias=? WHERE source='card' AND account_alias=?");
                 PreparedStatement houseExpenses = connection.prepareStatement("UPDATE house_expenses SET payment_source=? WHERE payment_source=?")) {
                account.setString(1, newAlias);
                account.setString(2, oldAlias);
                account.executeUpdate();
                statements.setString(1, newAlias);
                statements.setString(2, oldAlias);
                statements.executeUpdate();
                marks.setString(1, newAlias);
                marks.setString(2, oldAlias);
                marks.executeUpdate();
                houseExpenses.setString(1, paymentSource(newAlias));
                houseExpenses.setString(2, paymentSource(oldAlias));
                houseExpenses.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo renombrar tarjeta", exception);
        }
    }

    public void update(String oldAlias, CreditCardAccount account) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement accountStatement = connection.prepareStatement("""
                 UPDATE credit_card_accounts
                     SET alias=?, bank_name=?, card_name=?, account_last_digits=?, notes=?
                     WHERE alias=?
                     """);
                 PreparedStatement statements = connection.prepareStatement("UPDATE credit_card_statements SET account_alias=? WHERE account_alias=?");
                 PreparedStatement marks = connection.prepareStatement("UPDATE review_marks SET account_alias=? WHERE source='card' AND account_alias=?");
                 PreparedStatement houseExpenses = connection.prepareStatement("UPDATE house_expenses SET payment_source=? WHERE payment_source=?")) {
                accountStatement.setString(1, account.getAlias());
                accountStatement.setString(2, account.getBankName());
                accountStatement.setString(3, account.getCardName());
                accountStatement.setString(4, account.getAccountLastDigits());
                accountStatement.setString(5, account.getNotes());
                accountStatement.setString(6, oldAlias);
                accountStatement.executeUpdate();
                statements.setString(1, account.getAlias());
                statements.setString(2, oldAlias);
                statements.executeUpdate();
                marks.setString(1, account.getAlias());
                marks.setString(2, oldAlias);
                marks.executeUpdate();
                houseExpenses.setString(1, paymentSource(account.getAlias()));
                houseExpenses.setString(2, paymentSource(oldAlias));
                houseExpenses.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo editar tarjeta", exception);
        }
    }

    private String paymentSource(String alias) {
        return "Tarjeta: " + (alias == null ? "" : alias);
    }
}
