package com.silveira.accounting.controllers.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.models.bank.BankMonthlyClosing;
import com.silveira.accounting.models.bank.BankTransaction;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class BankAccountDetailController {
    private final BankApplicationService bank;

    public BankAccountDetailController(BankApplicationService bank) {
        this.bank = bank;
    }

    public List<BankTransaction> findRows(Integer year, Integer month, String provider, String type, String accountAlias) {
        return bank.transactions().find(year, month, provider, type, accountAlias);
    }

    public SourceTotals totals(Integer year, Integer month, String accountAlias) {
        return bank.transactions().totals(year, month, accountAlias);
    }

    public SourceTotals totalsFromRows(List<BankTransaction> rows) {
        return bank.transactions().totalsFromRows(rows);
    }

    public List<BankPeriodSummary> periodSummaries(String accountAlias) {
        return bank.periods().summaries(accountAlias);
    }

    public List<BankPeriodSummary> pendingPeriods(String accountAlias) {
        return periodSummaries(accountAlias).stream()
            .filter(period -> period.totals().pendingCount() > 0)
            .toList();
    }

    public double reviewedProviderIncome(int year, int month, String accountAlias, String provider) {
        return bank.transactions().find(year, month, provider, null, accountAlias).stream()
            .filter(transaction -> !transaction.isPendingReview())
            .filter(transaction -> transaction.getAmount() > 0)
            .mapToDouble(BankTransaction::getAmount)
            .sum();
    }

    public double firstOpeningBalance(Integer year, String accountAlias) {
        Optional<Double> periodOpening = periodSummaries(accountAlias).stream()
            .sorted(Comparator.comparing(summary -> summary.statementPeriod().periodStart()))
            .map(summary -> summary.statementPeriod().openingBalance())
            .filter(value -> Math.abs(value) > 0.0001)
            .findFirst();
        if (periodOpening.isPresent()) {
            return periodOpening.get();
        }
        return bank.transactions().monthlyTotals(year, accountAlias).stream()
            .map(total -> bank.periods().findClosing(accountAlias, total.year(), total.month()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(closing -> Math.abs(closing.openingBalance()) > 0.0001)
            .min(Comparator.comparingInt((BankMonthlyClosing closing) -> closing.year()).thenComparingInt(BankMonthlyClosing::month))
            .map(BankMonthlyClosing::openingBalance)
            .orElse(0.0);
    }
}
