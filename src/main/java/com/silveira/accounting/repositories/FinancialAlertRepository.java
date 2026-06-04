package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.FinancialAlert;

import java.sql.*;
import java.util.List;

public class FinancialAlertRepository {
    private final DatabaseManager databaseManager;

    public FinancialAlertRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveAll(long statementId, List<FinancialAlert> alerts) {
        String sql = "INSERT INTO financial_alerts(statement_id, severity, title, message) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            for (FinancialAlert alert : alerts) {
                ps.setLong(1, statementId);
                ps.setString(2, alert.severity());
                ps.setString(3, alert.title());
                ps.setString(4, alert.message());
                ps.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar alertas financieras", exception);
        }
    }
}
