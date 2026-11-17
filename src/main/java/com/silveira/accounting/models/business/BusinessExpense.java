package com.silveira.accounting.models.business;

import java.time.LocalDate;

public class BusinessExpense {
    private long id;
    private long businessId;
    private LocalDate expenseDate;
    private String description;
    private String provider;
    private double amount;
    private String invoice;
    private String paymentSource;
    private boolean reviewed;
    private String notes;
    private String documentPath;
    private String documentName;

    public BusinessExpense(long id, long businessId, LocalDate expenseDate, String description, String provider, double amount, String invoice, String paymentSource, boolean reviewed, String notes, String documentPath, String documentName) {
        this.id = id; this.businessId = businessId; this.expenseDate = expenseDate; this.description = description;
        this.provider = provider; this.amount = amount; this.invoice = invoice; this.paymentSource = paymentSource;
        this.reviewed = reviewed; this.notes = notes; this.documentPath = documentPath; this.documentName = documentName;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getBusinessId() { return businessId; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate value) { expenseDate = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { provider = value; }
    public double getAmount() { return amount; }
    public void setAmount(double value) { amount = value; }
    public String getInvoice() { return invoice; }
    public void setInvoice(String value) { invoice = value; }
    public String getPaymentSource() { return paymentSource; }
    public void setPaymentSource(String value) { paymentSource = value; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean value) { reviewed = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    public String getDocumentPath() { return documentPath; }
    public void setDocumentPath(String value) { documentPath = value; }
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String value) { documentName = value; }
}
