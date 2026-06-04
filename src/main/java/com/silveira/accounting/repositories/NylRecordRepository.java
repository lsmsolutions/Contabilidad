package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.SourceTotals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NylRecordRepository {
    private final DatabaseManager databaseManager;

    public NylRecordRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public SaveResult saveAll(List<NylRecord> records) {
        String sql = """
            INSERT OR IGNORE INTO nyl_records
            (year, month, concept, section, record_type, amount, source_pdf, import_status, review_required, pending_review, review_notes, fingerprint)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        int inserted = 0;
        Set<String> newConcepts = new HashSet<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement insert = connection.prepareStatement(sql);
             PreparedStatement closedCheck = connection.prepareStatement("SELECT 1 FROM closed_months WHERE source='NYL' AND year=? AND month=?");
             PreparedStatement conceptCheck = connection.prepareStatement("SELECT 1 FROM known_nyl_concepts WHERE concept=?");
             PreparedStatement conceptInsert = connection.prepareStatement("INSERT OR IGNORE INTO known_nyl_concepts(concept) VALUES (?)")) {
            connection.setAutoCommit(false);
            for (NylRecord record : records) {
                closedCheck.setInt(1, record.getYear());
                closedCheck.setInt(2, record.getMonth());
                try (ResultSet closed = closedCheck.executeQuery()) {
                    if (closed.next()) {
                        throw new IllegalStateException("El mes " + record.getMonth() + "/" + record.getYear() + " de NYL esta cerrado. No se sobrescribe.");
                    }
                }
                conceptCheck.setString(1, record.getConcept());
                try (ResultSet rs = conceptCheck.executeQuery()) {
                    if (!rs.next()) {
                        newConcepts.add(record.getConcept());
                    }
                }
                conceptInsert.setString(1, record.getConcept());
                conceptInsert.executeUpdate();

                insert.setInt(1, record.getYear());
                insert.setInt(2, record.getMonth());
                insert.setString(3, record.getConcept());
                insert.setString(4, record.getSection());
                insert.setString(5, record.getRecordType());
                insert.setDouble(6, record.getAmount());
                insert.setString(7, record.getSourcePdf());
                insert.setString(8, record.getImportStatus());
                insert.setInt(9, record.isReviewRequired() ? 1 : 0);
                insert.setInt(10, record.isPendingReview() ? 1 : 0);
                insert.setString(11, record.getReviewNotes());
                insert.setString(12, record.getFingerprint());
                inserted += insert.executeUpdate();
            }
            connection.commit();
            return new SaveResult(inserted, newConcepts);
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron guardar registros NYL", exception);
        }
    }

    public void recordCorrection(String fingerprint, String field, String oldValue, String newValue, String reason) {
        String sql = "INSERT INTO correction_history(entity, fingerprint, field_name, old_value, new_value, reason) VALUES ('NYL', ?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fingerprint);
            statement.setString(2, field);
            statement.setString(3, oldValue);
            statement.setString(4, newValue);
            statement.setString(5, reason);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo registrar historial de correccion", exception);
        }
    }

    public void updateReviewState(long id, boolean pendingReview, boolean reviewRequired, String notes) {
        String sql = "UPDATE nyl_records SET pending_review=?, review_required=?, review_notes=? WHERE id=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pendingReview ? 1 : 0);
            statement.setInt(2, reviewRequired ? 1 : 0);
            statement.setString(3, notes);
            statement.setLong(4, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar estado de revision", exception);
        }
    }

    public void updateRecord(NylRecord record) {
        String sql = """
            UPDATE nyl_records
            SET year=?, month=?, concept=?, section=?, record_type=?, amount=?, pending_review=?, review_required=?, review_notes=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, record.getYear());
            statement.setInt(2, record.getMonth());
            statement.setString(3, record.getConcept());
            statement.setString(4, record.getSection());
            statement.setString(5, record.getRecordType());
            statement.setDouble(6, record.getAmount());
            statement.setInt(7, record.isPendingReview() ? 1 : 0);
            statement.setInt(8, record.isReviewRequired() ? 1 : 0);
            statement.setString(9, record.getReviewNotes());
            statement.setLong(10, record.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar registro NYL", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement reconciliations = connection.prepareStatement("DELETE FROM reconciliations WHERE nyl_record_id=?");
             PreparedStatement record = connection.prepareStatement("DELETE FROM nyl_records WHERE id=?")) {
            reconciliations.setLong(1, id);
            reconciliations.executeUpdate();
            record.setLong(1, id);
            record.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar registro NYL", exception);
        }
    }

    public List<NylRecord> findPendingReview() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM nyl_records WHERE pending_review=1 ORDER BY year, month, section, concept");
             ResultSet rs = statement.executeQuery()) {
            List<NylRecord> results = new ArrayList<>();
            while (rs.next()) {
                results.add(map(rs));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar pendientes NYL", exception);
        }
    }

    public List<NylRecord> find(Integer year, Integer month, String concept, String type) {
        StringBuilder sql = new StringBuilder("SELECT * FROM nyl_records WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (year != null) {
            sql.append(" AND year=?");
            params.add(year);
        }
        if (month != null) {
            sql.append(" AND month=?");
            params.add(month);
        }
        if (concept != null && !concept.isBlank()) {
            sql.append(" AND concept LIKE ?");
            params.add("%" + concept + "%");
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND record_type=?");
            params.add(type);
        }
        sql.append(" ORDER BY year DESC, month DESC, concept");
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<NylRecord> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(map(rs));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar registros NYL", exception);
        }
    }

    public List<NylRecord> findByFingerprints(Set<String> fingerprints) {
        if (fingerprints.isEmpty()) {
            return List.of();
        }
        List<NylRecord> records = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM nyl_records WHERE fingerprint=? ORDER BY year, month, section, concept")) {
            for (String fingerprint : fingerprints) {
                statement.setString(1, fingerprint);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        records.add(map(rs));
                    }
                }
            }
            return records;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar registros existentes NYL", exception);
        }
    }

    public Set<String> existingFingerprints(List<NylRecord> records) {
        Set<String> requested = new HashSet<>();
        for (NylRecord record : records) {
            requested.add(record.getFingerprint());
        }
        if (requested.isEmpty()) {
            return Set.of();
        }
        Set<String> existing = new HashSet<>();
        String sql = "SELECT fingerprint FROM nyl_records WHERE fingerprint=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
            throw new IllegalStateException("No se pudieron revisar duplicados NYL", exception);
        }
    }

    public double sum(String sqlWhere, Object... params) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(amount),0) FROM nyl_records WHERE pending_review=0 AND " + sqlWhere)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo calcular total NYL", exception);
        }
    }

    public SourceTotals totals(Integer year, Integer month) {
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
        String sql = "SELECT "
            + "SUM(CASE WHEN pending_review=0 THEN 1 ELSE 0 END), "
            + "SUM(CASE WHEN pending_review=1 THEN 1 ELSE 0 END), "
            + "COALESCE(SUM(CASE WHEN pending_review=0 AND amount>0 THEN amount ELSE 0 END),0), "
            + "COALESCE(SUM(CASE WHEN pending_review=0 AND amount<0 THEN amount ELSE 0 END),0) "
            + "FROM nyl_records WHERE " + where;
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
            throw new IllegalStateException("No se pudieron calcular totales NYL", exception);
        }
    }

    public List<MonthlySourceTotals> monthlyTotals(Integer year) {
        String sql = """
            SELECT year, month,
                   SUM(CASE WHEN pending_review=0 THEN 1 ELSE 0 END) AS reviewed_count,
                   SUM(CASE WHEN pending_review=1 THEN 1 ELSE 0 END) AS pending_count,
                   COALESCE(SUM(CASE WHEN pending_review=0 AND amount>0 THEN amount ELSE 0 END),0) AS credits,
                   COALESCE(SUM(CASE WHEN pending_review=0 AND amount<0 THEN ABS(amount) ELSE 0 END),0) AS deductions
            FROM nyl_records
            WHERE (? IS NULL OR year=?)
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
            try (ResultSet rs = statement.executeQuery()) {
                List<MonthlySourceTotals> totals = new ArrayList<>();
                while (rs.next()) {
                    double credits = rs.getDouble("credits");
                    double deductions = rs.getDouble("deductions");
                    totals.add(new MonthlySourceTotals(
                        rs.getInt("year"),
                        rs.getInt("month"),
                        rs.getInt("reviewed_count"),
                        rs.getInt("pending_count"),
                        credits,
                        deductions,
                        credits - deductions
                    ));
                }
                return totals;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron calcular totales mensuales NYL", exception);
        }
    }

    private NylRecord map(ResultSet rs) throws SQLException {
        return new NylRecord(
            rs.getLong("id"),
            rs.getInt("year"),
            rs.getInt("month"),
            rs.getString("concept"),
            rs.getString("section"),
            rs.getString("record_type"),
            rs.getDouble("amount"),
            rs.getString("source_pdf"),
            rs.getString("fingerprint"),
            rs.getString("import_status"),
            rs.getInt("review_required") == 1,
            rs.getInt("pending_review") == 1,
            rs.getString("review_notes")
        );
    }

    public record SaveResult(int inserted, Set<String> newConcepts) {
    }
}
