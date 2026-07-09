package com.silveira.accounting.application.mortgage.service;

import com.silveira.accounting.application.mortgage.dto.MortgageLedgerRow;
import com.silveira.accounting.application.mortgage.dto.MortgageLedgerSnapshot;
import com.silveira.accounting.models.MortgageStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class MortgageLedgerApplicationService {
    private final MortgageStatementApplicationService statements;

    public MortgageLedgerApplicationService(MortgageStatementApplicationService statements) {
        this.statements = statements;
    }

    public MortgageLedgerSnapshot snapshot(String requestedLoan, int year) {
        List<String> loans = statements.findLoanAliases();
        String selectedLoan = selectedLoan(requestedLoan, loans);
        return new MortgageLedgerSnapshot(
            selectedLoan,
            year,
            loans,
            availableYears(selectedLoan),
            initialDebt(selectedLoan),
            rows(selectedLoan, year)
        );
    }

    private String selectedLoan(String requestedLoan, List<String> loans) {
        if (requestedLoan != null && !requestedLoan.isBlank() && loans.contains(requestedLoan)) {
            return requestedLoan;
        }
        return loans.isEmpty() ? "" : loans.get(0);
    }

    private List<Integer> availableYears(String loan) {
        TreeSet<Integer> years = new TreeSet<>();
        if (loan != null && !loan.isBlank()) {
            statements.findByLoan(loan, null, null).stream()
                .map(MortgageStatement::getStatementDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .forEach(years::add);
        }
        years.add(LocalDate.now().getYear());
        return years.reversed().stream().toList();
    }

    private double initialDebt(String loan) {
        if (loan == null || loan.isBlank()) {
            return 0.0;
        }
        return statements.findByLoan(loan, null, null).stream()
            .filter(statement -> statement.getStatementDate() != null)
            .sorted(Comparator.comparing(MortgageStatement::getStatementDate))
            .mapToDouble(MortgageStatement::getOriginalPrincipalBalance)
            .filter(value -> Math.abs(value) > 0.001)
            .findFirst()
            .orElse(0.0);
    }

    private List<MortgageLedgerRow> rows(String loan, int year) {
        List<MortgageLedgerRow> rows = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            List<MortgageStatement> monthStatements = statements.findByLoan(loan, year, month);
            rows.add(new MortgageLedgerRow(
                month,
                paidPrincipal(monthStatements),
                outstandingPrincipal(monthStatements),
                paidInterest(monthStatements),
                paidEscrow(monthStatements),
                !monthStatements.isEmpty()
            ));
        }
        return rows;
    }

    private double paidPrincipal(List<MortgageStatement> monthStatements) {
        return monthStatements.stream()
            .mapToDouble(MortgageStatement::getPastPaidPrincipalSinceLastStatement)
            .sum();
    }

    private double paidInterest(List<MortgageStatement> monthStatements) {
        return monthStatements.stream()
            .mapToDouble(MortgageStatement::getPastPaidInterestSinceLastStatement)
            .sum();
    }

    private double paidEscrow(List<MortgageStatement> monthStatements) {
        return monthStatements.stream()
            .mapToDouble(MortgageStatement::getPastPaidEscrowSinceLastStatement)
            .sum();
    }

    private double outstandingPrincipal(List<MortgageStatement> monthStatements) {
        return monthStatements.stream()
            .max(Comparator
                .comparing(MortgageStatement::getStatementDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(MortgageStatement::getId))
            .map(MortgageStatement::getOutstandingPrincipalBalance)
            .orElse(0.0);
    }
}
