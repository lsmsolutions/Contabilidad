package com.silveira.accounting.models.investment;

import java.time.LocalDate;

public class InvestmentStatement {
    private long id;
    private String accountAlias;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private double beginningValue;
    private double endingValue;
    private double deposits;
    private double withdrawals;
    private double dividendsInterest;
    private double marketChange;
    private double expenses;
    private double unrealizedGainLoss;
    private String sourcePdfPath;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getAccountAlias() { return accountAlias; }
    public void setAccountAlias(String accountAlias) { this.accountAlias = accountAlias; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public double getBeginningValue() { return beginningValue; }
    public void setBeginningValue(double beginningValue) { this.beginningValue = beginningValue; }
    public double getEndingValue() { return endingValue; }
    public void setEndingValue(double endingValue) { this.endingValue = endingValue; }
    public double getDeposits() { return deposits; }
    public void setDeposits(double deposits) { this.deposits = deposits; }
    public double getWithdrawals() { return withdrawals; }
    public void setWithdrawals(double withdrawals) { this.withdrawals = withdrawals; }
    public double getDividendsInterest() { return dividendsInterest; }
    public void setDividendsInterest(double dividendsInterest) { this.dividendsInterest = dividendsInterest; }
    public double getMarketChange() { return marketChange; }
    public void setMarketChange(double marketChange) { this.marketChange = marketChange; }
    public double getExpenses() { return expenses; }
    public void setExpenses(double expenses) { this.expenses = expenses; }
    public double getUnrealizedGainLoss() { return unrealizedGainLoss; }
    public void setUnrealizedGainLoss(double unrealizedGainLoss) { this.unrealizedGainLoss = unrealizedGainLoss; }
    public String getSourcePdfPath() { return sourcePdfPath; }
    public void setSourcePdfPath(String sourcePdfPath) { this.sourcePdfPath = sourcePdfPath; }
}
