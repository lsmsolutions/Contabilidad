package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.bank.BankMonthlyClosing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class BankMonthlyClosingRepository {
    private static final String OPENING_BALANCE = "saldo_inicial";
    private static final String STATEMENT_ENDING_BALANCE = "saldo_final_pdf";

    private final DatabaseManager databaseManager;

    public BankMonthlyClosingRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<BankMonthlyClosing> find(String accountAlias, int year, int month) {
        String source = source(accountAlias);
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT metric, amount FROM monthly_closings WHERE source=? AND year=? AND month=?")) {
            statement.setString(1, source);
            statement.setInt(2, year);
            statement.setInt(3, month);
            double opening = 0;
            double ending = 0;
            boolean found = false;
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    found = true;
                    if (OPENING_BALANCE.equals(rs.getString("metric"))) {
                        opening = rs.getDouble("amount");
                    } else if (STATEMENT_ENDING_BALANCE.equals(rs.getString("metric"))) {
                        ending = rs.getDouble("amount");
                    }
                }
            }
            return found ? Optional.of(new BankMonthlyClosing(accountAlias, year, month, opening, ending)) : Optional.empty();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar el cierre mensual bancario", exception);
        }
    }

    public void save(BankMonthlyClosing closing) {
        saveMetric(closing.accountAlias(), closing.year(), closing.month(), OPENING_BALANCE, closing.openingBalance());
        saveMetric(closing.accountAlias(), closing.year(), closing.month(), STATEMENT_ENDING_BALANCE, closing.statementEndingBalance());
    }

    private void saveMetric(String accountAlias, int year, int month, String metric, double amount) {
        String sql = """
            INSERT INTO monthly_closings(source, year, month, metric, amount)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(source, year, month, metric)
            DO UPDATE SET amount=excluded.amount
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, source(accountAlias));
            statement.setInt(2, year);
            statement.setInt(3, month);
            statement.setString(4, metric);
            statement.setDouble(5, amount);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar el cierre mensual bancario", exception);
        }
    }

    private String source(String accountAlias) {
        return "BANK:" + (accountAlias == null || accountAlias.isBlank() ? "sin_cuenta" : accountAlias);
    }
}
