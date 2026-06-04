package com.silveira.accounting.models;

import java.time.LocalDate;

public class InternalMovementRecord {
    private long id;
    private String sourceType;
    private long sourceId;
    private LocalDate date;
    private String from;
    private String to;
    private double amount;
    private String description;
    private String status;
    private boolean reviewed;
    private boolean manual;

    public InternalMovementRecord(long id, String sourceType, long sourceId, LocalDate date, String from, String to,
                                  double amount, String description, String status, boolean reviewed, boolean manual) {
        this.id = id;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.date = date;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.description = description;
        this.status = status;
        this.reviewed = reviewed;
        this.manual = manual;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public long getSourceId() { return sourceId; }
    public void setSourceId(long sourceId) { this.sourceId = sourceId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    public boolean isManual() { return manual; }
    public void setManual(boolean manual) { this.manual = manual; }
}
