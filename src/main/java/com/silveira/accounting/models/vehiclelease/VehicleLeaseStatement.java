package com.silveira.accounting.models.vehiclelease;

import java.time.LocalDate;

public class VehicleLeaseStatement {
    private long id;
    private String accountAlias;
    private LocalDate statementDate;
    private LocalDate dueDate;
    private double totalAmountDue;
    private LocalDate lastPaymentDate;
    private double lastPaymentAmount;
    private int paymentsMade;
    private int paymentsRemaining;
    private double leasePayment;
    private double salesUseTax;
    private double propertyTax;
    private double parkingTickets;
    private double returnedCheckFees;
    private double miscellaneousCharges;
    private double pastDueAmount;
    private double lateCharges;
    private String sourcePdfPath;
    private boolean reviewRequired = true;
    private boolean pendingReview = true;
    private String reviewNotes;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getAccountAlias() { return accountAlias; }
    public void setAccountAlias(String accountAlias) { this.accountAlias = accountAlias; }
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public double getTotalAmountDue() { return totalAmountDue; }
    public void setTotalAmountDue(double totalAmountDue) { this.totalAmountDue = totalAmountDue; }
    public LocalDate getLastPaymentDate() { return lastPaymentDate; }
    public void setLastPaymentDate(LocalDate lastPaymentDate) { this.lastPaymentDate = lastPaymentDate; }
    public double getLastPaymentAmount() { return lastPaymentAmount; }
    public void setLastPaymentAmount(double lastPaymentAmount) { this.lastPaymentAmount = lastPaymentAmount; }
    public int getPaymentsMade() { return paymentsMade; }
    public void setPaymentsMade(int paymentsMade) { this.paymentsMade = paymentsMade; }
    public int getPaymentsRemaining() { return paymentsRemaining; }
    public void setPaymentsRemaining(int paymentsRemaining) { this.paymentsRemaining = paymentsRemaining; }
    public double getLeasePayment() { return leasePayment; }
    public void setLeasePayment(double leasePayment) { this.leasePayment = leasePayment; }
    public double getSalesUseTax() { return salesUseTax; }
    public void setSalesUseTax(double salesUseTax) { this.salesUseTax = salesUseTax; }
    public double getPropertyTax() { return propertyTax; }
    public void setPropertyTax(double propertyTax) { this.propertyTax = propertyTax; }
    public double getParkingTickets() { return parkingTickets; }
    public void setParkingTickets(double parkingTickets) { this.parkingTickets = parkingTickets; }
    public double getReturnedCheckFees() { return returnedCheckFees; }
    public void setReturnedCheckFees(double returnedCheckFees) { this.returnedCheckFees = returnedCheckFees; }
    public double getMiscellaneousCharges() { return miscellaneousCharges; }
    public void setMiscellaneousCharges(double miscellaneousCharges) { this.miscellaneousCharges = miscellaneousCharges; }
    public double getPastDueAmount() { return pastDueAmount; }
    public void setPastDueAmount(double pastDueAmount) { this.pastDueAmount = pastDueAmount; }
    public double getLateCharges() { return lateCharges; }
    public void setLateCharges(double lateCharges) { this.lateCharges = lateCharges; }
    public String getSourcePdfPath() { return sourcePdfPath; }
    public void setSourcePdfPath(String sourcePdfPath) { this.sourcePdfPath = sourcePdfPath; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public boolean isPendingReview() { return pendingReview; }
    public void setPendingReview(boolean pendingReview) { this.pendingReview = pendingReview; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
