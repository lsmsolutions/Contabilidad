package com.silveira.accounting.application.mortgage.service;

import com.silveira.accounting.models.HouseExpense;
import com.silveira.accounting.repositories.mortgage.HouseExpenseRepository;
import java.util.List;

public class HouseExpenseApplicationService {
    private final HouseExpenseRepository repository;

    public HouseExpenseApplicationService(HouseExpenseRepository repository) {
        this.repository = repository;
    }

    public long save(HouseExpense expense) {
        return repository.save(expense);
    }

    public List<HouseExpense> findByLoan(String loanAlias, Integer year, Integer month) {
        return repository.findByLoan(loanAlias, year, month);
    }

    public void update(HouseExpense expense) {
        repository.update(expense);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
