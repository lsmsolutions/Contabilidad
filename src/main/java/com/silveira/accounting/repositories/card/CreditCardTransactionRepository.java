package com.silveira.accounting.repositories.card;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.CreditCardTransaction;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreditCardTransactionRepository {
    private final DatabaseManager databaseManager;

    public CreditCardTransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveAll(long statementId, List<CreditCardTransaction> transactions) {
        String sql = "INSERT INTO credit_card_transactions(statement_id, transaction_date, post_date, description, amount, type, category, review_required, pending_review, review_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            for (CreditCardTransaction t : transactions) {
                ps.setLong(1, statementId);
                ps.setString(2, t.getTransactionDate() == null ? null : t.getTransactionDate().toString());
                ps.setString(3, t.getPostDate() == null ? null : t.getPostDate().toString());
                ps.setString(4, t.getDescription());
                ps.setDouble(5, t.getAmount());
                ps.setString(6, t.getType());
                ps.setString(7, t.getCategory());
                ps.setInt(8, t.isReviewRequired() ? 1 : 0);
                ps.setInt(9, t.isPendingReview() ? 1 : 0);
                ps.setString(10, t.getReviewNotes());
                ps.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar transacciones de tarjeta", exception);
        }
    }

    public long save(long statementId, CreditCardTransaction t) {
        String sql = "INSERT INTO credit_card_transactions(statement_id, transaction_date, post_date, description, amount, type, category, review_required, pending_review, review_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, statementId, t);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar movimiento de tarjeta", exception);
        }
    }

    public List<CreditCardTransaction> findByAccount(String alias, Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.* FROM credit_card_transactions t
            JOIN credit_card_statements s ON s.id=t.statement_id
            WHERE s.account_alias=?
            """);
        if (year != null) sql.append(" AND substr(s.statement_end_date, 1, 4)=?");
        if (month != null) sql.append(" AND substr(s.statement_end_date, 6, 2)=?");
        sql.append(" ORDER BY s.statement_end_date DESC, t.post_date, t.id");
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setString(index++, alias);
            if (year != null) ps.setString(index++, String.valueOf(year));
            if (month != null) ps.setString(index++, String.format("%02d", month));
            try (ResultSet rs = ps.executeQuery()) {
                List<CreditCardTransaction> rows = new ArrayList<>();
                while (rs.next()) rows.add(map(rs));
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar movimientos de tarjeta", exception);
        }
    }

    public void update(CreditCardTransaction t) {
        String sql = """
            UPDATE credit_card_transactions SET transaction_date=?, post_date=?, description=?, amount=?, type=?, category=?,
                review_required=?, pending_review=?, review_notes=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date(t.getTransactionDate()));
            ps.setString(2, date(t.getPostDate()));
            ps.setString(3, t.getDescription());
            ps.setDouble(4, t.getAmount());
            ps.setString(5, t.getType());
            ps.setString(6, t.getCategory());
            ps.setInt(7, t.isReviewRequired() ? 1 : 0);
            ps.setInt(8, t.isPendingReview() ? 1 : 0);
            ps.setString(9, t.getReviewNotes());
            ps.setLong(10, t.getId());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar movimiento de tarjeta", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement("DELETE FROM credit_card_transactions WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar movimiento de tarjeta", exception);
        }
    }

    private void bind(PreparedStatement ps, long statementId, CreditCardTransaction t) throws SQLException {
        ps.setLong(1, statementId);
        ps.setString(2, date(t.getTransactionDate()));
        ps.setString(3, date(t.getPostDate()));
        ps.setString(4, t.getDescription());
        ps.setDouble(5, t.getAmount());
        ps.setString(6, t.getType());
        ps.setString(7, t.getCategory());
        ps.setInt(8, t.isReviewRequired() ? 1 : 0);
        ps.setInt(9, t.isPendingReview() ? 1 : 0);
        ps.setString(10, t.getReviewNotes());
    }

    private CreditCardTransaction map(ResultSet rs) throws SQLException {
        CreditCardTransaction t = new CreditCardTransaction(
            rs.getLong("id"),
            rs.getLong("statement_id"),
            localDate(rs.getString("transaction_date")),
            localDate(rs.getString("post_date")),
            rs.getString("description"),
            rs.getDouble("amount"),
            rs.getString("type"),
            rs.getString("category")
        );
        t.setReviewRequired(rs.getInt("review_required") == 1);
        t.setPendingReview(rs.getInt("pending_review") == 1);
        t.setReviewNotes(rs.getString("review_notes"));
        return t;
    }

    private String date(LocalDate date) { return date == null ? null : date.toString(); }
    private LocalDate localDate(String value) { return value == null ? null : LocalDate.parse(value); }
}
