package com.silveira.accounting.application.dashboard;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.bank.BankAccount;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardSnapshot(
    List<BankAccount> bankAccounts,
    Map<String, List<BankPeriodSummary>> bankPeriods,
    Map<Integer, Double> monthlyCommissions,
    Map<Integer, Double> monthlyDeductions,
    List<MortgageStatement> mortgageStatements,
    List<LatestStatement> latestStatements
) {
    public record LatestStatement(String source, String account, LocalDate date, String missingPeriods) {
    }
}
