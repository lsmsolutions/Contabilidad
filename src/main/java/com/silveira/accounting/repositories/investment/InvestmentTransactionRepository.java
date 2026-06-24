package com.silveira.accounting.repositories.investment;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvestmentTransactionRepository {
    private final DatabaseManager databaseManager;

    public InvestmentTransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void replace(long statementId, List<InvestmentTransaction> values) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM investment_transactions WHERE statement_id=?");
                 PreparedStatement insert = connection.prepareStatement("""
                     INSERT INTO investment_transactions(
                         statement_id, transaction_date, action, symbol, description,
                         quantity, price, amount, realized_gain_loss
                     ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
                delete.setLong(1, statementId);
                delete.executeUpdate();
                for (InvestmentTransaction value : values) {
                    insert.setLong(1, statementId);
                    insert.setString(2, value.getTransactionDate() == null ? null : value.getTransactionDate().toString());
                    insert.setString(3, value.getAction());
                    insert.setString(4, value.getSymbol());
                    insert.setString(5, value.getDescription());
                    insert.setDouble(6, value.getQuantity());
                    insert.setDouble(7, value.getPrice());
                    insert.setDouble(8, value.getAmount());
                    insert.setDouble(9, value.getRealizedGainLoss());
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar los movimientos de inversion", exception);
        }
    }

    public List<InvestmentTransaction> findByStatement(long statementId) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM investment_transactions WHERE statement_id=? ORDER BY transaction_date, id")) {
            statement.setLong(1, statementId);
            try (ResultSet result = statement.executeQuery()) {
                List<InvestmentTransaction> values = new ArrayList<>();
                while (result.next()) {
                    InvestmentTransaction value = new InvestmentTransaction();
                    value.setStatementId(statementId);
                    String date = result.getString("transaction_date");
                    value.setTransactionDate(date == null || date.isBlank() ? null : LocalDate.parse(date));
                    value.setAction(result.getString("action"));
                    value.setSymbol(result.getString("symbol"));
                    value.setDescription(result.getString("description"));
                    value.setQuantity(result.getDouble("quantity"));
                    value.setPrice(result.getDouble("price"));
                    value.setAmount(result.getDouble("amount"));
                    value.setRealizedGainLoss(result.getDouble("realized_gain_loss"));
                    values.add(value);
                }
                return values;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar los movimientos de inversion", exception);
        }
    }
}
