package com.silveira.accounting.models;

import java.time.LocalDate;

public class MortgageTransaction {
    private long id;
    private long statementId;
    private LocalDate transactionDate;
    private String description;
    private double total;
    private double principal;
    private double interest;
    private double escrow;
    private double fees;
    private double unapplied;
    private double corporateAdvance;
    private double other;
    private boolean reviewRequired = true;
    private boolean pendingReview = true;
    private String reviewNotes;

    public MortgageTransaction(long id, long statementId, LocalDate transactionDate, String description, double total, double principal, double interest, double escrow, double fees, double unapplied, double corporateAdvance, double other) {
        this.id = id;
        this.statementId = statementId;
        this.transactionDate = transactionDate;
        this.description = description;
        this.total = total;
        this.principal = principal;
        this.interest = interest;
        this.escrow = escrow;
        this.fees = fees;
        this.unapplied = unapplied;
        this.corporateAdvance = corporateAdvance;
        this.other = other;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getStatementId() { return statementId; }
    public void setStatementId(long statementId) { this.statementId = statementId; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }
    public double getInterest() { return interest; }
    public void setInterest(double interest) { this.interest = interest; }
    public double getEscrow() { return escrow; }
    public void setEscrow(double escrow) { this.escrow = escrow; }
    public double getFees() { return fees; }
    public void setFees(double fees) { this.fees = fees; }
    public double getUnapplied() { return unapplied; }
    public void setUnapplied(double unapplied) { this.unapplied = unapplied; }
    public double getCorporateAdvance() { return corporateAdvance; }
    public void setCorporateAdvance(double corporateAdvance) { this.corporateAdvance = corporateAdvance; }
    public double getOther() { return other; }
    public void setOther(double other) { this.other = other; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public boolean isPendingReview() { return pendingReview; }
    public void setPendingReview(boolean pendingReview) { this.pendingReview = pendingReview; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
