package com.silveira.accounting.models;

public class NylRecord {
    private long id;
    private int year;
    private int month;
    private String concept;
    private String section;
    private String recordType;
    private double amount;
    private String sourcePdf;
    private String fingerprint;
    private String importStatus;
    private boolean reviewRequired;
    private boolean pendingReview;
    private String reviewNotes;

    public NylRecord(long id, int year, int month, String concept, String recordType, double amount, String sourcePdf, String fingerprint) {
        this(id, year, month, concept, defaultSection(recordType), recordType, amount, sourcePdf, fingerprint, "importado_auto", false, false, "");
    }

    public NylRecord(long id, int year, int month, String concept, String recordType, double amount, String sourcePdf,
                     String fingerprint, String importStatus, boolean reviewRequired, String reviewNotes) {
        this(id, year, month, concept, defaultSection(recordType), recordType, amount, sourcePdf, fingerprint, importStatus, reviewRequired, reviewRequired, reviewNotes);
    }

    public NylRecord(long id, int year, int month, String concept, String section, String recordType, double amount, String sourcePdf,
                     String fingerprint, String importStatus, boolean reviewRequired, String reviewNotes) {
        this(id, year, month, concept, section, recordType, amount, sourcePdf, fingerprint, importStatus, reviewRequired, reviewRequired, reviewNotes);
    }

    public NylRecord(long id, int year, int month, String concept, String section, String recordType, double amount, String sourcePdf,
                     String fingerprint, String importStatus, boolean reviewRequired, boolean pendingReview, String reviewNotes) {
        this.id = id;
        this.year = year;
        this.month = month;
        this.concept = concept;
        this.section = section;
        this.recordType = recordType;
        this.amount = amount;
        this.sourcePdf = sourcePdf;
        this.fingerprint = fingerprint;
        this.importStatus = importStatus;
        this.reviewRequired = reviewRequired;
        this.pendingReview = pendingReview;
        this.reviewNotes = reviewNotes;
    }

    public long getId() { return id; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getSourcePdf() { return sourcePdf; }
    public String getFingerprint() { return fingerprint; }
    public String getImportStatus() { return importStatus; }
    public void setImportStatus(String importStatus) { this.importStatus = importStatus; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public boolean isPendingReview() { return pendingReview; }
    public void setPendingReview(boolean pendingReview) { this.pendingReview = pendingReview; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    private static String defaultSection(String recordType) {
        if ("deduccion".equals(recordType) || "withdrawal".equals(recordType)) {
            return "Deducciones";
        }
        if ("ajuste".equals(recordType)) {
            return "Ajustes";
        }
        return "Creditos";
    }
}
