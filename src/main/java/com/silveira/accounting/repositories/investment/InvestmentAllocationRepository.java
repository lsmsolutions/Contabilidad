package com.silveira.accounting.repositories.investment;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InvestmentAllocationRepository {
    private final DatabaseManager databaseManager;

    public InvestmentAllocationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void replace(long statementId, List<InvestmentAllocation> values) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM investment_allocations WHERE statement_id=?");
                 PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO investment_allocations(statement_id, category, market_value, percentage) VALUES (?, ?, ?, ?)")) {
                delete.setLong(1, statementId);
                delete.executeUpdate();
                for (InvestmentAllocation value : values) {
                    insert.setLong(1, statementId);
                    insert.setString(2, value.getCategory());
                    insert.setDouble(3, value.getMarketValue());
                    insert.setDouble(4, value.getPercentage());
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar la distribucion de inversion", exception);
        }
    }

    public List<InvestmentAllocation> findByStatement(long statementId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM investment_allocations WHERE statement_id=? ORDER BY market_value DESC")) {
            statement.setLong(1, statementId);
            try (ResultSet result = statement.executeQuery()) {
                List<InvestmentAllocation> values = new ArrayList<>();
                while (result.next()) {
                    InvestmentAllocation value = new InvestmentAllocation();
                    value.setStatementId(statementId);
                    value.setCategory(result.getString("category"));
                    value.setMarketValue(result.getDouble("market_value"));
                    value.setPercentage(result.getDouble("percentage"));
                    values.add(value);
                }
                return values;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar la distribucion de inversion", exception);
        }
    }
}
