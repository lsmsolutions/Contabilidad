package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.SourceTotals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BankTransactionRepository {
    private final DatabaseManager databaseManager;

    public BankTransactionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public int saveAll(List<BankTransaction> transactions) {
        String sql = """
            INSERT OR IGNORE INTO bank_transactions
            (transaction_date, description, amount, movement_type, provider, reference, month, year, source_pdf, account_alias, import_status, review_required, pending_review, review_notes, fingerprint, reconciled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        int inserted = 0;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (BankTransaction transaction : transactions) {
                statement.setString(1, transaction.getDate().toString());
                statement.setString(2, transaction.getDescription());
                statement.setDouble(3, transaction.getAmount());
                statement.setString(4, transaction.getMovementType());
                statement.setString(5, transaction.getProvider());
                statement.setString(6, transaction.getReference());
                statement.setInt(7, transaction.getDate().getMonthValue());
                statement.setInt(8, transaction.getDate().getYear());
                statement.setString(9, transaction.getSourcePdf());
                statement.setString(10, transaction.getAccountAlias());
                statement.setString(11, transaction.getImportStatus());
                statement.setInt(12, transaction.isReviewRequired() ? 1 : 0);
                statement.setInt(13, transaction.isPendingReview() ? 1 : 0);
                statement.setString(14, transaction.getReviewNotes());
                statement.setString(15, transaction.getFingerprint());
                statement.setInt(16, transaction.isReconciled() ? 1 : 0);
                inserted += statement.executeUpdate();
            }
            connection.commit();
            return inserted;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar transacciones bancarias", exception);
        }
    }

    public long save(BankTransaction transaction) {
        String sql = """
            INSERT INTO bank_transactions
            (transaction_date, description, amount, movement_type, provider, reference, month, year, source_pdf, account_alias, import_status, review_required, pending_review, review_notes, fingerprint, reconciled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transaction.getDate().toString());
            statement.setString(2, transaction.getDescription());
            statement.setDouble(3, transaction.getAmount());
            statement.setString(4, transaction.getMovementType());
            statement.setString(5, transaction.getProvider());
            statement.setString(6, transaction.getReference());
            statement.setInt(7, transaction.getDate().getMonthValue());
            statement.setInt(8, transaction.getDate().getYear());
            statement.setString(9, transaction.getSourcePdf());
            statement.setString(10, transaction.getAccountAlias());
            statement.setString(11, transaction.getImportStatus());
            statement.setInt(12, transaction.isReviewRequired() ? 1 : 0);
            statement.setInt(13, transaction.isPendingReview() ? 1 : 0);
            statement.setString(14, transaction.getReviewNotes());
            statement.setString(15, transaction.getFingerprint());
            statement.setInt(16, transaction.isReconciled() ? 1 : 0);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar transaccion bancaria", exception);
        }
    }

    public void updateProvider(long id, String provider, String movementType) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE bank_transactions SET provider=?, movement_type=? WHERE id=?")) {
            statement.setString(1, provider);
            statement.setString(2, movementType);
            statement.setLong(3, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar la transaccion", exception);
        }
    }

    public void updateRecord(BankTransaction transaction) {
        String sql = """
            UPDATE bank_transactions
            SET transaction_date=?, description=?, amount=?, movement_type=?, provider=?, reference=?,
                month=?, year=?, source_pdf=?, account_alias=?,
                pending_review=?, review_required=?, review_notes=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transaction.getDate().toString());
            statement.setString(2, transaction.getDescription());
            statement.setDouble(3, transaction.getAmount());
            statement.setString(4, transaction.getMovementType());
            statement.setString(5, transaction.getProvider());
            statement.setString(6, transaction.getReference());
            statement.setInt(7, transaction.getDate().getMonthValue());
            statement.setInt(8, transaction.getDate().getYear());
            statement.setString(9, transaction.getSourcePdf());
            statement.setString(10, transaction.getAccountAlias());
            statement.setInt(11, transaction.isPendingReview() ? 1 : 0);
            statement.setInt(12, transaction.isReviewRequired() ? 1 : 0);
            statement.setString(13, transaction.getReviewNotes());
            statement.setLong(14, transaction.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar transaccion bancaria", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement reconciliations = connection.prepareStatement("DELETE FROM reconciliations WHERE bank_transaction_id=?");
             PreparedStatement transaction = connection.prepareStatement("DELETE FROM bank_transactions WHERE id=?")) {
            reconciliations.setLong(1, id);
            reconciliations.executeUpdate();
            transaction.setLong(1, id);
            transaction.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar transaccion bancaria", exception);
        }
    }

    public List<BankTransaction> find(Integer year, Integer month, String provider, String type) {
        return find(year, month, provider, type, null);
    }

    public List<BankTransaction> find(Integer year, Integer month, String provider, String type, String accountAlias) {
        StringBuilder sql = new StringBuilder("SELECT * FROM bank_transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (year != null) {
            sql.append(" AND year=?");
            params.add(year);
        }
        if (month != null) {
            sql.append(" AND month=?");
            params.add(month);
        }
        if (provider != null && !provider.isBlank()) {
            sql.append(" AND provider LIKE ?");
            params.add("%" + provider + "%");
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND movement_type=?");
            params.add(type);
        }
        if (accountAlias != null && !accountAlias.isBlank()) {
            sql.append(" AND account_alias=?");
            params.add(accountAlias);
        }
        sql.append(" ORDER BY transaction_date DESC, id DESC");
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<BankTransaction> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(map(rs));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar transacciones bancarias", exception);
        }
    }

    public List<BankTransaction> findPendingReview() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM bank_transactions WHERE pending_review=1 ORDER BY year, month, transaction_date, id");
             ResultSet rs = statement.executeQuery()) {
            List<BankTransaction> results = new ArrayList<>();
            while (rs.next()) {
                results.add(map(rs));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar pendientes bancarios", exception);
        }
    }

    public java.util.Set<String> existingFingerprints(List<BankTransaction> transactions) {
        java.util.Set<String> requested = new java.util.HashSet<>();
        for (BankTransaction transaction : transactions) {
            requested.add(transaction.getFingerprint());
        }
        java.util.Set<String> existing = new java.util.HashSet<>();
        if (requested.isEmpty()) {
            return existing;
        }
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT fingerprint FROM bank_transactions WHERE fingerprint=?")) {
            for (String fingerprint : requested) {
                statement.setString(1, fingerprint);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        existing.add(fingerprint);
                    }
                }
            }
            return existing;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron revisar duplicados bancarios", exception);
        }
    }

    public double sum(String sqlWhere, Object... params) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM bank_transactions WHERE pending_review=0 AND " + sqlWhere)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo calcular total bancario", exception);
        }
    }

    public SourceTotals totals(Integer year, Integer month) {
        return totals(year, month, null);
    }

    public SourceTotals totals(Integer year, Integer month, String accountAlias) {
        String where = "1=1";
        List<Object> params = new ArrayList<>();
        if (year != null) {
            where += " AND year=?";
            params.add(year);
        }
        if (month != null) {
            where += " AND month=?";
            params.add(month);
        }
        if (accountAlias != null && !accountAlias.isBlank()) {
            where += " AND account_alias=?";
            params.add(accountAlias);
        }
        String sql = "SELECT "
            + "SUM(CASE WHEN pending_review=0 THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN pending_review=1 THEN 1 ELSE 0 END), "
            + "COALESCE(SUM(CASE WHEN pending_review=0 AND amount>0 THEN amount ELSE 0 END),0), "
            + "COALESCE(SUM(CASE WHEN pending_review=0 AND amount<0 THEN amount ELSE 0 END),0) "
            + "FROM bank_transactions WHERE " + where;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return new SourceTotals(0, 0, 0, 0, 0);
                }
                double income = rs.getDouble(3);
                double expenses = rs.getDouble(4);
                return new SourceTotals(rs.getInt(1), rs.getInt(2), income, expenses, income + expenses);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron calcular totales bancarios", exception);
        }
    }

    public List<MonthlySourceTotals> monthlyTotals(Integer year) {
        return monthlyTotals(year, null);
    }

    public List<MonthlySourceTotals> monthlyTotals(Integer year, String accountAlias) {
        String sql = """
            SELECT year, month,
                   SUM(CASE WHEN pending_review=0 THEN 1 ELSE 0 END) AS reviewed_count,
                   SUM(CASE WHEN pending_review=1 THEN 1 ELSE 0 END) AS pending_count,
                   COALESCE(SUM(CASE WHEN pending_review=0 AND amount>0 THEN amount ELSE 0 END),0) AS deposits,
                   COALESCE(SUM(CASE WHEN pending_review=0 AND amount<0 THEN ABS(amount) ELSE 0 END),0) AS withdrawals
            FROM bank_transactions
            WHERE (? IS NULL OR year=?)
              AND (? IS NULL OR account_alias=?)
            GROUP BY year, month
            ORDER BY year, month
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (year == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, year);
                statement.setInt(2, year);
            }
            if (accountAlias == null || accountAlias.isBlank()) {
                statement.setNull(3, java.sql.Types.VARCHAR);
                statement.setNull(4, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, accountAlias);
                statement.setString(4, accountAlias);
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<MonthlySourceTotals> totals = new ArrayList<>();
                while (rs.next()) {
                    double deposits = rs.getDouble("deposits");
                    double withdrawals = rs.getDouble("withdrawals");
                    totals.add(new MonthlySourceTotals(
                        rs.getInt("year"),
                        rs.getInt("month"),
                        rs.getInt("reviewed_count"),
                        rs.getInt("pending_count"),
                        deposits,
                        withdrawals,
                        deposits - withdrawals
                    ));
                }
                return totals;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron calcular totales mensuales bancarios", exception);
        }
    }

    private BankTransaction map(ResultSet rs) throws SQLException {
        BankTransaction transaction = new BankTransaction(
            rs.getLong("id"),
            LocalDate.parse(rs.getString("transaction_date")),
            rs.getString("description"),
            rs.getDouble("amount"),
            rs.getString("movement_type"),
            rs.getString("provider"),
            rs.getString("reference"),
            rs.getInt("month"),
            rs.getInt("year"),
            rs.getString("source_pdf"),
            rs.getString("fingerprint"),
            rs.getInt("reconciled") == 1
        );
        transaction.setImportStatus(rs.getString("import_status"));
        transaction.setAccountAlias(rs.getString("account_alias"));
        transaction.setReviewRequired(rs.getInt("review_required") == 1);
        transaction.setPendingReview(rs.getInt("pending_review") == 1);
        transaction.setReviewNotes(rs.getString("review_notes"));
        return transaction;
    }
}
