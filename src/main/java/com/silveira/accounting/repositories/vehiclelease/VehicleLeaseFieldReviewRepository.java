package com.silveira.accounting.repositories.vehiclelease;

import com.silveira.accounting.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class VehicleLeaseFieldReviewRepository {
    private final DatabaseManager databaseManager;

    public VehicleLeaseFieldReviewRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean isReviewed(long statementId, String fieldName) {
        String sql = "SELECT reviewed FROM vehicle_lease_statement_field_reviews WHERE statement_id=? AND field_name=?";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, statementId);
            statement.setString(2, fieldName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) != 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar la revision del campo", exception);
        }
    }

    public void setReviewed(long statementId, String fieldName, boolean reviewed) {
        String sql = """
            INSERT INTO vehicle_lease_statement_field_reviews(statement_id, field_name, reviewed, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(statement_id, field_name) DO UPDATE SET reviewed=excluded.reviewed, updated_at=CURRENT_TIMESTAMP
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, statementId);
            statement.setString(2, fieldName);
            statement.setInt(3, reviewed ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar la revision del campo", exception);
        }
    }

    public void setReviewed(long statementId, List<String> fields, boolean reviewed) {
        for (String field : fields) {
            setReviewed(statementId, field, reviewed);
        }
    }
}
