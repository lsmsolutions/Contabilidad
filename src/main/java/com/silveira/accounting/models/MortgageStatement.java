package com.silveira.accounting.models;

import java.time.LocalDate;

public class MortgageStatement {
    private long id;
    private String loanAlias;
    private String servicerName;
    private LocalDate statementDate;
    private LocalDate paymentDueDate;
    private double paymentAmountDue;
    private LocalDate lateFeeDate;
    private double lateFeeAmount;
    private String loanNumber;
    private String propertyAddress;
    private double originalPrincipalBalance;
    private double outstandingPrincipalBalance;
    private LocalDate maturityDate;
    private double interestRate;
    private double escrowBalance;
    private double unappliedFunds;
    private double currentPaymentDue;
    private double principalDue;
    private double interestDue;
    private double escrowDue;
    private double regularMonthlyPayment;
    private double pastDueAmount;
    private double fees;
    private double otherFeesAndCharges;
    private double totalDue;
    private double pastPaidPrincipalSinceLastStatement;
    private double pastPaidPrincipalYearToDate;
    private double pastPaidInterestSinceLastStatement;
    private double pastPaidInterestYearToDate;
    private double pastPaidEscrowSinceLastStatement;
    private double pastPaidEscrowYearToDate;
    private double pastPaidTotalSinceLastStatement;
    private double pastPaidTotalYearToDate;
    private String sourcePdfPath;
    private String importStatus = "importado_en_revision";
    private boolean reviewRequired = true;
    private boolean pendingReview = true;
    private String reviewNotes;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getLoanAlias() { return loanAlias; }
    public void setLoanAlias(String loanAlias) { this.loanAlias = loanAlias; }
    public String getServicerName() { return servicerName; }
    public void setServicerName(String servicerName) { this.servicerName = servicerName; }
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }
    public double getPaymentAmountDue() { return paymentAmountDue; }
    public void setPaymentAmountDue(double paymentAmountDue) { this.paymentAmountDue = paymentAmountDue; }
    public LocalDate getLateFeeDate() { return lateFeeDate; }
    public void setLateFeeDate(LocalDate lateFeeDate) { this.lateFeeDate = lateFeeDate; }
    public double getLateFeeAmount() { return lateFeeAmount; }
    public void setLateFeeAmount(double lateFeeAmount) { this.lateFeeAmount = lateFeeAmount; }
    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }
    public String getPropertyAddress() { return propertyAddress; }
    public void setPropertyAddress(String propertyAddress) { this.propertyAddress = propertyAddress; }
    public double getOriginalPrincipalBalance() { return originalPrincipalBalance; }
    public void setOriginalPrincipalBalance(double originalPrincipalBalance) { this.originalPrincipalBalance = originalPrincipalBalance; }
    public double getOutstandingPrincipalBalance() { return outstandingPrincipalBalance; }
    public void setOutstandingPrincipalBalance(double outstandingPrincipalBalance) { this.outstandingPrincipalBalance = outstandingPrincipalBalance; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
    public double getEscrowBalance() { return escrowBalance; }
    public void setEscrowBalance(double escrowBalance) { this.escrowBalance = escrowBalance; }
    public double getUnappliedFunds() { return unappliedFunds; }
    public void setUnappliedFunds(double unappliedFunds) { this.unappliedFunds = unappliedFunds; }
    public double getCurrentPaymentDue() { return currentPaymentDue; }
    public void setCurrentPaymentDue(double currentPaymentDue) { this.currentPaymentDue = currentPaymentDue; }
    public double getPrincipalDue() { return principalDue; }
    public void setPrincipalDue(double principalDue) { this.principalDue = principalDue; }
    public double getInterestDue() { return interestDue; }
    public void setInterestDue(double interestDue) { this.interestDue = interestDue; }
    public double getEscrowDue() { return escrowDue; }
    public void setEscrowDue(double escrowDue) { this.escrowDue = escrowDue; }
    public double getRegularMonthlyPayment() { return regularMonthlyPayment; }
    public void setRegularMonthlyPayment(double regularMonthlyPayment) { this.regularMonthlyPayment = regularMonthlyPayment; }
    public double getPastDueAmount() { return pastDueAmount; }
    public void setPastDueAmount(double pastDueAmount) { this.pastDueAmount = pastDueAmount; }
    public double getFees() { return fees; }
    public void setFees(double fees) { this.fees = fees; }
    public double getOtherFeesAndCharges() { return otherFeesAndCharges; }
    public void setOtherFeesAndCharges(double otherFeesAndCharges) { this.otherFeesAndCharges = otherFeesAndCharges; }
    public double getTotalDue() { return totalDue; }
    public void setTotalDue(double totalDue) { this.totalDue = totalDue; }
    public double getPastPaidPrincipalSinceLastStatement() { return pastPaidPrincipalSinceLastStatement; }
    public void setPastPaidPrincipalSinceLastStatement(double pastPaidPrincipalSinceLastStatement) { this.pastPaidPrincipalSinceLastStatement = pastPaidPrincipalSinceLastStatement; }
    public double getPastPaidPrincipalYearToDate() { return pastPaidPrincipalYearToDate; }
    public void setPastPaidPrincipalYearToDate(double pastPaidPrincipalYearToDate) { this.pastPaidPrincipalYearToDate = pastPaidPrincipalYearToDate; }
    public double getPastPaidInterestSinceLastStatement() { return pastPaidInterestSinceLastStatement; }
    public void setPastPaidInterestSinceLastStatement(double pastPaidInterestSinceLastStatement) { this.pastPaidInterestSinceLastStatement = pastPaidInterestSinceLastStatement; }
    public double getPastPaidInterestYearToDate() { return pastPaidInterestYearToDate; }
    public void setPastPaidInterestYearToDate(double pastPaidInterestYearToDate) { this.pastPaidInterestYearToDate = pastPaidInterestYearToDate; }
    public double getPastPaidEscrowSinceLastStatement() { return pastPaidEscrowSinceLastStatement; }
    public void setPastPaidEscrowSinceLastStatement(double pastPaidEscrowSinceLastStatement) { this.pastPaidEscrowSinceLastStatement = pastPaidEscrowSinceLastStatement; }
    public double getPastPaidEscrowYearToDate() { return pastPaidEscrowYearToDate; }
    public void setPastPaidEscrowYearToDate(double pastPaidEscrowYearToDate) { this.pastPaidEscrowYearToDate = pastPaidEscrowYearToDate; }
    public double getPastPaidTotalSinceLastStatement() { return pastPaidTotalSinceLastStatement; }
    public void setPastPaidTotalSinceLastStatement(double pastPaidTotalSinceLastStatement) { this.pastPaidTotalSinceLastStatement = pastPaidTotalSinceLastStatement; }
    public double getPastPaidTotalYearToDate() { return pastPaidTotalYearToDate; }
    public void setPastPaidTotalYearToDate(double pastPaidTotalYearToDate) { this.pastPaidTotalYearToDate = pastPaidTotalYearToDate; }
    public String getSourcePdfPath() { return sourcePdfPath; }
    public void setSourcePdfPath(String sourcePdfPath) { this.sourcePdfPath = sourcePdfPath; }
    public String getImportStatus() { return importStatus; }
    public void setImportStatus(String importStatus) { this.importStatus = importStatus; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public boolean isPendingReview() { return pendingReview; }
    public void setPendingReview(boolean pendingReview) { this.pendingReview = pendingReview; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
