package com.silveira.accounting.application.business;

import com.silveira.accounting.models.business.Business;
import com.silveira.accounting.models.business.BusinessExpense;
import java.util.List;

public class BusinessApplicationService {
    private final BusinessGateway businesses;
    private final BusinessExpenseGateway expenses;

    public BusinessApplicationService(BusinessGateway businesses, BusinessExpenseGateway expenses) {
        this.businesses = businesses;
        this.expenses = expenses;
    }

    public List<Business> businesses() { return businesses.findAll(); }
    public long createBusiness(String name) { return businesses.create(name == null ? "" : name.trim()); }
    public void renameBusiness(long id, String name) { businesses.rename(id, name == null ? "" : name.trim()); }
    public void deleteBusiness(long id) { businesses.delete(id); }
    public List<BusinessExpense> expenses(long businessId, Integer year, Integer month) { return expenses.find(businessId, year, month); }
    public long saveExpense(BusinessExpense expense) { return expenses.save(expense); }
    public void updateExpense(BusinessExpense expense) { expenses.update(expense); }
    public void deleteExpense(long id) { expenses.delete(id); }
}
