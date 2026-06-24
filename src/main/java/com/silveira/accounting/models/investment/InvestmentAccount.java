package com.silveira.accounting.models.investment;

public class InvestmentAccount {
    private long id;
    private String alias;
    private String providerName;
    private String accountType;
    private String accountNumber;
    private String notes;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
