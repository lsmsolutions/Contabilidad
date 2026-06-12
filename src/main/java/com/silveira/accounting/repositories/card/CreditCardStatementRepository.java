package com.silveira.accounting.repositories.card;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.SourceTotals;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CreditCardStatementRepository {
    private final DatabaseManager databaseManager;

    public CreditCardStatementRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public long save(CreditCardStatement s) {
        String sql = """
            INSERT OR IGNORE INTO credit_card_statements
            (account_alias, bank_name, card_name, account_last_digits, statement_start_date, statement_end_date, payment_due_date, next_closing_date,
             previous_balance, payments, other_credits, transactions, balance_transfers, cash_advances, fees_charged, interest_charged, new_balance,
             minimum_payment_due, credit_limit, available_credit, cash_advance_limit, available_cash_advance_credit, source_pdf_path, import_status,
             review_required, pending_review, review_notes, rewards_balance, rewards_previous_balance, rewards_earned, rewards_redeemed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, s);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            Long existing = findExistingId(connection, s);
            return existing == null ? 0 : existing;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar statement de tarjeta", exception);
        }
    }

    public List<CreditCardStatement> findByAccount(String alias) {
        return findByAccount(alias, null, null);
    }

    public Optional<CreditCardStatement> findById(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM credit_card_statements WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar statement de tarjeta", exception);
        }
    }

    public List<CreditCardStatement> findByAccount(String alias, Integer year, Integer month) {
        StringBuilder sql = new StringBuilder("SELECT * FROM credit_card_statements WHERE account_alias=?");
        if (year != null) sql.append(" AND substr(statement_end_date, 1, 4)=?");
        if (month != null) sql.append(" AND substr(statement_end_date, 6, 2)=?");
        sql.append(" ORDER BY statement_end_date DESC");
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, alias);
            if (year != null) statement.setString(index++, String.valueOf(year));
            if (month != null) statement.setString(index++, String.format("%02d", month));
            try (ResultSet rs = statement.executeQuery()) {
                List<CreditCardStatement> statements = new ArrayList<>();
                while (rs.next()) statements.add(map(rs));
                return statements;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar statements", exception);
        }
    }

    public void updateRecord(CreditCardStatement s) {
        String sql = """
            UPDATE credit_card_statements SET
                statement_start_date=?, statement_end_date=?, payment_due_date=?, next_closing_date=?,
                previous_balance=?, payments=?, other_credits=?, transactions=?, balance_transfers=?, cash_advances=?, fees_charged=?,
                interest_charged=?, new_balance=?, minimum_payment_due=?, credit_limit=?, available_credit=?,
                cash_advance_limit=?, available_cash_advance_credit=?, import_status=?, review_required=?, pending_review=?, review_notes=?,
                rewards_balance=?, rewards_previous_balance=?, rewards_earned=?, rewards_redeemed=?
            WHERE id=?
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date(s.getStatementStartDate()));
            ps.setString(2, date(s.getStatementEndDate()));
            ps.setString(3, date(s.getPaymentDueDate()));
            ps.setString(4, date(s.getNextClosingDate()));
            ps.setDouble(5, s.getPreviousBalance());
            ps.setDouble(6, s.getPayments());
            ps.setDouble(7, s.getOtherCredits());
            ps.setDouble(8, s.getTransactions());
            ps.setDouble(9, s.getBalanceTransfers());
            ps.setDouble(10, s.getCashAdvances());
            ps.setDouble(11, s.getFeesCharged());
            ps.setDouble(12, s.getInterestCharged());
            ps.setDouble(13, s.getNewBalance());
            ps.setDouble(14, s.getMinimumPaymentDue());
            ps.setDouble(15, s.getCreditLimit());
            ps.setDouble(16, s.getAvailableCredit());
            ps.setDouble(17, s.getCashAdvanceLimit());
            ps.setDouble(18, s.getAvailableCashAdvanceCredit());
            ps.setString(19, s.getImportStatus());
            ps.setInt(20, s.isReviewRequired() ? 1 : 0);
            ps.setInt(21, s.isPendingReview() ? 1 : 0);
            ps.setString(22, s.getReviewNotes());
            ps.setDouble(23, s.getRewardsBalance());
            ps.setDouble(24, s.getRewardsPreviousBalance());
            ps.setDouble(25, s.getRewardsEarned());
            ps.setDouble(26, s.getRewardsRedeemed());
            ps.setLong(27, s.getId());
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar statement de tarjeta", exception);
        }
    }

    public void updatePeriod(long id, LocalDate statementStartDate, LocalDate statementEndDate) {
        String sql = "UPDATE credit_card_statements SET statement_start_date=?, statement_end_date=? WHERE id=?";
        try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, date(statementStartDate));
            ps.setString(2, date(statementEndDate));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo actualizar el periodo del statement de tarjeta", exception);
        }
    }

    public SourceTotals totals(String alias, Integer year, Integer month) {
        List<CreditCardStatement> rows = findByAccount(alias, year, month);
        int reviewed = (int) rows.stream().filter(s -> !s.isPendingReview()).count();
        int pending = (int) rows.stream().filter(CreditCardStatement::isPendingReview).count();
        double debt = rows.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getNewBalance).sum();
        double minimum = rows.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getMinimumPaymentDue).sum();
        double interest = rows.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getInterestCharged).sum();
        return new SourceTotals(reviewed, pending, debt, minimum, interest);
    }

    public List<MonthlySourceTotals> monthlyTotals(String alias, Integer year) {
        List<CreditCardStatement> rows = findByAccount(alias, year, null);
        return rows.stream()
            .collect(java.util.stream.Collectors.groupingBy(s -> s.getStatementEndDate() == null ? "" : s.getStatementEndDate().getYear() + "-" + s.getStatementEndDate().getMonthValue()))
            .entrySet().stream()
            .filter(entry -> !entry.getKey().isBlank())
            .map(entry -> {
                List<CreditCardStatement> monthRows = entry.getValue();
                CreditCardStatement first = monthRows.get(0);
                int reviewed = (int) monthRows.stream().filter(s -> !s.isPendingReview()).count();
                int pending = (int) monthRows.stream().filter(CreditCardStatement::isPendingReview).count();
                double debt = monthRows.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getNewBalance).sum();
                double minimum = monthRows.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getMinimumPaymentDue).sum();
                double interest = monthRows.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getInterestCharged).sum();
                return new MonthlySourceTotals(first.getStatementEndDate().getYear(), first.getStatementEndDate().getMonthValue(), reviewed, pending, debt, minimum, interest);
            })
            .sorted(java.util.Comparator.comparing(MonthlySourceTotals::year).thenComparing(MonthlySourceTotals::month))
            .toList();
    }

    public void delete(long id) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement alerts = connection.prepareStatement("DELETE FROM financial_alerts WHERE statement_id=?");
             PreparedStatement transactions = connection.prepareStatement("DELETE FROM credit_card_transactions WHERE statement_id=?");
             PreparedStatement statement = connection.prepareStatement("DELETE FROM credit_card_statements WHERE id=?")) {
            alerts.setLong(1, id);
            alerts.executeUpdate();
            transactions.setLong(1, id);
            transactions.executeUpdate();
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo eliminar statement de tarjeta", exception);
        }
    }

    private Long findExistingId(Connection connection, CreditCardStatement s) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM credit_card_statements WHERE account_alias=? AND statement_end_date=? AND new_balance=?")) {
            statement.setString(1, s.getAccountAlias());
            statement.setString(2, date(s.getStatementEndDate()));
            statement.setDouble(3, s.getNewBalance());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    private void bind(PreparedStatement ps, CreditCardStatement s) throws SQLException {
        ps.setString(1, s.getAccountAlias());
        ps.setString(2, s.getBankName());
        ps.setString(3, s.getCardName());
        ps.setString(4, s.getAccountLastDigits());
        ps.setString(5, date(s.getStatementStartDate()));
        ps.setString(6, date(s.getStatementEndDate()));
        ps.setString(7, date(s.getPaymentDueDate()));
        ps.setString(8, date(s.getNextClosingDate()));
        ps.setDouble(9, s.getPreviousBalance());
        ps.setDouble(10, s.getPayments());
        ps.setDouble(11, s.getOtherCredits());
        ps.setDouble(12, s.getTransactions());
        ps.setDouble(13, s.getBalanceTransfers());
        ps.setDouble(14, s.getCashAdvances());
        ps.setDouble(15, s.getFeesCharged());
        ps.setDouble(16, s.getInterestCharged());
        ps.setDouble(17, s.getNewBalance());
        ps.setDouble(18, s.getMinimumPaymentDue());
        ps.setDouble(19, s.getCreditLimit());
        ps.setDouble(20, s.getAvailableCredit());
        ps.setDouble(21, s.getCashAdvanceLimit());
        ps.setDouble(22, s.getAvailableCashAdvanceCredit());
        ps.setString(23, s.getSourcePdfPath());
        ps.setString(24, s.getImportStatus());
        ps.setInt(25, s.isReviewRequired() ? 1 : 0);
        ps.setInt(26, s.isPendingReview() ? 1 : 0);
        ps.setString(27, s.getReviewNotes());
        ps.setDouble(28, s.getRewardsBalance());
        ps.setDouble(29, s.getRewardsPreviousBalance());
        ps.setDouble(30, s.getRewardsEarned());
        ps.setDouble(31, s.getRewardsRedeemed());
    }

    private String date(LocalDate date) { return date == null ? null : date.toString(); }
    private LocalDate localDate(String value) { return value == null ? null : LocalDate.parse(value); }

    private CreditCardStatement map(ResultSet rs) throws SQLException {
        CreditCardStatement s = new CreditCardStatement();
        s.setId(rs.getLong("id"));
        s.setAccountAlias(rs.getString("account_alias"));
        s.setBankName(rs.getString("bank_name"));
        s.setCardName(rs.getString("card_name"));
        s.setAccountLastDigits(rs.getString("account_last_digits"));
        s.setStatementStartDate(localDate(rs.getString("statement_start_date")));
        s.setStatementEndDate(localDate(rs.getString("statement_end_date")));
        s.setPaymentDueDate(localDate(rs.getString("payment_due_date")));
        s.setNextClosingDate(localDate(rs.getString("next_closing_date")));
        s.setPreviousBalance(rs.getDouble("previous_balance"));
        s.setPayments(rs.getDouble("payments"));
        s.setOtherCredits(rs.getDouble("other_credits"));
        s.setTransactions(rs.getDouble("transactions"));
        s.setBalanceTransfers(readDouble(rs, "balance_transfers"));
        s.setCashAdvances(rs.getDouble("cash_advances"));
        s.setFeesCharged(rs.getDouble("fees_charged"));
        s.setInterestCharged(rs.getDouble("interest_charged"));
        s.setNewBalance(rs.getDouble("new_balance"));
        s.setMinimumPaymentDue(rs.getDouble("minimum_payment_due"));
        s.setCreditLimit(rs.getDouble("credit_limit"));
        s.setAvailableCredit(rs.getDouble("available_credit"));
        s.setCashAdvanceLimit(rs.getDouble("cash_advance_limit"));
        s.setAvailableCashAdvanceCredit(rs.getDouble("available_cash_advance_credit"));
        s.setRewardsBalance(readDouble(rs, "rewards_balance"));
        s.setRewardsPreviousBalance(readDouble(rs, "rewards_previous_balance"));
        s.setRewardsEarned(readDouble(rs, "rewards_earned"));
        s.setRewardsRedeemed(readDouble(rs, "rewards_redeemed"));
        s.setSourcePdfPath(rs.getString("source_pdf_path"));
        s.setImportStatus(rs.getString("import_status"));
        s.setReviewRequired(rs.getInt("review_required") == 1);
        s.setPendingReview(rs.getInt("pending_review") == 1);
        s.setReviewNotes(rs.getString("review_notes"));
        return s;
    }

    private double readDouble(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getDouble(column);
        } catch (SQLException exception) {
            return 0;
        }
    }
}
