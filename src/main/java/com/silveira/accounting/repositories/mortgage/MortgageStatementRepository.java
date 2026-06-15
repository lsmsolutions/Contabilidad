package com.silveira.accounting.repositories.mortgage;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.SourceTotals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MortgageStatementRepository {
    private final DatabaseManager databaseManager;

    public MortgageStatementRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public long save(MortgageStatement s) {
        saveLoan(s.getLoanAlias(), s.getServicerName(), s.getLoanNumber(), s.getPropertyAddress(), "Detectada al importar o crear hipoteca");
        String sql = """
            INSERT OR IGNORE INTO mortgage_statements
            (loan_alias, servicer_name, statement_date, payment_due_date, payment_amount_due, late_fee_date, late_fee_amount,
             loan_number, property_address, original_principal_balance, outstanding_principal_balance, maturity_date,
             interest_rate, escrow_balance, unapplied_funds, current_payment_due, principal_due, interest_due, escrow_due,
             regular_monthly_payment, past_due_amount, fees, other_fees_and_charges, total_due, source_pdf_path,
             past_paid_principal_since_last_statement, past_paid_principal_year_to_date,
             past_paid_interest_since_last_statement, past_paid_interest_year_to_date,
             past_paid_escrow_since_last_statement, past_paid_escrow_year_to_date,
             past_paid_total_since_last_statement, past_paid_total_year_to_date,
             import_status, review_required, pending_review, review_notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, s);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            Long existing = existingId(connection, s);
            return existing == null ? 0 : existing;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar statement de hipoteca", exception);
        }
    }

    public List<String> findLoanAliases() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement("""
                 SELECT alias FROM mortgage_accounts
                 UNION
                 SELECT DISTINCT loan_alias FROM mortgage_statements
                 ORDER BY 1
                 """);
             ResultSet rs = ps.executeQuery()) {
            List<String> aliases = new ArrayList<>();
            while (rs.next()) aliases.add(rs.getString(1));
            return aliases;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar hipotecas", exception);
        }
    }

    public void saveLoan(String alias, String servicerName, String loanNumber, String propertyAddress, String notes) {
        if (alias == null || alias.isBlank()) {
            return;
        }
        String sql = """
            INSERT OR IGNORE INTO mortgage_accounts(alias, servicer_name, loan_number, property_address, notes)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, alias);
            ps.setString(2, servicerName);
            ps.setString(3, loanNumber);
            ps.setString(4, propertyAddress);
            ps.setString(5, notes);
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar hipoteca", exception);
        }
    }

    public List<MortgageStatement> findByLoan(String alias, Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("SELECT * FROM mortgage_statements WHERE loan_alias=?");
        if (year != null) sql.append(" AND substr(statement_date, 1, 4)=?");
        if (month != null) sql.append(" AND substr(statement_date, 6, 2)=?");
        sql.append(" ORDER BY statement_date DESC, id DESC");
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            ps.setString(index++, alias);
            if (year != null) ps.setString(index++, String.valueOf(year));
            if (month != null) ps.setString(index++, String.format("%02d", month));
            try (ResultSet rs = ps.executeQuery()) {
                List<MortgageStatement> rows = new ArrayList<>();
                while (rs.next()) rows.add(map(rs));
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar statements de hipoteca", exception);
        }
    }

    public List<MortgageStatement> findAll(Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("SELECT * FROM mortgage_statements WHERE 1=1");
        if (year != null) sql.append(" AND substr(statement_date, 1, 4)=?");
        if (month != null) sql.append(" AND substr(statement_date, 6, 2)=?");
        sql.append(" ORDER BY loan_alias, statement_date DESC");
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (year != null) ps.setString(index++, String.valueOf(year));
            if (month != null) ps.setString(index++, String.format("%02d", month));
            try (ResultSet rs = ps.executeQuery()) {
                List<MortgageStatement> rows = new ArrayList<>();
                while (rs.next()) rows.add(map(rs));
                return rows;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar hipotecas", exception);
        }
    }

    public SourceTotals totals(String alias, Integer year, Integer month) {
        List<MortgageStatement> rows = alias == null ? findAll(year, month) : findByLoan(alias, year, month);
        int reviewed = (int) rows.stream().filter(s -> !s.isPendingReview()).count();
        int pending = (int) rows.stream().filter(MortgageStatement::isPendingReview).count();
        double principal = rows.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getPrincipalDue).sum();
        double interest = rows.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getInterestDue).sum();
        double totalDue = rows.stream().filter(s -> !s.isPendingReview()).mapToDouble(s -> s.getTotalDue() > 0 ? s.getTotalDue() : s.getPaymentAmountDue()).sum();
        return new SourceTotals(reviewed, pending, principal, interest, totalDue);
    }

    public List<MonthlySourceTotals> monthlyTotals(String alias, Integer year) {
        return findByLoan(alias, year, null).stream()
            .filter(s -> s.getStatementDate() != null)
            .collect(java.util.stream.Collectors.groupingBy(s -> s.getStatementDate().getYear() + "-" + s.getStatementDate().getMonthValue()))
            .entrySet().stream()
            .map(entry -> {
                List<MortgageStatement> monthRows = entry.getValue();
                MortgageStatement first = monthRows.get(0);
                int reviewed = (int) monthRows.stream().filter(s -> !s.isPendingReview()).count();
                int pending = (int) monthRows.stream().filter(MortgageStatement::isPendingReview).count();
                double principal = monthRows.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getPrincipalDue).sum();
                double interest = monthRows.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getInterestDue).sum();
                double total = monthRows.stream().filter(s -> !s.isPendingReview()).mapToDouble(s -> s.getTotalDue() > 0 ? s.getTotalDue() : s.getPaymentAmountDue()).sum();
                return new MonthlySourceTotals(first.getStatementDate().getYear(), first.getStatementDate().getMonthValue(), reviewed, pending, principal, interest, total);
            })
            .sorted(java.util.Comparator.comparing(MonthlySourceTotals::year).thenComparing(MonthlySourceTotals::month))
            .toList();
    }

    public void updateRecord(MortgageStatement s) {
        String sql = """
            UPDATE mortgage_statements SET servicer_name=?, statement_date=?, payment_due_date=?, payment_amount_due=?, late_fee_date=?,
                late_fee_amount=?, loan_number=?, property_address=?, original_principal_balance=?, outstanding_principal_balance=?,
                maturity_date=?, interest_rate=?, escrow_balance=?, unapplied_funds=?, current_payment_due=?, principal_due=?,
                interest_due=?, escrow_due=?, regular_monthly_payment=?, past_due_amount=?, fees=?, other_fees_and_charges=?,
                total_due=?, past_paid_principal_since_last_statement=?, past_paid_principal_year_to_date=?,
                past_paid_interest_since_last_statement=?, past_paid_interest_year_to_date=?,
                past_paid_escrow_since_last_statement=?, past_paid_escrow_year_to_date=?,
                past_paid_total_since_last_statement=?, past_paid_total_year_to_date=?,
                import_status=?, review_required=?, pending_review=?, review_notes=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s.getServicerName());
            ps.setString(2, date(s.getStatementDate()));
            ps.setString(3, date(s.getPaymentDueDate()));
            ps.setDouble(4, s.getPaymentAmountDue());
            ps.setString(5, date(s.getLateFeeDate()));
            ps.setDouble(6, s.getLateFeeAmount());
            ps.setString(7, s.getLoanNumber());
            ps.setString(8, s.getPropertyAddress());
            ps.setDouble(9, s.getOriginalPrincipalBalance());
            ps.setDouble(10, s.getOutstandingPrincipalBalance());
            ps.setString(11, date(s.getMaturityDate()));
            ps.setDouble(12, s.getInterestRate());
            ps.setDouble(13, s.getEscrowBalance());
            ps.setDouble(14, s.getUnappliedFunds());
            ps.setDouble(15, s.getCurrentPaymentDue());
            ps.setDouble(16, s.getPrincipalDue());
            ps.setDouble(17, s.getInterestDue());
            ps.setDouble(18, s.getEscrowDue());
            ps.setDouble(19, s.getRegularMonthlyPayment());
            ps.setDouble(20, s.getPastDueAmount());
            ps.setDouble(21, s.getFees());
            ps.setDouble(22, s.getOtherFeesAndCharges());
            ps.setDouble(23, s.getTotalDue());
            ps.setDouble(24, s.getPastPaidPrincipalSinceLastStatement());
            ps.setDouble(25, s.getPastPaidPrincipalYearToDate());
            ps.setDouble(26, s.getPastPaidInterestSinceLastStatement());
            ps.setDouble(27, s.getPastPaidInterestYearToDate());
            ps.setDouble(28, s.getPastPaidEscrowSinceLastStatement());
            ps.setDouble(29, s.getPastPaidEscrowYearToDate());
            ps.setDouble(30, s.getPastPaidTotalSinceLastStatement());
            ps.setDouble(31, s.getPastPaidTotalYearToDate());
            ps.setString(32, s.getImportStatus());
            ps.setInt(33, s.isReviewRequired() ? 1 : 0);
            ps.setInt(34, s.isPendingReview() ? 1 : 0);
            ps.setString(35, s.getReviewNotes());
            ps.setLong(36, s.getId());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar hipoteca", exception);
        }
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement alerts = connection.prepareStatement("DELETE FROM mortgage_alerts WHERE statement_id=?");
             PreparedStatement fieldReviews = connection.prepareStatement("DELETE FROM mortgage_statement_field_reviews WHERE statement_id=?");
             PreparedStatement transactions = connection.prepareStatement("DELETE FROM mortgage_transactions WHERE statement_id=?");
             PreparedStatement statement = connection.prepareStatement("DELETE FROM mortgage_statements WHERE id=?")) {
            alerts.setLong(1, id);
            alerts.executeUpdate();
            fieldReviews.setLong(1, id);
            fieldReviews.executeUpdate();
            transactions.setLong(1, id);
            transactions.executeUpdate();
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar hipoteca", exception);
        }
    }

    public void deleteLoan(String alias) {
        for (MortgageStatement statement : findByLoan(alias, null, null)) {
            delete(statement.getId());
        }
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement("DELETE FROM mortgage_accounts WHERE alias=?")) {
            ps.setString(1, alias);
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar hipoteca", exception);
        }
    }

    public void renameLoan(String oldAlias, String newAlias) {
        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement account = connection.prepareStatement("UPDATE mortgage_accounts SET alias=? WHERE alias=?");
                 PreparedStatement statements = connection.prepareStatement("UPDATE mortgage_statements SET loan_alias=? WHERE loan_alias=?")) {
                account.setString(1, newAlias);
                account.setString(2, oldAlias);
                account.executeUpdate();
                statements.setString(1, newAlias);
                statements.setString(2, oldAlias);
                statements.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo renombrar hipoteca", exception);
        }
    }

    private Long existingId(Connection connection, MortgageStatement s) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
            SELECT id FROM mortgage_statements
            WHERE loan_alias=? AND statement_date IS ? AND payment_due_date IS ? AND total_due=? AND outstanding_principal_balance=?
            """)) {
            ps.setString(1, s.getLoanAlias());
            ps.setString(2, date(s.getStatementDate()));
            ps.setString(3, date(s.getPaymentDueDate()));
            ps.setDouble(4, s.getTotalDue());
            ps.setDouble(5, s.getOutstandingPrincipalBalance());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private void bind(PreparedStatement ps, MortgageStatement s) throws SQLException {
        ps.setString(1, s.getLoanAlias());
        ps.setString(2, s.getServicerName());
        ps.setString(3, date(s.getStatementDate()));
        ps.setString(4, date(s.getPaymentDueDate()));
        ps.setDouble(5, s.getPaymentAmountDue());
        ps.setString(6, date(s.getLateFeeDate()));
        ps.setDouble(7, s.getLateFeeAmount());
        ps.setString(8, s.getLoanNumber());
        ps.setString(9, s.getPropertyAddress());
        ps.setDouble(10, s.getOriginalPrincipalBalance());
        ps.setDouble(11, s.getOutstandingPrincipalBalance());
        ps.setString(12, date(s.getMaturityDate()));
        ps.setDouble(13, s.getInterestRate());
        ps.setDouble(14, s.getEscrowBalance());
        ps.setDouble(15, s.getUnappliedFunds());
        ps.setDouble(16, s.getCurrentPaymentDue());
        ps.setDouble(17, s.getPrincipalDue());
        ps.setDouble(18, s.getInterestDue());
        ps.setDouble(19, s.getEscrowDue());
        ps.setDouble(20, s.getRegularMonthlyPayment());
        ps.setDouble(21, s.getPastDueAmount());
        ps.setDouble(22, s.getFees());
        ps.setDouble(23, s.getOtherFeesAndCharges());
        ps.setDouble(24, s.getTotalDue());
        ps.setString(25, s.getSourcePdfPath());
        ps.setDouble(26, s.getPastPaidPrincipalSinceLastStatement());
        ps.setDouble(27, s.getPastPaidPrincipalYearToDate());
        ps.setDouble(28, s.getPastPaidInterestSinceLastStatement());
        ps.setDouble(29, s.getPastPaidInterestYearToDate());
        ps.setDouble(30, s.getPastPaidEscrowSinceLastStatement());
        ps.setDouble(31, s.getPastPaidEscrowYearToDate());
        ps.setDouble(32, s.getPastPaidTotalSinceLastStatement());
        ps.setDouble(33, s.getPastPaidTotalYearToDate());
        ps.setString(34, s.getImportStatus());
        ps.setInt(35, s.isReviewRequired() ? 1 : 0);
        ps.setInt(36, s.isPendingReview() ? 1 : 0);
        ps.setString(37, s.getReviewNotes());
    }

    private MortgageStatement map(ResultSet rs) throws SQLException {
        MortgageStatement s = new MortgageStatement();
        s.setId(rs.getLong("id"));
        s.setLoanAlias(rs.getString("loan_alias"));
        s.setServicerName(rs.getString("servicer_name"));
        s.setStatementDate(localDate(rs.getString("statement_date")));
        s.setPaymentDueDate(localDate(rs.getString("payment_due_date")));
        s.setPaymentAmountDue(rs.getDouble("payment_amount_due"));
        s.setLateFeeDate(localDate(rs.getString("late_fee_date")));
        s.setLateFeeAmount(rs.getDouble("late_fee_amount"));
        s.setLoanNumber(rs.getString("loan_number"));
        s.setPropertyAddress(rs.getString("property_address"));
        s.setOriginalPrincipalBalance(rs.getDouble("original_principal_balance"));
        s.setOutstandingPrincipalBalance(rs.getDouble("outstanding_principal_balance"));
        s.setMaturityDate(localDate(rs.getString("maturity_date")));
        s.setInterestRate(rs.getDouble("interest_rate"));
        s.setEscrowBalance(rs.getDouble("escrow_balance"));
        s.setUnappliedFunds(rs.getDouble("unapplied_funds"));
        s.setCurrentPaymentDue(rs.getDouble("current_payment_due"));
        s.setPrincipalDue(rs.getDouble("principal_due"));
        s.setInterestDue(rs.getDouble("interest_due"));
        s.setEscrowDue(rs.getDouble("escrow_due"));
        s.setRegularMonthlyPayment(rs.getDouble("regular_monthly_payment"));
        s.setPastDueAmount(rs.getDouble("past_due_amount"));
        s.setFees(rs.getDouble("fees"));
        s.setOtherFeesAndCharges(rs.getDouble("other_fees_and_charges"));
        s.setTotalDue(rs.getDouble("total_due"));
        s.setPastPaidPrincipalSinceLastStatement(rs.getDouble("past_paid_principal_since_last_statement"));
        s.setPastPaidPrincipalYearToDate(rs.getDouble("past_paid_principal_year_to_date"));
        s.setPastPaidInterestSinceLastStatement(rs.getDouble("past_paid_interest_since_last_statement"));
        s.setPastPaidInterestYearToDate(rs.getDouble("past_paid_interest_year_to_date"));
        s.setPastPaidEscrowSinceLastStatement(rs.getDouble("past_paid_escrow_since_last_statement"));
        s.setPastPaidEscrowYearToDate(rs.getDouble("past_paid_escrow_year_to_date"));
        s.setPastPaidTotalSinceLastStatement(rs.getDouble("past_paid_total_since_last_statement"));
        s.setPastPaidTotalYearToDate(rs.getDouble("past_paid_total_year_to_date"));
        s.setSourcePdfPath(rs.getString("source_pdf_path"));
        s.setImportStatus(rs.getString("import_status"));
        s.setReviewRequired(rs.getInt("review_required") == 1);
        s.setPendingReview(rs.getInt("pending_review") == 1);
        s.setReviewNotes(rs.getString("review_notes"));
        return s;
    }

    private String date(LocalDate date) { return date == null ? null : date.toString(); }
    private LocalDate localDate(String value) { return value == null ? null : LocalDate.parse(value); }
}
