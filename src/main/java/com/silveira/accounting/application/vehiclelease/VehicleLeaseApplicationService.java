package com.silveira.accounting.application.vehiclelease;

import com.silveira.accounting.application.importing.DocumentImportService;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.parsers.vehiclelease.VehicleLeaseImportData;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseAccountRepository;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseFieldReviewRepository;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseStatementRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class VehicleLeaseApplicationService {
    public static final List<String> REVIEW_FIELDS = List.of(
        "total_amount_due",
        "last_payment_amount",
        "lease_payment",
        "sales_use_tax",
        "property_tax",
        "parking_tickets",
        "returned_check_fees",
        "miscellaneous_charges",
        "past_due_amount",
        "late_charges"
    );

    private final VehicleLeaseAccountRepository accounts;
    private final VehicleLeaseStatementRepository statements;
    private final VehicleLeaseFieldReviewRepository reviews;
    private final DocumentImportService<VehicleLeaseImportData> imports;

    public VehicleLeaseApplicationService(
        VehicleLeaseAccountRepository accounts,
        VehicleLeaseStatementRepository statements,
        VehicleLeaseFieldReviewRepository reviews,
        DocumentImportService<VehicleLeaseImportData> imports
    ) {
        this.accounts = accounts;
        this.statements = statements;
        this.reviews = reviews;
        this.imports = imports;
    }

    public VehicleLeaseStatement importPdf(Path pdf) {
        VehicleLeaseImportData data = imports.importPdf(pdf);
        return saveImport(data, null, true);
    }

    public VehicleLeaseStatement importPdf(Path pdf, String accountAlias) {
        VehicleLeaseImportData data = imports.importPdf(pdf);
        return saveImport(data, accountAlias, false);
    }

    public VehicleLeaseStatement importPdfWithAi(Path pdf) {
        VehicleLeaseImportData data = imports.importPdfWithAi(pdf);
        return saveImport(data, null, true);
    }

    public VehicleLeaseStatement importPdfWithAi(Path pdf, String accountAlias) {
        VehicleLeaseImportData data = imports.importPdfWithAi(pdf);
        return saveImport(data, accountAlias, false);
    }

    private VehicleLeaseStatement saveImport(VehicleLeaseImportData data, String accountAlias, boolean saveAccount) {
        if (saveAccount) {
            accounts.save(data.account());
        }
        VehicleLeaseStatement statement = data.statement();
        if (accountAlias != null) {
            statement.setAccountAlias(accountAlias);
        }
        statement.setReviewRequired(true);
        statement.setPendingReview(true);
        if (statement.getReviewNotes() == null || statement.getReviewNotes().isBlank()) {
            statement.setReviewNotes("Revisar contra el PDF original");
        }
        long id = statements.save(statement);
        statement.setId(id);
        return statement;
    }

    public List<VehicleLeaseAccount> accounts() {
        return accounts.findAll();
    }

    public Optional<VehicleLeaseAccount> account(String alias) {
        return accounts.findByAlias(alias);
    }

    public void saveAccount(VehicleLeaseAccount account) {
        accounts.save(account);
    }

    public void updateAccount(String originalAlias, VehicleLeaseAccount account) {
        accounts.update(originalAlias, account);
    }

    public void deleteAccount(String alias) {
        accounts.delete(alias);
    }

    public List<VehicleLeaseStatement> statements(String alias) {
        return statements.findByAccount(alias);
    }

    public void update(VehicleLeaseStatement statement) {
        statements.update(statement);
    }

    public void delete(VehicleLeaseStatement statement) {
        if (statement.getId() > 0) {
            statements.delete(statement.getId());
        }
    }

    public boolean isFieldReviewed(VehicleLeaseStatement statement, String field) {
        return statement.getId() > 0 && reviews.isReviewed(statement.getId(), field);
    }

    public boolean isStatementReviewed(VehicleLeaseStatement statement) {
        return statement.getId() > 0 && REVIEW_FIELDS.stream().allMatch(field -> reviews.isReviewed(statement.getId(), field));
    }

    public void setFieldReviewed(VehicleLeaseStatement statement, String field, boolean reviewed) {
        reviews.setReviewed(statement.getId(), field, reviewed);
        updateOverallReview(statement);
    }

    public void setAllReviewed(VehicleLeaseStatement statement, boolean reviewed) {
        reviews.setReviewed(statement.getId(), REVIEW_FIELDS, reviewed);
        statement.setPendingReview(!reviewed);
        statement.setReviewRequired(!reviewed);
        statement.setReviewNotes(reviewed ? "Revisado" : "Revisar contra el PDF original");
        statements.update(statement);
    }

    private void updateOverallReview(VehicleLeaseStatement statement) {
        boolean reviewed = REVIEW_FIELDS.stream().allMatch(field -> reviews.isReviewed(statement.getId(), field));
        statement.setPendingReview(!reviewed);
        statement.setReviewRequired(!reviewed);
        statement.setReviewNotes(reviewed ? "Revisado" : "Revisar contra el PDF original");
        statements.update(statement);
    }
}
