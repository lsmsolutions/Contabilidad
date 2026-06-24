package com.silveira.accounting.repositories.investment;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.investment.InvestmentStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvestmentStatementRepository {
    private final DatabaseManager databaseManager;

    public InvestmentStatementRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public long save(InvestmentStatement value) {
        String sql = """
            INSERT INTO investment_statements(
                account_alias, period_start, period_end, beginning_value, ending_value, deposits, withdrawals,
                dividends_interest, market_change, expenses, unrealized_gain_loss, source_pdf_path
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(account_alias, period_end) DO UPDATE SET
                period_start=excluded.period_start, beginning_value=excluded.beginning_value,
                ending_value=excluded.ending_value, deposits=excluded.deposits, withdrawals=excluded.withdrawals,
                dividends_interest=excluded.dividends_interest, market_change=excluded.market_change,
                expenses=excluded.expenses, unrealized_gain_loss=excluded.unrealized_gain_loss,
                source_pdf_path=excluded.source_pdf_path
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, value);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return findId(connection, value.getAccountAlias(), value.getPeriodEnd());
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar el estado de inversion", exception);
        }
    }

    public List<InvestmentStatement> findByAccount(String alias) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM investment_statements WHERE account_alias=? ORDER BY period_end DESC")) {
            statement.setString(1, alias);
            try (ResultSet result = statement.executeQuery()) {
                List<InvestmentStatement> values = new ArrayList<>();
                while (result.next()) {
                    values.add(map(result));
                }
                return values;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar los estados de inversion", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement allocations = connection.prepareStatement("DELETE FROM investment_allocations WHERE statement_id=?");
                 PreparedStatement positions = connection.prepareStatement("DELETE FROM investment_positions WHERE statement_id=?");
                 PreparedStatement transactions = connection.prepareStatement("DELETE FROM investment_transactions WHERE statement_id=?");
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM investment_statements WHERE id=?")) {
                execute(allocations, id);
                execute(positions, id);
                execute(transactions, id);
                execute(statement, id);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar el estado de inversion", exception);
        }
    }

    private void execute(PreparedStatement statement, long id) throws SQLException {
        statement.setLong(1, id);
        statement.executeUpdate();
    }

    private void bind(PreparedStatement statement, InvestmentStatement value) throws SQLException {
        statement.setString(1, value.getAccountAlias());
        statement.setString(2, value.getPeriodStart().toString());
        statement.setString(3, value.getPeriodEnd().toString());
        statement.setDouble(4, value.getBeginningValue());
        statement.setDouble(5, value.getEndingValue());
        statement.setDouble(6, value.getDeposits());
        statement.setDouble(7, value.getWithdrawals());
        statement.setDouble(8, value.getDividendsInterest());
        statement.setDouble(9, value.getMarketChange());
        statement.setDouble(10, value.getExpenses());
        statement.setDouble(11, value.getUnrealizedGainLoss());
        statement.setString(12, value.getSourcePdfPath());
    }

    private long findId(Connection connection, String alias, LocalDate periodEnd) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id FROM investment_statements WHERE account_alias=? AND period_end=?")) {
            statement.setString(1, alias);
            statement.setString(2, periodEnd.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getLong(1);
                }
            }
        }
        throw new SQLException("No se encontro el estado de inversion guardado");
    }

    private InvestmentStatement map(ResultSet result) throws SQLException {
        InvestmentStatement value = new InvestmentStatement();
        value.setId(result.getLong("id"));
        value.setAccountAlias(result.getString("account_alias"));
        value.setPeriodStart(LocalDate.parse(result.getString("period_start")));
        value.setPeriodEnd(LocalDate.parse(result.getString("period_end")));
        value.setBeginningValue(result.getDouble("beginning_value"));
        value.setEndingValue(result.getDouble("ending_value"));
        value.setDeposits(result.getDouble("deposits"));
        value.setWithdrawals(result.getDouble("withdrawals"));
        value.setDividendsInterest(result.getDouble("dividends_interest"));
        value.setMarketChange(result.getDouble("market_change"));
        value.setExpenses(result.getDouble("expenses"));
        value.setUnrealizedGainLoss(result.getDouble("unrealized_gain_loss"));
        value.setSourcePdfPath(result.getString("source_pdf_path"));
        return value;
    }
}
