package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.InternalMovementRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InternalMovementRepository {
    private final DatabaseManager databaseManager;

    public InternalMovementRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<InternalMovementRecord> findBySource(String sourceType, long sourceId) {
        String sql = "SELECT * FROM internal_movements WHERE source_type=? AND source_id=?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sourceType);
            statement.setLong(2, sourceId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar movimiento interno", exception);
        }
    }

    public List<InternalMovementRecord> findManual(Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("SELECT * FROM internal_movements WHERE manual=1");
        if (year != null) sql.append(" AND substr(movement_date, 1, 4)=?");
        if (month != null) sql.append(" AND substr(movement_date, 6, 2)=?");
        sql.append(" ORDER BY movement_date DESC, id DESC");
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (year != null) statement.setString(index++, String.valueOf(year));
            if (month != null) statement.setString(index++, String.format("%02d", month));
            try (ResultSet rs = statement.executeQuery()) {
                List<InternalMovementRecord> rows = new ArrayList<>();
                while (rs.next()) rows.add(map(rs));
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar movimientos internos manuales", exception);
        }
    }

    public long save(InternalMovementRecord movement) {
        String sql = """
            INSERT INTO internal_movements(source_type, source_id, movement_date, movement_from, movement_to, amount, description, status, reviewed, manual, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(source_type, source_id) DO UPDATE SET
                movement_date=excluded.movement_date,
                movement_from=excluded.movement_from,
                movement_to=excluded.movement_to,
                amount=excluded.amount,
                description=excluded.description,
                status=excluded.status,
                reviewed=excluded.reviewed,
                manual=excluded.manual,
                updated_at=CURRENT_TIMESTAMP
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, movement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return findBySource(movement.getSourceType(), movement.getSourceId()).map(InternalMovementRecord::getId).orElse(0L);
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar movimiento interno", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM internal_movements WHERE id=?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar movimiento interno", exception);
        }
    }

    private void bind(PreparedStatement statement, InternalMovementRecord movement) throws SQLException {
        statement.setString(1, movement.getSourceType());
        statement.setLong(2, movement.getSourceId());
        statement.setString(3, movement.getDate() == null ? null : movement.getDate().toString());
        statement.setString(4, movement.getFrom());
        statement.setString(5, movement.getTo());
        statement.setDouble(6, movement.getAmount());
        statement.setString(7, movement.getDescription());
        statement.setString(8, movement.getStatus());
        statement.setInt(9, movement.isReviewed() ? 1 : 0);
        statement.setInt(10, movement.isManual() ? 1 : 0);
    }

    private InternalMovementRecord map(ResultSet rs) throws SQLException {
        return new InternalMovementRecord(
            rs.getLong("id"),
            rs.getString("source_type"),
            rs.getLong("source_id"),
            date(rs.getString("movement_date")),
            rs.getString("movement_from"),
            rs.getString("movement_to"),
            rs.getDouble("amount"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getInt("reviewed") == 1,
            rs.getInt("manual") == 1
        );
    }

    private LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
