package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.bank.BankStatementPeriod;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BankStatementPeriodRepository {
    private final DatabaseManager databaseManager;

    public BankStatementPeriodRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<BankStatementPeriod> find(String accountAlias, String sourcePdf) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM bank_statement_periods WHERE account_alias=? AND source_pdf=?")) {
            statement.setString(1, clean(accountAlias));
            statement.setString(2, clean(sourcePdf));
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar el periodo bancario", exception);
        }
    }

    public Optional<BankStatementPeriod> findOverlapping(String accountAlias, LocalDate start, LocalDate end) {
        String sql = """
            SELECT * FROM bank_statement_periods
            WHERE account_alias=?
              AND period_start <= ?
              AND period_end >= ?
            ORDER BY period_start DESC, id DESC
            LIMIT 1
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clean(accountAlias));
            statement.setString(2, end.toString());
            statement.setString(3, start.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar el periodo bancario", exception);
        }
    }

    public List<BankStatementPeriod> findByAccount(String accountAlias) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM bank_statement_periods WHERE account_alias=? ORDER BY period_start, id")) {
            statement.setString(1, clean(accountAlias));
            try (ResultSet rs = statement.executeQuery()) {
                List<BankStatementPeriod> periods = new ArrayList<>();
                while (rs.next()) {
                    periods.add(map(rs));
                }
                return periods;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar periodos bancarios", exception);
        }
    }

    public void save(BankStatementPeriod period) {
        String updateSql = """
            UPDATE bank_statement_periods
            SET period_start=?,
                period_end=?,
                opening_balance=?,
                statement_ending_balance=?
            WHERE account_alias=? AND source_pdf=?
            """;
        String insertSql = """
            INSERT INTO bank_statement_periods(account_alias, source_pdf, period_start, period_end, opening_balance, statement_ending_balance)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        String accountAlias = clean(period.accountAlias());
        String sourcePdf = clean(period.sourcePdf());
        try (Connection connection = databaseManager.getConnection()) {
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, period.periodStart().toString());
                update.setString(2, period.periodEnd().toString());
                update.setDouble(3, period.openingBalance());
                update.setDouble(4, period.statementEndingBalance());
                update.setString(5, accountAlias);
                update.setString(6, sourcePdf);
                if (update.executeUpdate() > 0) {
                    return;
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setString(1, accountAlias);
                insert.setString(2, sourcePdf);
                insert.setString(3, period.periodStart().toString());
                insert.setString(4, period.periodEnd().toString());
                insert.setDouble(5, period.openingBalance());
                insert.setDouble(6, period.statementEndingBalance());
                insert.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar el periodo bancario", exception);
        }
    }

    public void delete(String accountAlias, String sourcePdf) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM bank_statement_periods WHERE account_alias=? AND source_pdf=?")) {
            statement.setString(1, clean(accountAlias));
            statement.setString(2, clean(sourcePdf));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar el periodo bancario", exception);
        }
    }

    private BankStatementPeriod map(ResultSet rs) throws SQLException {
        return new BankStatementPeriod(
            rs.getString("account_alias"),
            rs.getString("source_pdf"),
            LocalDate.parse(rs.getString("period_start")),
            LocalDate.parse(rs.getString("period_end")),
            rs.getDouble("opening_balance"),
            rs.getDouble("statement_ending_balance")
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? "sin_cuenta" : value;
    }
}
