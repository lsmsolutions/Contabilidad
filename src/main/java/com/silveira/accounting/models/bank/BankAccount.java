package com.silveira.accounting.models.bank;

public class BankAccount {
    private long id;
    private String alias;
    private String accountNumber;
    private String bankName;
    private String accountType;
    private String notes;

    public BankAccount(long id, String alias, String accountNumber, String bankName, String accountType, String notes) {
        this.id = id;
        this.alias = alias;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.accountType = accountType;
        this.notes = notes;
    }

    public long getId() { return id; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
