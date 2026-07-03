package com.silveira.accounting.application.dashboard;

import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.NylRecord;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardApplicationService {
    private final DashboardGateway gateway;

    public DashboardApplicationService(DashboardGateway gateway) {
        this.gateway = gateway;
    }

    public DashboardSnapshot snapshot() {
        List<DashboardSnapshot.CardPayment> cardPayments = gateway.cardAccounts().stream()
            .map(account -> new DashboardSnapshot.CardPayment(
                account,
                gateway.cardStatements(account.getAlias()).stream().findFirst()
            ))
            .toList();
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
        return new DashboardSnapshot(
            cardPayments,
            bankAccounts,
            bankPeriods,
            monthlyCommissions,
            monthlyDeductions,
            mortgageStatements
        );
    }
}
