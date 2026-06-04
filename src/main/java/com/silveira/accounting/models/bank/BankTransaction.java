package com.silveira.accounting.models.bank;

import java.time.LocalDate;

public class BankTransaction {
    private long id;
    private LocalDate date;
    private String description;
    private double amount;
    private String movementType;
    private String provider;
    private String reference;
    private int month;
    private int year;
    private String sourcePdf;
    private String accountAlias;
    private String fingerprint;
    private boolean reconciled;
    private String importStatus;
    private boolean reviewRequired;
    private boolean pendingReview;
    private String reviewNotes;

    public BankTransaction(long id, LocalDate date, String description, double amount, String movementType, String provider,
                           String reference, int month, int year, String sourcePdf, String fingerprint, boolean reconciled) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.movementType = movementType;
        this.provider = provider;
        this.reference = reference;
        this.month = month;
        this.year = year;
        this.sourcePdf = sourcePdf;
        this.accountAlias = "";
        this.fingerprint = fingerprint;
        this.reconciled = reconciled;
        this.importStatus = "importado_auto";
        this.reviewRequired = true;
        this.pendingReview = true;
        this.reviewNotes = "Revisar contra PDF original";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public String getSourcePdf() { return sourcePdf; }
    public String getAccountAlias() { return accountAlias; }
    public void setAccountAlias(String accountAlias) { this.accountAlias = accountAlias; }
    public String getFingerprint() { return fingerprint; }
    public boolean isReconciled() { return reconciled; }
    public void setReconciled(boolean reconciled) { this.reconciled = reconciled; }
    public String getImportStatus() { return importStatus; }
    public void setImportStatus(String importStatus) { this.importStatus = importStatus; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public boolean isPendingReview() { return pendingReview; }
    public void setPendingReview(boolean pendingReview) { this.pendingReview = pendingReview; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
