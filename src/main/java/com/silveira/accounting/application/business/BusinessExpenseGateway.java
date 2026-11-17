package com.silveira.accounting.application.business;

import com.silveira.accounting.models.business.BusinessExpense;
import java.util.List;

public interface BusinessExpenseGateway {
    List<BusinessExpense> find(long businessId, Integer year, Integer month);
    long save(BusinessExpense expense);
    void update(BusinessExpense expense);
    void delete(long id);
}
