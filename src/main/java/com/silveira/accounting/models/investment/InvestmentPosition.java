package com.silveira.accounting.models.investment;

public class InvestmentPosition {
    private long statementId;
    private String symbol;
    private String description;
    private String assetType;
    private double quantity;
    private double price;
    private double marketValue;
    private double costBasis;
    private double unrealizedGainLoss;

    public long getStatementId() { return statementId; }
    public void setStatementId(long statementId) { this.statementId = statementId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getMarketValue() { return marketValue; }
    public void setMarketValue(double marketValue) { this.marketValue = marketValue; }
    public double getCostBasis() { return costBasis; }
    public void setCostBasis(double costBasis) { this.costBasis = costBasis; }
    public double getUnrealizedGainLoss() { return unrealizedGainLoss; }
    public void setUnrealizedGainLoss(double unrealizedGainLoss) { this.unrealizedGainLoss = unrealizedGainLoss; }
}
