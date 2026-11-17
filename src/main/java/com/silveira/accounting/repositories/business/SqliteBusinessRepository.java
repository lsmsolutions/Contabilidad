package com.silveira.accounting.repositories.business;

import com.silveira.accounting.application.business.BusinessGateway;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.business.Business;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteBusinessRepository implements BusinessGateway {
    private final DatabaseManager database;
    public SqliteBusinessRepository(DatabaseManager database) { this.database = database; }

    @Override public List<Business> findAll() {
        List<Business> result = new ArrayList<>();
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, name FROM businesses ORDER BY name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new Business(rs.getLong("id"), rs.getString("name")));
            return result;
        } catch (SQLException e) { throw new IllegalStateException("Could not load businesses", e); }
    }

    @Override public long create(String name) {
        if (name.isBlank()) throw new IllegalArgumentException("Business name is required");
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO businesses(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getLong(1) : 0; }
        } catch (SQLException e) { throw new IllegalStateException("Could not create business. The name may already exist.", e); }
    }

    @Override public void rename(long id, String name) {
        if (name.isBlank()) throw new IllegalArgumentException("Business name is required");
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE businesses SET name=? WHERE id=?")) {
            ps.setString(1, name); ps.setLong(2, id); ps.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Could not rename business. The name may already exist.", e); }
    }

    @Override public void delete(long id) {
        try (Connection c = database.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement expenses = c.prepareStatement("DELETE FROM business_expenses WHERE business_id=?");
                 PreparedStatement business = c.prepareStatement("DELETE FROM businesses WHERE id=?")) {
                expenses.setLong(1, id); expenses.executeUpdate();
                business.setLong(1, id); business.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) { throw new IllegalStateException("Could not delete business", e); }
    }
}
