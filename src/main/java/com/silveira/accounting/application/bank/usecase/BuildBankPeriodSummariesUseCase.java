package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.bank.BankMonthlyClosing;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.repositories.BankMonthlyClosingRepository;
import com.silveira.accounting.repositories.BankStatementPeriodRepository;
import com.silveira.accounting.repositories.BankTransactionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BuildBankPeriodSummariesUseCase {
    private final BankTransactionRepository transactions;
    private final BankStatementPeriodRepository periods;
    private final GetBankMonthlyClosingUseCase getMonthlyClosing;
    private final ApplyBankMonthlyClosingFallbackUseCase applyMonthlyClosingFallback;
    private final CalculateBankRowTotalsUseCase calculateRowTotals;

    public BuildBankPeriodSummariesUseCase(
        BankTransactionRepository transactions,
        BankStatementPeriodRepository periods,
        BankMonthlyClosingRepository closings
    ) {
        this.transactions = transactions;
        this.periods = periods;
        this.getMonthlyClosing = new GetBankMonthlyClosingUseCase(closings);
        this.applyMonthlyClosingFallback = new ApplyBankMonthlyClosingFallbackUseCase();
        this.calculateRowTotals = new CalculateBankRowTotalsUseCase();
    }

    public List<BankPeriodSummary> execute(String accountAliasFilter) {
        List<BankTransaction> transactionRows = transactions.find(null, null, null, null, accountAliasFilter);
        Map<String, List<BankTransaction>> bySource = new LinkedHashMap<>();
        for (BankTransaction transaction : transactionRows.stream()
            .sorted(Comparator.comparing(BankTransaction::getDate).thenComparing(BankTransaction::getId))
            .toList()) {
            bySource.computeIfAbsent(periodKey(transaction), key -> new ArrayList<>()).add(transaction);
        }
        for (BankStatementPeriod savedPeriod : periods.findByAccount(accountAliasFilter)) {
            bySource.computeIfAbsent(savedPeriod.sourcePdf(), key -> new ArrayList<>());
        }

        List<BankPeriodSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<BankTransaction>> entry : bySource.entrySet()) {
            summaries.add(summaryFrom(entry, accountAliasFilter));
        }
        summaries.sort(Comparator.comparing(summary -> summary.statementPeriod().periodStart()));
        return withChainedOpeningBalances(summaries);
    }

    private BankPeriodSummary summaryFrom(Map.Entry<String, List<BankTransaction>> entry, String accountAliasFilter) {
        List<BankTransaction> rows = entry.getValue();
        Optional<BankStatementPeriod> savedPeriod = periods.find(accountAliasFilter, entry.getKey());
        LocalDate start = rows.stream().map(BankTransaction::getDate).min(LocalDate::compareTo)
            .orElseGet(() -> savedPeriod.map(BankStatementPeriod::periodStart).orElse(LocalDate.now()));
        LocalDate end = rows.stream().map(BankTransaction::getDate).max(LocalDate::compareTo).orElse(start);
        if (rows.isEmpty() && savedPeriod.isPresent()) {
            end = savedPeriod.get().periodEnd();
        }
        String accountAlias = rows.stream()
            .map(BankTransaction::getAccountAlias)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElseGet(() -> savedPeriod.map(BankStatementPeriod::accountAlias).orElse(accountAliasFilter));
        String sourcePdf = rows.stream()
            .map(BankTransaction::getSourcePdf)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElseGet(() -> savedPeriod.map(BankStatementPeriod::sourcePdf).orElse(entry.getKey()));
        BankMonthlyClosing monthlyClosing = getMonthlyClosing.execute(accountAlias, end.getYear(), end.getMonthValue());
        BankStatementPeriod defaults = new BankStatementPeriod(
            accountAlias,
            sourcePdf,
            start,
            end,
            monthlyClosing.openingBalance(),
            monthlyClosing.statementEndingBalance()
        );
        LocalDate periodEnd = end;
        BankStatementPeriod period = savedPeriod
            .or(() -> periods.find(accountAlias, sourcePdf))
            .or(() -> isManualSource(sourcePdf) ? Optional.empty() : periods.findOverlapping(accountAlias, start, periodEnd))
            .map(saved -> applyMonthlyClosingFallback.execute(saved, monthlyClosing))
            .orElse(defaults);
        return new BankPeriodSummary(period, rows, calculateRowTotals.execute(rows));
    }

    private List<BankPeriodSummary> withChainedOpeningBalances(List<BankPeriodSummary> summaries) {
        List<BankPeriodSummary> chained = new ArrayList<>();
        double carryOver = 0;
        boolean first = true;
        for (BankPeriodSummary summary : summaries) {
            BankStatementPeriod period = summary.statementPeriod();
            if (!first && Math.abs(period.openingBalance()) < 0.001) {
                period = new BankStatementPeriod(
                    period.accountAlias(),
                    period.sourcePdf(),
                    period.periodStart(),
                    period.periodEnd(),
                    carryOver,
                    period.statementEndingBalance()
                );
            }
            BankPeriodSummary updated = new BankPeriodSummary(period, summary.transactions(), summary.totals());
            chained.add(updated);
            carryOver = period.openingBalance() + updated.totals().net();
            first = false;
        }
        return chained;
    }

    private boolean isManualSource(String sourcePdf) {
        return sourcePdf == null || sourcePdf.isBlank() || sourcePdf.toLowerCase(java.util.Locale.ROOT).contains("manual");
    }

    private String periodKey(BankTransaction transaction) {
        String source = transaction.getSourcePdf();
        if (source == null || source.isBlank()) {
            return "manual-" + transaction.getYear() + "-" + transaction.getMonth();
        }
        return source;
    }
}
