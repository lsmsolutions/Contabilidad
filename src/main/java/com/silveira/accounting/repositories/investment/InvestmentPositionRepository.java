package com.silveira.accounting.repositories.investment;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.investment.InvestmentPosition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InvestmentPositionRepository {
    private final DatabaseManager databaseManager;

    public InvestmentPositionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void replace(long statementId, List<InvestmentPosition> values) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM investment_positions WHERE statement_id=?");
                 PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO investment_positions(
                         statement_id, symbol, description, asset_type, quantity, price,
                         market_value, cost_basis, unrealized_gain_loss
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
                delete.setLong(1, statementId);
                delete.executeUpdate();
                for (InvestmentPosition value : values) {
                    insert.setLong(1, statementId);
                    insert.setString(2, value.getSymbol());
                    insert.setString(3, value.getDescription());
                    insert.setString(4, value.getAssetType());
                    insert.setDouble(5, value.getQuantity());
                    insert.setDouble(6, value.getPrice());
                    insert.setDouble(7, value.getMarketValue());
                    insert.setDouble(8, value.getCostBasis());
                    insert.setDouble(9, value.getUnrealizedGainLoss());
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar las posiciones de inversion", exception);
        }
    }

    public List<InvestmentPosition> findByStatement(long statementId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM investment_positions WHERE statement_id=? ORDER BY asset_type, symbol")) {
            statement.setLong(1, statementId);
            try (ResultSet result = statement.executeQuery()) {
                List<InvestmentPosition> values = new ArrayList<>();
                while (result.next()) {
                    InvestmentPosition value = new InvestmentPosition();
                    value.setStatementId(statementId);
                    value.setSymbol(result.getString("symbol"));
                    value.setDescription(result.getString("description"));
                    value.setAssetType(result.getString("asset_type"));
                    value.setQuantity(result.getDouble("quantity"));
                    value.setPrice(result.getDouble("price"));
                    value.setMarketValue(result.getDouble("market_value"));
                    value.setCostBasis(result.getDouble("cost_basis"));
                    value.setUnrealizedGainLoss(result.getDouble("unrealized_gain_loss"));
                    values.add(value);
                }
                return values;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar las posiciones de inversion", exception);
        }
    }
}
