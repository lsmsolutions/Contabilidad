package com.silveira.accounting.models.investment;

public class InvestmentAllocation {
    private long statementId;
    private String category;
    private double marketValue;
    private double percentage;

    public long getStatementId() { return statementId; }
    public void setStatementId(long statementId) { this.statementId = statementId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getMarketValue() { return marketValue; }
    public void setMarketValue(double marketValue) { this.marketValue = marketValue; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}
