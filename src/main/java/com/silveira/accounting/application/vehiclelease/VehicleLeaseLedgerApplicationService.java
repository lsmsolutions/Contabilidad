package com.silveira.accounting.application.vehiclelease;

import com.silveira.accounting.application.vehiclelease.dto.VehicleLeaseLedgerRow;
import com.silveira.accounting.application.vehiclelease.dto.VehicleLeaseLedgerSnapshot;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseAccountRepository;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseStatementRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class VehicleLeaseLedgerApplicationService {
    private final VehicleLeaseAccountRepository accounts;
    private final VehicleLeaseStatementRepository statements;

    public VehicleLeaseLedgerApplicationService(VehicleLeaseAccountRepository accounts, VehicleLeaseStatementRepository statements) {
        this.accounts = accounts;
        this.statements = statements;
    }

    public VehicleLeaseLedgerSnapshot snapshot(String requestedAccount, int year) {
        List<VehicleLeaseAccount> accountList = accounts.findAll();
        String selectedAccount = selectedAccount(requestedAccount, accountList);
        return new VehicleLeaseLedgerSnapshot(
            selectedAccount,
            year,
            accountList,
            availableYears(selectedAccount),
            rows(selectedAccount, year)
        );
    }

    private String selectedAccount(String requestedAccount, List<VehicleLeaseAccount> accountList) {
        if (requestedAccount != null && !requestedAccount.isBlank()
            && accountList.stream().anyMatch(account -> requestedAccount.equals(account.getAlias()))) {
            return requestedAccount;
        }
        return accountList.isEmpty() ? "" : accountList.get(0).getAlias();
    }

    private List<Integer> availableYears(String accountAlias) {
        TreeSet<Integer> years = new TreeSet<>();
        if (accountAlias != null && !accountAlias.isBlank()) {
            statements.findByAccount(accountAlias).stream()
                .map(VehicleLeaseStatement::getStatementDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .forEach(years::add);
        }
        years.add(LocalDate.now().getYear());
        return years.reversed().stream().toList();
    }

    private List<VehicleLeaseLedgerRow> rows(String accountAlias, int year) {
        List<VehicleLeaseLedgerRow> rows = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Optional<VehicleLeaseStatement> statement = latestStatement(accountAlias, year, month);
            rows.add(new VehicleLeaseLedgerRow(
                month,
                statement.map(VehicleLeaseStatement::getStatementDate).orElse(null),
                statement.map(VehicleLeaseStatement::getDueDate).orElse(null),
                statement.map(VehicleLeaseStatement::getTotalAmountDue).orElse(0.0),
                statement.map(VehicleLeaseStatement::getPaymentsMade).orElse(0),
                statement.map(VehicleLeaseStatement::getPaymentsRemaining).orElse(0),
                statement.isPresent()
            ));
        }
        return rows;
    }

    private Optional<VehicleLeaseStatement> latestStatement(String accountAlias, int year, int month) {
        if (accountAlias == null || accountAlias.isBlank()) {
            return Optional.empty();
        }
        return statements.findByAccount(accountAlias).stream()
            .filter(statement -> statement.getStatementDate() != null)
            .filter(statement -> statement.getStatementDate().getYear() == year)
            .filter(statement -> statement.getStatementDate().getMonthValue() == month)
            .max(Comparator.comparing(VehicleLeaseStatement::getStatementDate).thenComparingLong(VehicleLeaseStatement::getId));
    }
}
