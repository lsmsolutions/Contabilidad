package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.MortgageTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MortgageTransactionRepository {
    private final DatabaseManager databaseManager;

    public MortgageTransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveAll(long statementId, List<MortgageTransaction> rows) {
        String sql = "INSERT INTO mortgage_transactions(statement_id, transaction_date, description, total, principal, interest, escrow, fees, unapplied, corporate_advance, other, review_required, pending_review, review_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            for (MortgageTransaction row : rows) {
                bind(ps, statementId, row);
                ps.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar movimientos de hipoteca", exception);
        }
    }

    public long save(long statementId, MortgageTransaction row) {
        String sql = "INSERT INTO mortgage_transactions(statement_id, transaction_date, description, total, principal, interest, escrow, fees, unapplied, corporate_advance, other, review_required, pending_review, review_notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, statementId, row);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar movimiento de hipoteca", exception);
        }
    }

    public List<MortgageTransaction> findByLoan(String alias, Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("""
            SELECT t.* FROM mortgage_transactions t
            JOIN mortgage_statements s ON s.id=t.statement_id
            WHERE s.loan_alias=?
            """);
        if (year != null) sql.append(" AND substr(s.statement_date, 1, 4)=?");
        if (month != null) sql.append(" AND substr(s.statement_date, 6, 2)=?");
        sql.append(" ORDER BY s.statement_date DESC, t.transaction_date, t.id");
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setString(index++, alias);
            if (year != null) ps.setString(index++, String.valueOf(year));
            if (month != null) ps.setString(index++, String.format("%02d", month));
            try (ResultSet rs = ps.executeQuery()) {
                List<MortgageTransaction> rows = new ArrayList<>();
                while (rs.next()) rows.add(map(rs));
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar movimientos de hipoteca", exception);
        }
    }

    public void update(MortgageTransaction row) {
        String sql = """
            UPDATE mortgage_transactions SET transaction_date=?, description=?, total=?, principal=?, interest=?, escrow=?, fees=?,
                unapplied=?, corporate_advance=?, other=?, review_required=?, pending_review=?, review_notes=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date(row.getTransactionDate()));
            ps.setString(2, row.getDescription());
            ps.setDouble(3, row.getTotal());
            ps.setDouble(4, row.getPrincipal());
            ps.setDouble(5, row.getInterest());
            ps.setDouble(6, row.getEscrow());
            ps.setDouble(7, row.getFees());
            ps.setDouble(8, row.getUnapplied());
            ps.setDouble(9, row.getCorporateAdvance());
            ps.setDouble(10, row.getOther());
            ps.setInt(11, row.isReviewRequired() ? 1 : 0);
            ps.setInt(12, row.isPendingReview() ? 1 : 0);
            ps.setString(13, row.getReviewNotes());
            ps.setLong(14, row.getId());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar movimiento de hipoteca", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement("DELETE FROM mortgage_transactions WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar movimiento de hipoteca", exception);
        }
    }

    private void bind(PreparedStatement ps, long statementId, MortgageTransaction row) throws SQLException {
        ps.setLong(1, statementId);
        ps.setString(2, date(row.getTransactionDate()));
        ps.setString(3, row.getDescription());
        ps.setDouble(4, row.getTotal());
        ps.setDouble(5, row.getPrincipal());
        ps.setDouble(6, row.getInterest());
        ps.setDouble(7, row.getEscrow());
        ps.setDouble(8, row.getFees());
        ps.setDouble(9, row.getUnapplied());
        ps.setDouble(10, row.getCorporateAdvance());
        ps.setDouble(11, row.getOther());
        ps.setInt(12, row.isReviewRequired() ? 1 : 0);
        ps.setInt(13, row.isPendingReview() ? 1 : 0);
        ps.setString(14, row.getReviewNotes());
    }

    private MortgageTransaction map(ResultSet rs) throws SQLException {
        MortgageTransaction row = new MortgageTransaction(
            rs.getLong("id"),
            rs.getLong("statement_id"),
            localDate(rs.getString("transaction_date")),
            rs.getString("description"),
            rs.getDouble("total"),
            rs.getDouble("principal"),
            rs.getDouble("interest"),
            rs.getDouble("escrow"),
            rs.getDouble("fees"),
            rs.getDouble("unapplied"),
            rs.getDouble("corporate_advance"),
            rs.getDouble("other")
        );
        row.setReviewRequired(rs.getInt("review_required") == 1);
        row.setPendingReview(rs.getInt("pending_review") == 1);
        row.setReviewNotes(rs.getString("review_notes"));
        return row;
    }

    private String date(LocalDate date) { return date == null ? null : date.toString(); }
    private LocalDate localDate(String value) { return value == null ? null : LocalDate.parse(value); }
}
