package com.silveira.accounting.repositories.business;

import com.silveira.accounting.application.business.BusinessExpenseGateway;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.business.BusinessExpense;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SqliteBusinessExpenseRepository implements BusinessExpenseGateway {
    private final DatabaseManager database;
    public SqliteBusinessExpenseRepository(DatabaseManager database) { this.database = database; }

    @Override public List<BusinessExpense> find(long businessId, Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("SELECT * FROM business_expenses WHERE business_id=?");
        if (year != null) sql.append(" AND substr(expense_date,1,4)=?");
        if (month != null) sql.append(" AND substr(expense_date,6,2)=?");
        sql.append(" ORDER BY expense_date DESC, id DESC");
        List<BusinessExpense> result = new ArrayList<>();
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1; ps.setLong(i++, businessId);
            if (year != null) ps.setString(i++, String.format("%04d", year));
            if (month != null) ps.setString(i, String.format("%02d", month));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(map(rs)); }
            return result;
        } catch (SQLException e) { throw new IllegalStateException("Could not load business expenses", e); }
    }

    @Override public long save(BusinessExpense e) {
        String sql = "INSERT INTO business_expenses(business_id,expense_date,description,provider,amount,invoice,payment_source,reviewed,notes,document_path,document_name) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, e); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { return keys.next() ? keys.getLong(1) : 0; }
        } catch (SQLException ex) { throw new IllegalStateException("Could not save business expense", ex); }
    }

    @Override public void update(BusinessExpense e) {
        String sql = "UPDATE business_expenses SET business_id=?,expense_date=?,description=?,provider=?,amount=?,invoice=?,payment_source=?,reviewed=?,notes=?,document_path=?,document_name=? WHERE id=?";
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, e); ps.setLong(12, e.getId()); ps.executeUpdate();
        } catch (SQLException ex) { throw new IllegalStateException("Could not update business expense", ex); }
    }

    @Override public void delete(long id) {
        try (Connection c = database.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM business_expenses WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Could not delete business expense", e); }
    }

    private void bind(PreparedStatement ps, BusinessExpense e) throws SQLException {
        ps.setLong(1, e.getBusinessId()); ps.setString(2, e.getExpenseDate() == null ? null : e.getExpenseDate().toString());
        ps.setString(3, e.getDescription()); ps.setString(4, e.getProvider()); ps.setDouble(5, e.getAmount());
        ps.setString(6, e.getInvoice()); ps.setString(7, e.getPaymentSource()); ps.setInt(8, e.isReviewed() ? 1 : 0);
        ps.setString(9, e.getNotes()); ps.setString(10, e.getDocumentPath()); ps.setString(11, e.getDocumentName());
    }

    private BusinessExpense map(ResultSet rs) throws SQLException {
        String date = rs.getString("expense_date");
        return new BusinessExpense(rs.getLong("id"), rs.getLong("business_id"), date == null ? null : LocalDate.parse(date), rs.getString("description"), rs.getString("provider"), rs.getDouble("amount"), rs.getString("invoice"), rs.getString("payment_source"), rs.getInt("reviewed") != 0, rs.getString("notes"), rs.getString("document_path"), rs.getString("document_name"));
    }
}
