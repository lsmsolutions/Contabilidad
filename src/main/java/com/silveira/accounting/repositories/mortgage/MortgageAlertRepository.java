package com.silveira.accounting.repositories.mortgage;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.MortgageAlert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class MortgageAlertRepository {
    private final DatabaseManager databaseManager;

    public MortgageAlertRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveAll(long statementId, List<MortgageAlert> alerts) {
        String delete = "DELETE FROM mortgage_alerts WHERE statement_id=?";
        String insert = "INSERT INTO mortgage_alerts(statement_id, severity, title, message) VALUES (?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement deleteStatement = connection.prepareStatement(delete);
             PreparedStatement insertStatement = connection.prepareStatement(insert)) {
            deleteStatement.setLong(1, statementId);
            deleteStatement.executeUpdate();
            for (MortgageAlert alert : alerts) {
                insertStatement.setLong(1, statementId);
                insertStatement.setString(2, alert.severity());
                insertStatement.setString(3, alert.title());
                insertStatement.setString(4, alert.message());
                insertStatement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar alertas de hipoteca", exception);
        }
    }
}
