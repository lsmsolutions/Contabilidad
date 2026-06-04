package com.silveira.accounting.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final Path databasePath = Path.of("data", "silveira-accounting.db");

    public void initialize() {
        try {
            Files.createDirectories(databasePath.getParent());
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS bank_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        alias TEXT NOT NULL UNIQUE,
                        account_number TEXT,
                        bank_name TEXT NOT NULL DEFAULT 'Chase',
                        account_type TEXT,
                        notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS bank_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        transaction_date TEXT NOT NULL,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        movement_type TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        reference TEXT,
                        month INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        source_pdf TEXT NOT NULL,
                        account_alias TEXT NOT NULL DEFAULT '',
                        import_status TEXT NOT NULL DEFAULT 'importado_auto',
                        review_required INTEGER NOT NULL DEFAULT 1,
                        pending_review INTEGER NOT NULL DEFAULT 1,
                        review_notes TEXT,
                        fingerprint TEXT NOT NULL UNIQUE,
                        reconciled INTEGER NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS nyl_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        concept TEXT NOT NULL,
                        section TEXT NOT NULL DEFAULT 'Creditos',
                        record_type TEXT NOT NULL,
                        amount REAL NOT NULL,
                        source_pdf TEXT NOT NULL,
                        import_status TEXT NOT NULL DEFAULT 'importado_auto',
                        review_required INTEGER NOT NULL DEFAULT 0,
                        pending_review INTEGER NOT NULL DEFAULT 0,
                        review_notes TEXT,
                        fingerprint TEXT NOT NULL UNIQUE,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS known_nyl_concepts (
                        concept TEXT PRIMARY KEY,
                        first_seen_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS monthly_closings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        source TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        metric TEXT NOT NULL,
                        amount REAL NOT NULL,
                        UNIQUE(source, year, month, metric)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS bank_statement_periods (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_alias TEXT NOT NULL,
                        source_pdf TEXT NOT NULL,
                        period_start TEXT NOT NULL,
                        period_end TEXT NOT NULL,
                        opening_balance REAL NOT NULL DEFAULT 0,
                        statement_ending_balance REAL NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(account_alias, source_pdf)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS closed_months (
                        source TEXT NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        closed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        notes TEXT,
                        PRIMARY KEY(source, year, month)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS review_marks (
                        source TEXT NOT NULL,
                        account_alias TEXT NOT NULL DEFAULT '',
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        marked INTEGER NOT NULL DEFAULT 0,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY(source, account_alias, year, month)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS correction_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        entity TEXT NOT NULL,
                        entity_id INTEGER,
                        fingerprint TEXT,
                        field_name TEXT NOT NULL,
                        old_value TEXT,
                        new_value TEXT,
                        reason TEXT,
                        changed_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS reconciliations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        bank_transaction_id INTEGER,
                        nyl_record_id INTEGER,
                        status TEXT NOT NULL,
                        difference REAL NOT NULL DEFAULT 0,
                        notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(bank_transaction_id) REFERENCES bank_transactions(id),
                        FOREIGN KEY(nyl_record_id) REFERENCES nyl_records(id)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS credit_card_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        alias TEXT NOT NULL UNIQUE,
                        bank_name TEXT NOT NULL,
                        card_name TEXT,
                        account_last_digits TEXT,
                        notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS credit_card_statements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_alias TEXT NOT NULL,
                        bank_name TEXT NOT NULL,
                        card_name TEXT,
                        account_last_digits TEXT,
                        statement_start_date TEXT,
                        statement_end_date TEXT,
                        payment_due_date TEXT,
                        next_closing_date TEXT,
                        previous_balance REAL NOT NULL DEFAULT 0,
                        payments REAL NOT NULL DEFAULT 0,
                        other_credits REAL NOT NULL DEFAULT 0,
                        transactions REAL NOT NULL DEFAULT 0,
                        balance_transfers REAL NOT NULL DEFAULT 0,
                        cash_advances REAL NOT NULL DEFAULT 0,
                        fees_charged REAL NOT NULL DEFAULT 0,
                        interest_charged REAL NOT NULL DEFAULT 0,
                        new_balance REAL NOT NULL DEFAULT 0,
                        minimum_payment_due REAL NOT NULL DEFAULT 0,
                        credit_limit REAL NOT NULL DEFAULT 0,
                        available_credit REAL NOT NULL DEFAULT 0,
                        cash_advance_limit REAL NOT NULL DEFAULT 0,
                        available_cash_advance_credit REAL NOT NULL DEFAULT 0,
                        source_pdf_path TEXT NOT NULL,
                        import_status TEXT NOT NULL DEFAULT 'importado_auto',
                        review_required INTEGER NOT NULL DEFAULT 1,
                        pending_review INTEGER NOT NULL DEFAULT 1,
                        review_notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(account_alias, statement_end_date, new_balance)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS credit_card_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        statement_id INTEGER NOT NULL,
                        transaction_date TEXT,
                        post_date TEXT,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT,
                        review_required INTEGER NOT NULL DEFAULT 1,
                        pending_review INTEGER NOT NULL DEFAULT 1,
                        review_notes TEXT,
                        FOREIGN KEY(statement_id) REFERENCES credit_card_statements(id)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS credit_card_statement_field_reviews (
                        statement_id INTEGER NOT NULL,
                        field_name TEXT NOT NULL,
                        reviewed INTEGER NOT NULL DEFAULT 0,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY(statement_id, field_name),
                        FOREIGN KEY(statement_id) REFERENCES credit_card_statements(id)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS financial_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        statement_id INTEGER NOT NULL,
                        severity TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(statement_id) REFERENCES credit_card_statements(id)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS mortgage_accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        alias TEXT NOT NULL UNIQUE,
                        servicer_name TEXT,
                        loan_number TEXT,
                        property_address TEXT,
                        notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS mortgage_statements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        loan_alias TEXT NOT NULL,
                        servicer_name TEXT,
                        statement_date TEXT,
                        payment_due_date TEXT,
                        payment_amount_due REAL NOT NULL DEFAULT 0,
                        late_fee_date TEXT,
                        late_fee_amount REAL NOT NULL DEFAULT 0,
                        loan_number TEXT,
                        property_address TEXT,
                        original_principal_balance REAL NOT NULL DEFAULT 0,
                        outstanding_principal_balance REAL NOT NULL DEFAULT 0,
                        maturity_date TEXT,
                        interest_rate REAL NOT NULL DEFAULT 0,
                        escrow_balance REAL NOT NULL DEFAULT 0,
                        unapplied_funds REAL NOT NULL DEFAULT 0,
                        current_payment_due REAL NOT NULL DEFAULT 0,
                        principal_due REAL NOT NULL DEFAULT 0,
                        interest_due REAL NOT NULL DEFAULT 0,
                        escrow_due REAL NOT NULL DEFAULT 0,
                        regular_monthly_payment REAL NOT NULL DEFAULT 0,
                        past_due_amount REAL NOT NULL DEFAULT 0,
                        fees REAL NOT NULL DEFAULT 0,
                        other_fees_and_charges REAL NOT NULL DEFAULT 0,
                        total_due REAL NOT NULL DEFAULT 0,
                        past_paid_principal_since_last_statement REAL NOT NULL DEFAULT 0,
                        past_paid_principal_year_to_date REAL NOT NULL DEFAULT 0,
                        past_paid_interest_since_last_statement REAL NOT NULL DEFAULT 0,
                        past_paid_interest_year_to_date REAL NOT NULL DEFAULT 0,
                        past_paid_escrow_since_last_statement REAL NOT NULL DEFAULT 0,
                        past_paid_escrow_year_to_date REAL NOT NULL DEFAULT 0,
                        past_paid_total_since_last_statement REAL NOT NULL DEFAULT 0,
                        past_paid_total_year_to_date REAL NOT NULL DEFAULT 0,
                        source_pdf_path TEXT NOT NULL,
                        import_status TEXT NOT NULL DEFAULT 'importado_en_revision',
                        review_required INTEGER NOT NULL DEFAULT 1,
                        pending_review INTEGER NOT NULL DEFAULT 1,
                        review_notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(loan_alias, statement_date, payment_due_date, total_due, outstanding_principal_balance)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS mortgage_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        statement_id INTEGER NOT NULL,
                        transaction_date TEXT,
                        description TEXT NOT NULL,
                        total REAL NOT NULL DEFAULT 0,
                        principal REAL NOT NULL DEFAULT 0,
                        interest REAL NOT NULL DEFAULT 0,
                        escrow REAL NOT NULL DEFAULT 0,
                        fees REAL NOT NULL DEFAULT 0,
                        unapplied REAL NOT NULL DEFAULT 0,
                        corporate_advance REAL NOT NULL DEFAULT 0,
                        other REAL NOT NULL DEFAULT 0,
                        review_required INTEGER NOT NULL DEFAULT 1,
                        pending_review INTEGER NOT NULL DEFAULT 1,
                        review_notes TEXT,
                        FOREIGN KEY(statement_id) REFERENCES mortgage_statements(id)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS mortgage_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        statement_id INTEGER NOT NULL,
                        severity TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY(statement_id) REFERENCES mortgage_statements(id)
                    )
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS house_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        loan_alias TEXT NOT NULL,
                        expense_date TEXT,
                        description TEXT NOT NULL,
                        provider TEXT,
                        amount REAL NOT NULL DEFAULT 0,
                        invoice TEXT,
                        payment_source TEXT,
                        notes TEXT,
                        document_path TEXT,
                        document_name TEXT,
                        review_required INTEGER NOT NULL DEFAULT 1,
                        pending_review INTEGER NOT NULL DEFAULT 1,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_house_expenses_loan_date_id ON house_expenses(loan_alias, expense_date DESC, id DESC)");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_house_expenses_date_id ON house_expenses(expense_date DESC, id DESC)");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS internal_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        source_type TEXT NOT NULL,
                        source_id INTEGER NOT NULL DEFAULT 0,
                        movement_date TEXT,
                        movement_from TEXT NOT NULL DEFAULT '',
                        movement_to TEXT NOT NULL DEFAULT '',
                        amount REAL NOT NULL DEFAULT 0,
                        description TEXT,
                        status TEXT NOT NULL DEFAULT 'Pendiente',
                        reviewed INTEGER NOT NULL DEFAULT 0,
                        manual INTEGER NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(source_type, source_id)
                    )
                    """);
                migrate(statement);
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("No se pudo inicializar la base de datos local", exception);
        }
    }

    private void migrate(Statement statement) throws SQLException {
        addColumnIfMissing(statement, "bank_transactions", "import_status", "TEXT NOT NULL DEFAULT 'importado_auto'");
        addColumnIfMissing(statement, "bank_transactions", "account_alias", "TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing(statement, "bank_transactions", "review_required", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "bank_transactions", "pending_review", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "bank_transactions", "review_notes", "TEXT");
        addColumnIfMissing(statement, "nyl_records", "import_status", "TEXT NOT NULL DEFAULT 'importado_auto'");
        addColumnIfMissing(statement, "nyl_records", "section", "TEXT NOT NULL DEFAULT 'Creditos'");
        addColumnIfMissing(statement, "nyl_records", "review_required", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "nyl_records", "pending_review", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "nyl_records", "review_notes", "TEXT");
        addColumnIfMissing(statement, "credit_card_statements", "review_required", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "credit_card_statements", "pending_review", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "credit_card_statements", "review_notes", "TEXT");
        addColumnIfMissing(statement, "credit_card_statements", "balance_transfers", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "credit_card_statements", "rewards_balance", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "credit_card_statements", "rewards_previous_balance", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "credit_card_statements", "rewards_earned", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "credit_card_statements", "rewards_redeemed", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "credit_card_transactions", "review_required", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "credit_card_transactions", "pending_review", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "credit_card_transactions", "review_notes", "TEXT");
        statement.execute("""
            CREATE TABLE IF NOT EXISTS credit_card_statement_field_reviews (
                statement_id INTEGER NOT NULL,
                field_name TEXT NOT NULL,
                reviewed INTEGER NOT NULL DEFAULT 0,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY(statement_id, field_name),
                FOREIGN KEY(statement_id) REFERENCES credit_card_statements(id)
            )
            """);
        statement.execute("""
            CREATE TABLE IF NOT EXISTS review_marks (
                source TEXT NOT NULL,
                account_alias TEXT NOT NULL DEFAULT '',
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                marked INTEGER NOT NULL DEFAULT 0,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY(source, account_alias, year, month)
            )
            """);
        addColumnIfMissing(statement, "mortgage_statements", "loan_alias", "TEXT NOT NULL DEFAULT 'hipoteca'");
        addColumnIfMissing(statement, "mortgage_statements", "review_required", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "mortgage_statements", "pending_review", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "mortgage_statements", "review_notes", "TEXT");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_principal_since_last_statement", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_principal_year_to_date", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_interest_since_last_statement", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_interest_year_to_date", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_escrow_since_last_statement", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_escrow_year_to_date", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_total_since_last_statement", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_statements", "past_paid_total_year_to_date", "REAL NOT NULL DEFAULT 0");
        addColumnIfMissing(statement, "mortgage_transactions", "review_required", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "mortgage_transactions", "pending_review", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfMissing(statement, "mortgage_transactions", "review_notes", "TEXT");
        addColumnIfMissing(statement, "house_expenses", "document_path", "TEXT");
        addColumnIfMissing(statement, "house_expenses", "document_name", "TEXT");
        addColumnIfMissing(statement, "house_expenses", "payment_source", "TEXT");
        statement.execute("""
            CREATE TABLE IF NOT EXISTS internal_movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source_type TEXT NOT NULL,
                source_id INTEGER NOT NULL DEFAULT 0,
                movement_date TEXT,
                movement_from TEXT NOT NULL DEFAULT '',
                movement_to TEXT NOT NULL DEFAULT '',
                amount REAL NOT NULL DEFAULT 0,
                description TEXT,
                status TEXT NOT NULL DEFAULT 'Pendiente',
                reviewed INTEGER NOT NULL DEFAULT 0,
                manual INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(source_type, source_id)
            )
            """);
    }

    private void addColumnIfMissing(Statement statement, String table, String column, String definition) {
        try {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
            // SQLite raises "duplicate column name" when the app has already migrated this database.
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }
}
