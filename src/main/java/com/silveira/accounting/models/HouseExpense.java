package com.silveira.accounting.models;

import java.time.LocalDate;

public class HouseExpense {
    private long id;
    private String loanAlias;
    private LocalDate expenseDate;
    private String description;
    private String provider;
    private double amount;
    private String invoice;
    private String paymentSource;
    private String notes;
    private String documentPath;
    private String documentName;
    private boolean reviewRequired = true;
    private boolean pendingReview = true;

    public HouseExpense(long id, String loanAlias, LocalDate expenseDate, String description, String provider, double amount, String invoice, String notes) {
        this.id = id;
        this.loanAlias = loanAlias;
        this.expenseDate = expenseDate;
        this.description = description;
        this.provider = provider;
        this.amount = amount;
        this.invoice = invoice;
        this.notes = notes;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getLoanAlias() { return loanAlias; }
    public void setLoanAlias(String loanAlias) { this.loanAlias = loanAlias; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getInvoice() { return invoice; }
    public void setInvoice(String invoice) { this.invoice = invoice; }
    public String getPaymentSource() { return paymentSource; }
    public void setPaymentSource(String paymentSource) { this.paymentSource = paymentSource; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String documentPath) { this.documentPath = documentPath; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public boolean isPendingReview() { return pendingReview; }
    public void setPendingReview(boolean pendingReview) { this.pendingReview = pendingReview; }
}
