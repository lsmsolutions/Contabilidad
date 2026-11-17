package com.silveira.accounting.application.dashboard;

import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.NylRecord;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DashboardApplicationService {
    private final DashboardGateway gateway;

    public DashboardApplicationService(DashboardGateway gateway) {
        this.gateway = gateway;
    }

    public DashboardSnapshot snapshot() {
        Map<String, List<com.silveira.accounting.application.bank.dto.BankPeriodSummary>> bankPeriods = new LinkedHashMap<>();
        var bankAccounts = gateway.bankAccounts();
        for (var account : bankAccounts) {
            bankPeriods.put(account.getAlias(), gateway.bankPeriodSummaries(account.getAlias()));
        }
        List<NylRecord> records = gateway.nylRecords().stream()
            .filter(record -> !record.isPendingReview())
            .toList();
        Map<Integer, Double> monthlyCommissions = records.stream()
            .filter(record -> record.getRecordType().equals("comision") || record.getRecordType().equals("credito"))
            .collect(Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(NylRecord::getAmount)));
        Map<Integer, Double> monthlyDeductions = records.stream()
            .filter(record -> record.getRecordType().equals("deduccion"))
            .collect(Collectors.groupingBy(
                NylRecord::getMonth,
                Collectors.summingDouble(record -> Math.abs(record.getAmount()))
            ));
        List<MortgageStatement> mortgageStatements = gateway.mortgageStatements("CasaH").stream()
            .filter(statement -> !statement.isPendingReview())
            .sorted(Comparator.comparing(
                MortgageStatement::getStatementDate,
                Comparator.nullsLast(LocalDate::compareTo)
            ))
            .toList();
        List<DashboardSnapshot.LatestStatement> latestStatements = latestStatements(bankAccounts);
        return new DashboardSnapshot(
            bankAccounts,
            bankPeriods,
            monthlyCommissions,
            monthlyDeductions,
            mortgageStatements,
            latestStatements
        );
    }

    private List<DashboardSnapshot.LatestStatement> latestStatements(List<com.silveira.accounting.models.bank.BankAccount> bankAccounts) {
        List<DashboardSnapshot.LatestStatement> rows = new ArrayList<>();
        for (var account : bankAccounts) {
            rows.add(latestStatement("Bank", account.getAlias(), latestBankStatement(account.getAlias()).orElse(null)));
        }
        for (var account : gateway.cardAccounts()) {
            rows.add(latestStatement("Card", account.getAlias(), latestCardStatement(account.getAlias()).orElse(null)));
        }
        for (String alias : gateway.mortgageAliases()) {
            rows.add(latestStatement("Mortgage", alias, latestMortgageStatement(alias).orElse(null)));
        }
        for (var account : gateway.investmentAccounts()) {
            rows.add(latestStatement("Investment", account.getAlias(), latestInvestmentStatement(account.getAlias()).orElse(null)));
        }
        for (var account : gateway.vehicleLeaseAccounts()) {
            rows.add(latestStatement("Vehicle Lease", account.getAlias(), latestVehicleLeaseStatement(account.getAlias()).orElse(null)));
        }
        return rows;
    }

    private DashboardSnapshot.LatestStatement latestStatement(String source, String account, LocalDate date) {
        return new DashboardSnapshot.LatestStatement(source, account, date, missingPeriods(date));
    }

    private String missingPeriods(LocalDate latestDate) {
        if (latestDate == null) {
            return "No statements registered";
        }
        YearMonth next = YearMonth.from(latestDate).plusMonths(1);
        YearMonth current = YearMonth.now();
        if (next.isAfter(current)) {
            return "Up to date";
        }
        List<String> months = new ArrayList<>();
        for (YearMonth month = next; !month.isAfter(current); month = month.plusMonths(1)) {
            months.add(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + month.getYear());
        }
        return String.join(", ", months);
    }

    private Optional<LocalDate> latestBankStatement(String alias) {
        return gateway.bankPeriodSummaries(alias).stream()
            .map(summary -> summary.statementPeriod().periodEnd())
            .filter(date -> date != null)
            .max(LocalDate::compareTo);
    }

    private Optional<LocalDate> latestCardStatement(String alias) {
        return gateway.cardStatements(alias).stream()
            .map(com.silveira.accounting.models.CreditCardStatement::getStatementEndDate)
            .filter(date -> date != null)
            .max(LocalDate::compareTo);
    }

    private Optional<LocalDate> latestMortgageStatement(String alias) {
        return gateway.mortgageStatements(alias).stream()
            .map(MortgageStatement::getStatementDate)
            .filter(date -> date != null)
            .max(LocalDate::compareTo);
    }

    private Optional<LocalDate> latestInvestmentStatement(String alias) {
        return gateway.investmentStatements(alias).stream()
            .map(com.silveira.accounting.models.investment.InvestmentStatement::getPeriodEnd)
            .filter(date -> date != null)
            .max(LocalDate::compareTo);
    }

    private Optional<LocalDate> latestVehicleLeaseStatement(String alias) {
        return gateway.vehicleLeaseStatements(alias).stream()
            .map(com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement::getStatementDate)
            .filter(date -> date != null)
            .max(LocalDate::compareTo);
    }
}
