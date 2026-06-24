package com.silveira.accounting.repositories.investment;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.investment.InvestmentAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvestmentAccountRepository {
    private final DatabaseManager databaseManager;

    public InvestmentAccountRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(InvestmentAccount account) {
        String sql = """
            INSERT INTO investment_accounts(alias, provider_name, account_type, account_number, notes)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(alias) DO UPDATE SET provider_name=excluded.provider_name,
                account_type=excluded.account_type, account_number=excluded.account_number,
                notes=COALESCE(investment_accounts.notes, excluded.notes)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, account);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar la cuenta de inversion", exception);
        }
    }

    public void update(String originalAlias, InvestmentAccount account) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement accountUpdate = connection.prepareStatement("""
                    UPDATE investment_accounts SET alias=?, provider_name=?, account_type=?, account_number=?, notes=?
                    WHERE alias=?
                    """);
                 PreparedStatement statementUpdate = connection.prepareStatement(
                     "UPDATE investment_statements SET account_alias=? WHERE account_alias=?")) {
                bind(accountUpdate, account);
                accountUpdate.setString(6, originalAlias);
                accountUpdate.executeUpdate();
                statementUpdate.setString(1, account.getAlias());
                statementUpdate.setString(2, originalAlias);
                statementUpdate.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar la cuenta de inversion", exception);
        }
    }

    public List<InvestmentAccount> findAll() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM investment_accounts ORDER BY alias");
             ResultSet result = statement.executeQuery()) {
            List<InvestmentAccount> values = new ArrayList<>();
            while (result.next()) {
                values.add(map(result));
            }
            return values;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar las cuentas de inversion", exception);
        }
    }

    public Optional<InvestmentAccount> findByAlias(String alias) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM investment_accounts WHERE alias=?")) {
            statement.setString(1, alias);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar la cuenta de inversion", exception);
        }
    }

    public void delete(String alias) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement allocations = childDelete(connection, "investment_allocations", alias);
                 PreparedStatement positions = childDelete(connection, "investment_positions", alias);
                 PreparedStatement transactions = childDelete(connection, "investment_transactions", alias);
                 PreparedStatement statements = connection.prepareStatement("DELETE FROM investment_statements WHERE account_alias=?");
                 PreparedStatement account = connection.prepareStatement("DELETE FROM investment_accounts WHERE alias=?")) {
                execute(allocations, alias);
                execute(positions, alias);
                execute(transactions, alias);
                execute(statements, alias);
                execute(account, alias);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar la cuenta de inversion", exception);
        }
    }

    private PreparedStatement childDelete(Connection connection, String table, String alias) throws SQLException {
        return connection.prepareStatement("DELETE FROM " + table
            + " WHERE statement_id IN (SELECT id FROM investment_statements WHERE account_alias=?)");
    }

    private void execute(PreparedStatement statement, String alias) throws SQLException {
        statement.setString(1, alias);
        statement.executeUpdate();
    }

    private void bind(PreparedStatement statement, InvestmentAccount account) throws SQLException {
        statement.setString(1, account.getAlias());
        statement.setString(2, account.getProviderName());
        statement.setString(3, account.getAccountType());
        statement.setString(4, account.getAccountNumber());
        statement.setString(5, account.getNotes());
    }

    private InvestmentAccount map(ResultSet result) throws SQLException {
        InvestmentAccount value = new InvestmentAccount();
        value.setId(result.getLong("id"));
        value.setAlias(result.getString("alias"));
        value.setProviderName(result.getString("provider_name"));
        value.setAccountType(result.getString("account_type"));
        value.setAccountNumber(result.getString("account_number"));
        value.setNotes(result.getString("notes"));
        return value;
    }
}
