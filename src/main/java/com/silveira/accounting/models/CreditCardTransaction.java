package com.silveira.accounting.models;

import java.time.LocalDate;

public class CreditCardTransaction {
    private long id;
    private long statementId;
    private LocalDate transactionDate;
    private LocalDate postDate;
    private String description;
    private double amount;
    private String type;
    private String category;
    private boolean reviewRequired = true;
    private boolean pendingReview = true;
    private String reviewNotes;

    public CreditCardTransaction(long id, long statementId, LocalDate transactionDate, LocalDate postDate, String description, double amount, String type, String category) {
        this.id = id;
        this.statementId = statementId;
        this.transactionDate = transactionDate;
        this.postDate = postDate;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.category = category;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getStatementId() { return statementId; }
    public void setStatementId(long statementId) { this.statementId = statementId; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public LocalDate getPostDate() { return postDate; }
    public void setPostDate(LocalDate postDate) { this.postDate = postDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public boolean isPendingReview() { return pendingReview; }
    public void setPendingReview(boolean pendingReview) { this.pendingReview = pendingReview; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
