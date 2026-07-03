package com.silveira.accounting.application.dashboard;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.bank.BankAccount;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record DashboardSnapshot(
    List<CardPayment> cardPayments,
    List<BankAccount> bankAccounts,
    Map<String, List<BankPeriodSummary>> bankPeriods,
    Map<Integer, Double> monthlyCommissions,
    Map<Integer, Double> monthlyDeductions,
    List<MortgageStatement> mortgageStatements
) {
    public record CardPayment(CreditCardAccount account, Optional<CreditCardStatement> latestStatement) {
    }
}
