package com.silveira.accounting.application.card.service;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.repositories.card.CreditCardStatementRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class CardStatementApplicationService {
    private final CreditCardStatementRepository repository;

    public CardStatementApplicationService(CreditCardStatementRepository repository) {
        this.repository = repository;
    }

    public long save(CreditCardStatement statement) {
        return repository.save(statement);
    }

    public CreditCardStatement createManual(String alias, LocalDate today) {
        CreditCardStatement statement = new CreditCardStatement();
        statement.setAccountAlias(alias);
        statement.setStatementStartDate(today.withDayOfMonth(1));
        statement.setStatementEndDate(today);
        statement.setPaymentDueDate(today.plusDays(21));
        statement.setPendingReview(true);
        statement.setReviewRequired(true);
        statement.setReviewNotes("Anadido manualmente");
        statement.setId(repository.save(statement));
        return statement;
    }

    public void saveVisible(List<CreditCardStatement> statements) {
        for (CreditCardStatement statement : statements) {
            if (statement.getId() > 0) {
                repository.updateRecord(statement);
            } else {
                statement.setId(repository.save(statement));
            }
        }
    }

    public List<CreditCardStatement> findByAccount(String alias) {
        return repository.findByAccount(alias);
    }

    public Optional<CreditCardStatement> findById(long id) {
        return repository.findById(id);
    }

    public List<CreditCardStatement> findByAccount(String alias, Integer year, Integer month) {
        return repository.findByAccount(alias, year, month);
    }

    public void updateRecord(CreditCardStatement statement) {
        repository.updateRecord(statement);
    }

    public void updatePeriod(long id, LocalDate start, LocalDate end) {
        repository.updatePeriod(id, start, end);
    }

    public SourceTotals totals(String alias, Integer year, Integer month) {
        return repository.totals(alias, year, month);
    }

    public List<MonthlySourceTotals> monthlyTotals(String alias, Integer year) {
        return repository.monthlyTotals(alias, year);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
