package com.silveira.accounting.application.bank.service;

import com.silveira.accounting.application.bank.dto.BankLedgerRow;
import com.silveira.accounting.application.bank.dto.BankLedgerSnapshot;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.bank.BankAccount;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

public class BankLedgerApplicationService {
    private final BankAccountApplicationService accounts;
    private final BankPeriodApplicationService periods;

    public BankLedgerApplicationService(BankAccountApplicationService accounts, BankPeriodApplicationService periods) {
        this.accounts = accounts;
        this.periods = periods;
    }

    public BankLedgerSnapshot snapshot(int year, int month) {
        List<BankLedgerRow> accountRows = new ArrayList<>();
        List<BankLedgerRow> reyRows = new ArrayList<>();
        List<BankLedgerRow> accumulatedAccountRows = new ArrayList<>();
        List<BankLedgerRow> accumulatedReyRows = new ArrayList<>();
        for (BankAccount account : accounts.list()) {
            BankLedgerRow row = rowFor(account, year, month);
            BankLedgerRow accumulatedRow = accumulatedRowFor(account, year, month);
            if (isReyAccount(account)) {
                reyRows.add(row);
                accumulatedReyRows.add(accumulatedRow);
            } else {
                accountRows.add(row);
                accumulatedAccountRows.add(accumulatedRow);
            }
        }
        accountRows.sort(Comparator.comparing(BankLedgerRow::account, String.CASE_INSENSITIVE_ORDER));
        reyRows.sort(Comparator.comparing(BankLedgerRow::account, String.CASE_INSENSITIVE_ORDER));
        accumulatedAccountRows.sort(Comparator.comparing(BankLedgerRow::account, String.CASE_INSENSITIVE_ORDER));
        accumulatedReyRows.sort(Comparator.comparing(BankLedgerRow::account, String.CASE_INSENSITIVE_ORDER));
        return new BankLedgerSnapshot(year, month, availableYears(), accountRows, reyRows, accumulatedAccountRows, accumulatedReyRows);
    }

    public List<Integer> availableYears() {
        TreeSet<Integer> years = new TreeSet<>();
        for (BankAccount account : accounts.list()) {
            periods.summaries(account.getAlias()).stream()
                .map(summary -> summary.statementPeriod().periodEnd())
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .forEach(years::add);
        }
        int currentYear = LocalDate.now().getYear();
        years.add(currentYear);
        return years.reversed().stream().toList();
    }

    private BankLedgerRow rowFor(BankAccount account, int year, int month) {
        Optional<BankPeriodSummary> summary = periods.summaries(account.getAlias()).stream()
            .filter(candidate -> candidate.statementPeriod().periodEnd() != null)
            .filter(candidate -> candidate.statementPeriod().periodEnd().getYear() == year)
            .filter(candidate -> candidate.statementPeriod().periodEnd().getMonthValue() == month)
            .max(Comparator.comparing(candidate -> candidate.statementPeriod().periodEnd()));
        double deposits = summary.map(value -> value.totals().income()).orElse(0.0);
        double withdrawals = summary.map(value -> value.totals().expenses()).orElse(0.0);
        double calculatedBalance = summary
            .map(value -> value.statementPeriod().openingBalance() + value.totals().net())
            .orElse(0.0);
        return new BankLedgerRow(
            text(account.getAlias()),
            text(account.getBankName()),
            lastDigits(account.getAccountNumber()),
            deposits,
            withdrawals,
            calculatedBalance
        );
    }

    private BankLedgerRow accumulatedRowFor(BankAccount account, int year, int month) {
        List<BankPeriodSummary> summaries = periods.summaries(account.getAlias()).stream()
            .filter(candidate -> candidate.statementPeriod().periodEnd() != null)
            .filter(candidate -> candidate.statementPeriod().periodEnd().getYear() == year)
            .filter(candidate -> candidate.statementPeriod().periodEnd().getMonthValue() <= month)
            .sorted(Comparator.comparing(candidate -> candidate.statementPeriod().periodEnd()))
            .toList();
        double deposits = summaries.stream().mapToDouble(value -> value.totals().income()).sum();
        double withdrawals = summaries.stream().mapToDouble(value -> value.totals().expenses()).sum();
        double calculatedBalance = summaries.stream()
            .reduce((previous, current) -> current)
            .map(value -> value.statementPeriod().openingBalance() + value.totals().net())
            .orElse(0.0);
        return new BankLedgerRow(
            text(account.getAlias()),
            text(account.getBankName()),
            lastDigits(account.getAccountNumber()),
            deposits,
            withdrawals,
            calculatedBalance
        );
    }

    private boolean isReyAccount(BankAccount account) {
        return text(account.getAlias()).toLowerCase(Locale.ROOT).contains("rey");
    }

    private String lastDigits(String value) {
        String digits = text(value).replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return digits;
        }
        return digits.substring(digits.length() - 4);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
