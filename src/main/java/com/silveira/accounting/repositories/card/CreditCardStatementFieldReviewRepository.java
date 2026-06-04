package com.silveira.accounting.repositories.card;

import com.silveira.accounting.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CreditCardStatementFieldReviewRepository {
    private final DatabaseManager databaseManager;

    public CreditCardStatementFieldReviewRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean isReviewed(long statementId, String fieldName, boolean defaultReviewed) {
        if (statementId <= 0) {
            return defaultReviewed;
        }
        String sql = "SELECT reviewed FROM credit_card_statement_field_reviews WHERE statement_id=? AND field_name=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, statementId);
            statement.setString(2, fieldName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("reviewed") == 1 : defaultReviewed;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar revision de campo de tarjeta", exception);
        }
    }

    public Map<String, Boolean> findByStatement(long statementId) {
        Map<String, Boolean> reviews = new HashMap<>();
        if (statementId <= 0) {
            return reviews;
        }
        String sql = "SELECT field_name, reviewed FROM credit_card_statement_field_reviews WHERE statement_id=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, statementId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    reviews.put(rs.getString("field_name"), rs.getInt("reviewed") == 1);
                }
            }
            return reviews;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar revisiones de campos de tarjeta", exception);
        }
    }

    public void setReviewed(long statementId, String fieldName, boolean reviewed) {
        if (statementId <= 0) {
            return;
        }
        String sql = """
            INSERT INTO credit_card_statement_field_reviews(statement_id, field_name, reviewed, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(statement_id, field_name)
            DO UPDATE SET reviewed=excluded.reviewed, updated_at=CURRENT_TIMESTAMP
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, statementId);
            statement.setString(2, fieldName);
            statement.setInt(3, reviewed ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar revision de campo de tarjeta", exception);
        }
    }

    public void setReviewed(long statementId, Collection<String> fieldNames, boolean reviewed) {
        for (String fieldName : fieldNames) {
            setReviewed(statementId, fieldName, reviewed);
        }
    }
}
