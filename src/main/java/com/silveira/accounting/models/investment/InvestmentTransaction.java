package com.silveira.accounting.models.investment;

import java.time.LocalDate;

public class InvestmentTransaction {
    private long statementId;
    private LocalDate transactionDate;
    private String category;
    private String action;
    private String symbol;
    private String description;
    private double quantity;
    private double price;
    private double amount;
    private double realizedGainLoss;
    private boolean subtotal;

    public long getStatementId() { return statementId; }
    public void setStatementId(long statementId) { this.statementId = statementId; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getRealizedGainLoss() { return realizedGainLoss; }
    public void setRealizedGainLoss(double realizedGainLoss) { this.realizedGainLoss = realizedGainLoss; }
    public boolean isSubtotal() { return subtotal; }
    public void setSubtotal(boolean subtotal) { this.subtotal = subtotal; }
}
