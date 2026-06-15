package com.silveira.accounting.application.mortgage.service;

import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.repositories.mortgage.MortgageStatementRepository;
import java.util.List;

public class MortgageStatementApplicationService {
    private final MortgageStatementRepository repository;

    public MortgageStatementApplicationService(MortgageStatementRepository repository) {
        this.repository = repository;
    }

    public long save(MortgageStatement statement) {
        return repository.save(statement);
    }

    public List<String> findLoanAliases() {
        return repository.findLoanAliases();
    }

    public void saveLoan(String alias, String servicerName, String loanNumber, String propertyAddress, String notes) {
        repository.saveLoan(alias, servicerName, loanNumber, propertyAddress, notes);
    }

    public List<MortgageStatement> findByLoan(String alias, Integer year, Integer month) {
        return repository.findByLoan(alias, year, month);
    }

    public SourceTotals totals(String alias, Integer year, Integer month) {
        return repository.totals(alias, year, month);
    }

    public List<MonthlySourceTotals> monthlyTotals(String alias, Integer year) {
        return repository.monthlyTotals(alias, year);
    }

    public void updateRecord(MortgageStatement statement) {
        repository.updateRecord(statement);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
