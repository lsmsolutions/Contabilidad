package com.silveira.accounting.models;

import java.time.LocalDate;

public class CreditCardStatement {
    private long id;
    private String accountAlias;
    private String bankName;
    private String cardName;
    private String accountLastDigits;
    private LocalDate statementStartDate;
    private LocalDate statementEndDate;
    private LocalDate paymentDueDate;
    private LocalDate nextClosingDate;
    private double previousBalance;
    private double payments;
    private double otherCredits;
    private double transactions;
    private double balanceTransfers;
    private double cashAdvances;
    private double feesCharged;
    private double interestCharged;
    private double newBalance;
    private double minimumPaymentDue;
    private double creditLimit;
    private double availableCredit;
    private double cashAdvanceLimit;
    private double availableCashAdvanceCredit;
    private double rewardsBalance;
    private double rewardsPreviousBalance;
    private double rewardsEarned;
    private double rewardsRedeemed;
    private String sourcePdfPath;
    private String importStatus;
    private boolean reviewRequired = true;
    private boolean pendingReview = true;
    private String reviewNotes;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getAccountAlias() { return accountAlias; }
    public void setAccountAlias(String accountAlias) { this.accountAlias = accountAlias; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public String getAccountLastDigits() { return accountLastDigits; }
    public void setAccountLastDigits(String accountLastDigits) { this.accountLastDigits = accountLastDigits; }
    public LocalDate getStatementStartDate() { return statementStartDate; }
    public void setStatementStartDate(LocalDate statementStartDate) { this.statementStartDate = statementStartDate; }
    public LocalDate getStatementEndDate() { return statementEndDate; }
    public void setStatementEndDate(LocalDate statementEndDate) { this.statementEndDate = statementEndDate; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }
    public LocalDate getNextClosingDate() { return nextClosingDate; }
    public void setNextClosingDate(LocalDate nextClosingDate) { this.nextClosingDate = nextClosingDate; }
    public double getPreviousBalance() { return previousBalance; }
    public void setPreviousBalance(double previousBalance) { this.previousBalance = previousBalance; }
    public double getPayments() { return payments; }
    public void setPayments(double payments) { this.payments = payments; }
    public double getOtherCredits() { return otherCredits; }
    public void setOtherCredits(double otherCredits) { this.otherCredits = otherCredits; }
    public double getTransactions() { return transactions; }
    public void setTransactions(double transactions) { this.transactions = transactions; }
    public double getBalanceTransfers() { return balanceTransfers; }
    public void setBalanceTransfers(double balanceTransfers) { this.balanceTransfers = balanceTransfers; }
    public double getCashAdvances() { return cashAdvances; }
    public void setCashAdvances(double cashAdvances) { this.cashAdvances = cashAdvances; }
    public double getFeesCharged() { return feesCharged; }
    public void setFeesCharged(double feesCharged) { this.feesCharged = feesCharged; }
    public double getInterestCharged() { return interestCharged; }
    public void setInterestCharged(double interestCharged) { this.interestCharged = interestCharged; }
    public double getNewBalance() { return newBalance; }
    public void setNewBalance(double newBalance) { this.newBalance = newBalance; }
    public double getMinimumPaymentDue() { return minimumPaymentDue; }
    public void setMinimumPaymentDue(double minimumPaymentDue) { this.minimumPaymentDue = minimumPaymentDue; }
    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }
    public double getAvailableCredit() { return availableCredit; }
    public void setAvailableCredit(double availableCredit) { this.availableCredit = availableCredit; }
    public double getCashAdvanceLimit() { return cashAdvanceLimit; }
    public void setCashAdvanceLimit(double cashAdvanceLimit) { this.cashAdvanceLimit = cashAdvanceLimit; }
    public double getAvailableCashAdvanceCredit() { return availableCashAdvanceCredit; }
    public void setAvailableCashAdvanceCredit(double availableCashAdvanceCredit) { this.availableCashAdvanceCredit = availableCashAdvanceCredit; }
    public double getRewardsBalance() { return rewardsBalance; }
    public void setRewardsBalance(double rewardsBalance) { this.rewardsBalance = rewardsBalance; }
    public double getRewardsPreviousBalance() { return rewardsPreviousBalance; }
    public void setRewardsPreviousBalance(double rewardsPreviousBalance) { this.rewardsPreviousBalance = rewardsPreviousBalance; }
    public double getRewardsEarned() { return rewardsEarned; }
    public void setRewardsEarned(double rewardsEarned) { this.rewardsEarned = rewardsEarned; }
    public double getRewardsRedeemed() { return rewardsRedeemed; }
    public void setRewardsRedeemed(double rewardsRedeemed) { this.rewardsRedeemed = rewardsRedeemed; }
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
