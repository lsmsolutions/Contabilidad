package com.silveira.accounting.application.investment;

import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import com.silveira.accounting.parsers.investment.InvestmentImportData;
import com.silveira.accounting.parsers.investment.SchwabInvestmentStatementParser;
import com.silveira.accounting.repositories.investment.InvestmentAccountRepository;
import com.silveira.accounting.repositories.investment.InvestmentAllocationRepository;
import com.silveira.accounting.repositories.investment.InvestmentPositionRepository;
import com.silveira.accounting.repositories.investment.InvestmentStatementRepository;
import com.silveira.accounting.repositories.investment.InvestmentTransactionRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class InvestmentApplicationService {
    private final InvestmentAccountRepository accounts;
    private final InvestmentStatementRepository statements;
    private final InvestmentAllocationRepository allocations;
    private final InvestmentPositionRepository positions;
    private final InvestmentTransactionRepository transactions;
    private final SchwabInvestmentStatementParser schwabParser;

    public InvestmentApplicationService(
        InvestmentAccountRepository accounts,
        InvestmentStatementRepository statements,
        InvestmentAllocationRepository allocations,
        InvestmentPositionRepository positions,
        InvestmentTransactionRepository transactions,
        SchwabInvestmentStatementParser schwabParser
    ) {
        this.accounts = accounts;
        this.statements = statements;
        this.allocations = allocations;
        this.positions = positions;
        this.transactions = transactions;
        this.schwabParser = schwabParser;
    }

    public InvestmentStatement importPdf(Path pdf, String accountAlias) {
        InvestmentImportData data = schwabParser.parse(pdf);
        InvestmentAccount account = data.account();
        if (accountAlias != null && !accountAlias.isBlank()) {
            account.setAlias(accountAlias);
            data.statement().setAccountAlias(accountAlias);
            accounts.findByAlias(accountAlias).ifPresent(existing -> {
                account.setProviderName(existing.getProviderName());
                account.setAccountType(existing.getAccountType());
                account.setAccountNumber(existing.getAccountNumber());
                account.setNotes(existing.getNotes());
            });
        }
        accounts.save(account);
        long statementId = statements.save(data.statement());
        data.statement().setId(statementId);
        allocations.replace(statementId, data.allocations());
        positions.replace(statementId, data.positions());
        transactions.replace(statementId, data.transactions());
        return data.statement();
    }

    public List<InvestmentAccount> accounts() { return accounts.findAll(); }
    public Optional<InvestmentAccount> account(String alias) { return accounts.findByAlias(alias); }
    public void saveAccount(InvestmentAccount account) { accounts.save(account); }
    public void updateAccount(String originalAlias, InvestmentAccount account) { accounts.update(originalAlias, account); }
    public void deleteAccount(String alias) { accounts.delete(alias); }
    public List<InvestmentStatement> statements(String alias) { return statements.findByAccount(alias); }
    public List<InvestmentAllocation> allocations(long statementId) { return allocations.findByStatement(statementId); }
    public List<InvestmentPosition> positions(long statementId) { return positions.findByStatement(statementId); }
    public List<InvestmentTransaction> transactions(long statementId) { return transactions.findByStatement(statementId); }
    public void deleteStatement(long statementId) { statements.delete(statementId); }
}
