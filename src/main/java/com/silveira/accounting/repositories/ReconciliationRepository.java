package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.ReconciliationItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ReconciliationRepository {
    private final DatabaseManager databaseManager;

    public ReconciliationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveMatches(List<ReconciliationItem> items, String notes) {
        String sql = "INSERT INTO reconciliations(bank_transaction_id, nyl_record_id, status, difference, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             PreparedStatement updateBank = connection.prepareStatement("UPDATE bank_transactions SET reconciled=1 WHERE id=?")) {
            connection.setAutoCommit(false);
            for (ReconciliationItem item : items) {
                if (!"Conciliado".equals(item.status())) {
                    continue;
                }
                statement.setLong(1, item.bankId());
                statement.setLong(2, item.nylId());
                statement.setString(3, item.status());
                statement.setDouble(4, item.difference());
                statement.setString(5, notes);
                statement.executeUpdate();
                updateBank.setLong(1, item.bankId());
                updateBank.executeUpdate();
            }
            connection.commit();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar la conciliacion", exception);
        }
    }
}
