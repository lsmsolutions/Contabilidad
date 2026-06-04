package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class NylMonthlyResultRepository {
    private static final String SOURCE = "NYL";
    private static final String REPORT_RESULT = "resultado_pdf";
    private static final String NYL_BANK = "nyl_banco";

    private final DatabaseManager databaseManager;

    public NylMonthlyResultRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<Double> findPdfResult(int year, int month) {
        return findMetric(year, month, REPORT_RESULT);
    }

    public Optional<Double> findNylBank(int year, int month) {
        return findMetric(year, month, NYL_BANK);
    }

    private Optional<Double> findMetric(int year, int month, String metric) {
        String sql = "SELECT amount FROM monthly_closings WHERE source=? AND year=? AND month=? AND metric=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SOURCE);
            statement.setInt(2, year);
            statement.setInt(3, month);
            statement.setString(4, metric);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(rs.getDouble("amount")) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo leer cierre mensual de NYL", exception);
        }
    }

    public void savePdfResult(int year, int month, double amount) {
        saveMetric(year, month, REPORT_RESULT, amount);
    }

    public void saveNylBank(int year, int month, double amount) {
        saveMetric(year, month, NYL_BANK, amount);
    }

    private void saveMetric(int year, int month, String metric, double amount) {
        String sql = """
            INSERT INTO monthly_closings(source, year, month, metric, amount)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(source, year, month, metric) DO UPDATE SET amount=excluded.amount
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SOURCE);
            statement.setInt(2, year);
            statement.setInt(3, month);
            statement.setString(4, metric);
            statement.setDouble(5, amount);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar cierre mensual de NYL", exception);
        }
    }
}
