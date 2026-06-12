package com.silveira.accounting.application.bank.service;

import com.silveira.accounting.application.bank.usecase.CalculateBankNylIncomeUseCase;
import com.silveira.accounting.application.bank.usecase.CalculateBankRowTotalsUseCase;
import com.silveira.accounting.application.bank.usecase.NormalizeBankTransactionSignUseCase;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.repositories.bank.BankTransactionRepository;

import java.util.List;
import java.util.Set;

public class BankTransactionApplicationService {
    private final BankTransactionRepository repository;
    private final CalculateBankNylIncomeUseCase calculateNylIncome = new CalculateBankNylIncomeUseCase();
    private final CalculateBankRowTotalsUseCase calculateRowTotals = new CalculateBankRowTotalsUseCase();
    private final NormalizeBankTransactionSignUseCase normalizeSign = new NormalizeBankTransactionSignUseCase();

    public BankTransactionApplicationService(BankTransactionRepository repository) {
        this.repository = repository;
    }

    public List<BankTransaction> find(Integer year, Integer month, String provider, String type) {
        return repository.find(year, month, provider, type);
    }

    public List<BankTransaction> find(Integer year, Integer month, String provider, String type, String accountAlias) {
        return repository.find(year, month, provider, type, accountAlias);
    }

    public List<BankTransaction> findPendingReview() {
        return repository.findPendingReview();
    }

    public Set<String> existingFingerprints(List<BankTransaction> transactions) {
        return repository.existingFingerprints(transactions);
    }

    public SourceTotals totals(Integer year, Integer month) {
        return repository.totals(year, month);
    }

    public SourceTotals totals(Integer year, Integer month, String accountAlias) {
        return repository.totals(year, month, accountAlias);
    }

    public List<MonthlySourceTotals> monthlyTotals(Integer year, String accountAlias) {
        return repository.monthlyTotals(year, accountAlias);
    }

    public SourceTotals totalsFromRows(List<BankTransaction> rows) {
        return calculateRowTotals.execute(rows);
    }

    public double nylIncome(List<BankTransaction> rows) {
        return calculateNylIncome.execute(rows);
    }

    public void normalizeSign(BankTransaction transaction) {
        normalizeSign.execute(transaction);
    }

    public long save(BankTransaction transaction) {
        return repository.save(transaction);
    }

    public int saveAll(List<BankTransaction> transactions) {
        return repository.saveAll(transactions);
    }

    public void update(BankTransaction transaction) {
        repository.updateRecord(transaction);
    }

    public void updateProvider(long id, String provider, String movementType) {
        repository.updateProvider(id, provider, movementType);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
