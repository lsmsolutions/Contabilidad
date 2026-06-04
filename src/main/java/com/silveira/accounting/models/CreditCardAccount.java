package com.silveira.accounting.models;

public class CreditCardAccount {
    private long id;
    private String alias;
    private String bankName;
    private String cardName;
    private String accountLastDigits;
    private String notes;

    public CreditCardAccount(long id, String alias, String bankName, String cardName, String accountLastDigits, String notes) {
        this.id = id;
        this.alias = alias;
        this.bankName = bankName;
        this.cardName = cardName;
        this.accountLastDigits = accountLastDigits;
        this.notes = notes;
    }

    public long getId() { return id; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public String getAccountLastDigits() { return accountLastDigits; }
    public void setAccountLastDigits(String accountLastDigits) { this.accountLastDigits = accountLastDigits; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
