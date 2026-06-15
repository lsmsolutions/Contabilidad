package com.silveira.accounting.repositories.mortgage;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.HouseExpense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class HouseExpenseRepository {
    private final DatabaseManager databaseManager;

    public HouseExpenseRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public long save(HouseExpense expense) {
        String sql = """
            INSERT INTO house_expenses(loan_alias, expense_date, description, provider, amount, invoice, payment_source, notes, document_path, document_name, review_required, pending_review)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, expense);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar gasto de casa", exception);
        }
    }

    public List<HouseExpense> findByLoan(String loanAlias, Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("SELECT * FROM house_expenses WHERE 1=1");
        boolean filterLoan = loanAlias != null && !loanAlias.isBlank();
        if (filterLoan) sql.append(" AND loan_alias=?");
        LocalDate startDate = null;
        LocalDate endDate = null;
        boolean filterMonthOnly = year == null && month != null;
        if (year != null && month != null) {
            YearMonth period = YearMonth.of(year, month);
            startDate = period.atDay(1);
            endDate = period.plusMonths(1).atDay(1);
        } else if (year != null) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = startDate.plusYears(1);
        }
        if (startDate != null) sql.append(" AND expense_date>=?");
        if (endDate != null) sql.append(" AND expense_date<?");
        if (filterMonthOnly) sql.append(" AND substr(expense_date, 6, 2)=?");
        sql.append(" ORDER BY expense_date DESC, id DESC");
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (filterLoan) ps.setString(index++, loanAlias);
            if (startDate != null) ps.setString(index++, startDate.toString());
            if (endDate != null) ps.setString(index++, endDate.toString());
            if (filterMonthOnly) ps.setString(index++, String.format("%02d", month));
            try (ResultSet rs = ps.executeQuery()) {
                List<HouseExpense> rows = new ArrayList<>();
                while (rs.next()) rows.add(map(rs));
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar gastos de casa", exception);
        }
    }

    public void update(HouseExpense expense) {
        String sql = """
            UPDATE house_expenses SET loan_alias=?, expense_date=?, description=?, provider=?, amount=?, invoice=?, payment_source=?, notes=?, document_path=?, document_name=?,
                review_required=?, pending_review=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, expense);
            ps.setLong(13, expense.getId());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar gasto de casa", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement("DELETE FROM house_expenses WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar gasto de casa", exception);
        }
    }

    private void bind(PreparedStatement ps, HouseExpense expense) throws SQLException {
        ps.setString(1, expense.getLoanAlias());
        ps.setString(2, date(expense.getExpenseDate()));
        ps.setString(3, expense.getDescription());
        ps.setString(4, expense.getProvider());
        ps.setDouble(5, expense.getAmount());
        ps.setString(6, expense.getInvoice());
        ps.setString(7, expense.getPaymentSource());
        ps.setString(8, expense.getNotes());
        ps.setString(9, expense.getDocumentPath());
        ps.setString(10, expense.getDocumentName());
        ps.setInt(11, expense.isReviewRequired() ? 1 : 0);
        ps.setInt(12, expense.isPendingReview() ? 1 : 0);
    }

    private HouseExpense map(ResultSet rs) throws SQLException {
        HouseExpense expense = new HouseExpense(
            rs.getLong("id"),
            rs.getString("loan_alias"),
            localDate(rs.getString("expense_date")),
            rs.getString("description"),
            rs.getString("provider"),
            rs.getDouble("amount"),
            rs.getString("invoice"),
            rs.getString("notes")
        );
        expense.setPaymentSource(rs.getString("payment_source"));
        expense.setDocumentPath(rs.getString("document_path"));
        expense.setDocumentName(rs.getString("document_name"));
        expense.setReviewRequired(rs.getInt("review_required") == 1);
        expense.setPendingReview(rs.getInt("pending_review") == 1);
        return expense;
    }

    private String date(LocalDate date) { return date == null ? null : date.toString(); }
    private LocalDate localDate(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
}
