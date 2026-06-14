package com.silveira.accounting.models.vehiclelease;

import java.time.LocalDate;

public class VehicleLeaseAccount {
    private long id;
    private String alias;
    private String providerName;
    private int vehicleYear;
    private String make;
    private String model;
    private String trim;
    private String accountNumber;
    private String vin;
    private LocalDate maturityDate;
    private String notes;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public int getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(int vehicleYear) { this.vehicleYear = vehicleYear; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getTrim() { return trim; }
    public void setTrim(String trim) { this.trim = trim; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
