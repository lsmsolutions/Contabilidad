package com.silveira.accounting.repositories;

import com.silveira.accounting.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReviewMarkRepository {
    private final DatabaseManager databaseManager;

    public ReviewMarkRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean isMarked(String source, String accountAlias, int year, int month) {
        String sql = "SELECT marked FROM review_marks WHERE source=? AND account_alias=? AND year=? AND month=?";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindKey(statement, source, accountAlias, year, month);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt("marked") == 1;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar la marca de segunda revision", exception);
        }
    }

    public boolean toggle(String source, String accountAlias, int year, int month) {
        boolean marked = !isMarked(source, accountAlias, year, month);
        String sql = """
            INSERT INTO review_marks(source, account_alias, year, month, marked, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(source, account_alias, year, month)
            DO UPDATE SET marked=excluded.marked, updated_at=CURRENT_TIMESTAMP
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindKey(statement, source, accountAlias, year, month);
            statement.setInt(5, marked ? 1 : 0);
            statement.executeUpdate();
            return marked;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar la marca de segunda revision", exception);
        }
    }

    private void bindKey(PreparedStatement statement, String source, String accountAlias, int year, int month) throws SQLException {
        statement.setString(1, clean(source));
        statement.setString(2, clean(accountAlias));
        statement.setInt(3, year);
        statement.setInt(4, month);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
