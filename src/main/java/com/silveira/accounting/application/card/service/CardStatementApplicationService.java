package com.silveira.accounting.application.card.service;

import com.silveira.accounting.application.card.dto.CardPeriodSummary;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.repositories.card.CreditCardStatementRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

    public List<CardPeriodSummary> periodSummaries(String alias) {
        return repository.monthlyTotals(alias, null).stream()
            .map(total -> periodSummary(alias, total))
            .toList();
    }

    public String periodTitle(List<CreditCardStatement> statements, MonthlySourceTotals fallback) {
        LocalDate start = statements.stream()
            .map(CreditCardStatement::getStatementStartDate)
            .filter(java.util.Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(null);
        LocalDate end = statements.stream()
            .map(CreditCardStatement::getStatementEndDate)
            .filter(java.util.Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);
        if (start != null && end != null) {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return start.format(format) + " - " + end.format(format);
        }
        return monthName(fallback.month()) + " " + fallback.year();
    }

    public void deletePeriod(List<CreditCardStatement> statements) {
        for (CreditCardStatement statement : statements) {
            if (statement.getId() > 0) {
                repository.delete(statement.getId());
            }
        }
    }

    private CardPeriodSummary periodSummary(String alias, MonthlySourceTotals total) {
        List<CreditCardStatement> statements = repository.findByAccount(alias, total.year(), total.month());
        List<CreditCardStatement> reviewed = statements.stream()
            .filter(statement -> !statement.isPendingReview())
            .toList();
        CreditCardStatement opening = statements.stream()
            .min(Comparator
                .comparing(CreditCardStatement::getStatementStartDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparingLong(CreditCardStatement::getId))
            .orElse(null);
        CreditCardStatement closing = statements.stream()
            .max(Comparator
                .comparing(CreditCardStatement::getStatementEndDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(CreditCardStatement::getId))
            .orElse(null);
        return new CardPeriodSummary(
            total.year(),
            total.month(),
            periodTitle(statements, total),
            statements,
            opening,
            closing,
            reviewed.stream().mapToDouble(CreditCardStatement::getPayments).sum(),
            reviewed.stream().mapToDouble(CreditCardStatement::getTransactions).sum(),
            reviewed.stream().mapToDouble(CreditCardStatement::getInterestCharged).sum()
        );
    }

    private String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> "Mes " + month;
        };
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
