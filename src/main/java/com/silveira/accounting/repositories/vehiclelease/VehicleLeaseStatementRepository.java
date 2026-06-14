package com.silveira.accounting.repositories.vehiclelease;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VehicleLeaseStatementRepository {
    private final DatabaseManager databaseManager;

    public VehicleLeaseStatementRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public long save(VehicleLeaseStatement value) {
        String sql = """
            INSERT INTO vehicle_lease_statements(
                account_alias, statement_date, due_date, total_amount_due, last_payment_date, last_payment_amount,
                payments_made, payments_remaining, lease_payment, sales_use_tax, property_tax, parking_tickets,
                returned_check_fees, miscellaneous_charges, past_due_amount, late_charges, source_pdf_path,
                review_required, pending_review, review_notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(account_alias, statement_date) DO UPDATE SET
                due_date=excluded.due_date,
                total_amount_due=excluded.total_amount_due,
                last_payment_date=excluded.last_payment_date,
                last_payment_amount=excluded.last_payment_amount,
                payments_made=excluded.payments_made,
                payments_remaining=excluded.payments_remaining,
                lease_payment=excluded.lease_payment,
                sales_use_tax=excluded.sales_use_tax,
                property_tax=excluded.property_tax,
                parking_tickets=excluded.parking_tickets,
                returned_check_fees=excluded.returned_check_fees,
                miscellaneous_charges=excluded.miscellaneous_charges,
                past_due_amount=excluded.past_due_amount,
                late_charges=excluded.late_charges,
                source_pdf_path=excluded.source_pdf_path
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, value);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return findId(connection, value.getAccountAlias(), value.getStatementDate());
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar el estado de leasing", exception);
        }
    }

    public void update(VehicleLeaseStatement value) {
        String sql = """
            UPDATE vehicle_lease_statements SET
                statement_date=?, due_date=?, total_amount_due=?, last_payment_date=?, last_payment_amount=?,
                payments_made=?, payments_remaining=?, lease_payment=?, sales_use_tax=?, property_tax=?, parking_tickets=?,
                returned_check_fees=?, miscellaneous_charges=?, past_due_amount=?, late_charges=?,
                review_required=?, pending_review=?, review_notes=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            setDate(statement, 1, value.getStatementDate());
            setDate(statement, 2, value.getDueDate());
            statement.setDouble(3, value.getTotalAmountDue());
            setDate(statement, 4, value.getLastPaymentDate());
            statement.setDouble(5, value.getLastPaymentAmount());
            statement.setInt(6, value.getPaymentsMade());
            statement.setInt(7, value.getPaymentsRemaining());
            statement.setDouble(8, value.getLeasePayment());
            statement.setDouble(9, value.getSalesUseTax());
            statement.setDouble(10, value.getPropertyTax());
            statement.setDouble(11, value.getParkingTickets());
            statement.setDouble(12, value.getReturnedCheckFees());
            statement.setDouble(13, value.getMiscellaneousCharges());
            statement.setDouble(14, value.getPastDueAmount());
            statement.setDouble(15, value.getLateCharges());
            statement.setInt(16, value.isReviewRequired() ? 1 : 0);
            statement.setInt(17, value.isPendingReview() ? 1 : 0);
            statement.setString(18, value.getReviewNotes());
            statement.setLong(19, value.getId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar el estado de leasing", exception);
        }
    }

    public List<VehicleLeaseStatement> findByAccount(String alias) {
        String sql = "SELECT * FROM vehicle_lease_statements WHERE account_alias=? ORDER BY statement_date DESC, id DESC";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, alias);
            try (ResultSet result = statement.executeQuery()) {
                List<VehicleLeaseStatement> values = new ArrayList<>();
                while (result.next()) {
                    values.add(map(result));
                }
                return values;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar los estados de leasing", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement reviews = connection.prepareStatement("DELETE FROM vehicle_lease_statement_field_reviews WHERE statement_id=?");
                 PreparedStatement statement = connection.prepareStatement("DELETE FROM vehicle_lease_statements WHERE id=?")) {
                reviews.setLong(1, id);
                reviews.executeUpdate();
                statement.setLong(1, id);
                statement.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar el estado de leasing", exception);
        }
    }

    private void bind(PreparedStatement statement, VehicleLeaseStatement value) throws SQLException {
        statement.setString(1, value.getAccountAlias());
        setDate(statement, 2, value.getStatementDate());
        setDate(statement, 3, value.getDueDate());
        statement.setDouble(4, value.getTotalAmountDue());
        setDate(statement, 5, value.getLastPaymentDate());
        statement.setDouble(6, value.getLastPaymentAmount());
        statement.setInt(7, value.getPaymentsMade());
        statement.setInt(8, value.getPaymentsRemaining());
        statement.setDouble(9, value.getLeasePayment());
        statement.setDouble(10, value.getSalesUseTax());
        statement.setDouble(11, value.getPropertyTax());
        statement.setDouble(12, value.getParkingTickets());
        statement.setDouble(13, value.getReturnedCheckFees());
        statement.setDouble(14, value.getMiscellaneousCharges());
        statement.setDouble(15, value.getPastDueAmount());
        statement.setDouble(16, value.getLateCharges());
        statement.setString(17, value.getSourcePdfPath());
        statement.setInt(18, value.isReviewRequired() ? 1 : 0);
        statement.setInt(19, value.isPendingReview() ? 1 : 0);
        statement.setString(20, value.getReviewNotes());
    }

    private long findId(Connection connection, String alias, LocalDate statementDate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id FROM vehicle_lease_statements WHERE account_alias=? AND statement_date=?")) {
            statement.setString(1, alias);
            statement.setString(2, statementDate.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getLong(1);
                }
            }
        }
        throw new SQLException("No se encontro el estado guardado");
    }

    private VehicleLeaseStatement map(ResultSet result) throws SQLException {
        VehicleLeaseStatement value = new VehicleLeaseStatement();
        value.setId(result.getLong("id"));
        value.setAccountAlias(result.getString("account_alias"));
        value.setStatementDate(date(result.getString("statement_date")));
        value.setDueDate(date(result.getString("due_date")));
        value.setTotalAmountDue(result.getDouble("total_amount_due"));
        value.setLastPaymentDate(date(result.getString("last_payment_date")));
        value.setLastPaymentAmount(result.getDouble("last_payment_amount"));
        value.setPaymentsMade(result.getInt("payments_made"));
        value.setPaymentsRemaining(result.getInt("payments_remaining"));
        value.setLeasePayment(result.getDouble("lease_payment"));
        value.setSalesUseTax(result.getDouble("sales_use_tax"));
        value.setPropertyTax(result.getDouble("property_tax"));
        value.setParkingTickets(result.getDouble("parking_tickets"));
        value.setReturnedCheckFees(result.getDouble("returned_check_fees"));
        value.setMiscellaneousCharges(result.getDouble("miscellaneous_charges"));
        value.setPastDueAmount(result.getDouble("past_due_amount"));
        value.setLateCharges(result.getDouble("late_charges"));
        value.setSourcePdfPath(result.getString("source_pdf_path"));
        value.setReviewRequired(result.getInt("review_required") != 0);
        value.setPendingReview(result.getInt("pending_review") != 0);
        value.setReviewNotes(result.getString("review_notes"));
        return value;
    }

    private void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.toString());
        }
    }

    private LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
