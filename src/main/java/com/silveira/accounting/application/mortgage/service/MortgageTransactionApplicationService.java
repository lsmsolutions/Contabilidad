package com.silveira.accounting.application.mortgage.service;

import com.silveira.accounting.models.MortgageTransaction;
import com.silveira.accounting.repositories.mortgage.MortgageTransactionRepository;
import java.util.List;

public class MortgageTransactionApplicationService {
    private final MortgageTransactionRepository repository;

    public MortgageTransactionApplicationService(MortgageTransactionRepository repository) {
        this.repository = repository;
    }

    public void saveAll(long statementId, List<MortgageTransaction> rows) {
        repository.saveAll(statementId, rows);
    }

    public long save(long statementId, MortgageTransaction row) {
        return repository.save(statementId, row);
    }

    public List<MortgageTransaction> findByLoan(String alias, Integer year, Integer month) {
        return repository.findByLoan(alias, year, month);
    }

    public void update(MortgageTransaction row) {
        repository.update(row);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
