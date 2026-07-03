package com.silveira.accounting.application.investment;

import com.silveira.accounting.application.importing.DocumentImportService;
import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import com.silveira.accounting.parsers.investment.InvestmentImportData;
import com.silveira.accounting.parsers.investment.SchwabInvestmentStatementParser;
import com.silveira.accounting.parsers.investment.OpenAiInvestmentAiImportGateway;
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
    private final DocumentImportService<InvestmentImportData> imports;

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
        this.imports = new DocumentImportService<>(schwabParser, new OpenAiInvestmentAiImportGateway());
    }

    public InvestmentStatement importPdf(Path pdf, String accountAlias) {
        return saveImport(imports.importPdf(pdf), accountAlias);
    }

    public InvestmentStatement importPdfWithAi(Path pdf, String accountAlias) {
        return saveImport(imports.importPdfWithAi(pdf), accountAlias);
    }

    private InvestmentStatement saveImport(InvestmentImportData data, String accountAlias) {
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
        if (data.statement().getCostBasisTotal() == 0 && data.positions() != null && !data.positions().isEmpty()) {
            data.statement().setCostBasisTotal(data.positions().stream().mapToDouble(InvestmentPosition::getCostBasis).sum());
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
    public void updateStatement(InvestmentStatement statement) { statements.update(statement); }
    public List<InvestmentStatement> statements(String alias) { return statements.findByAccount(alias); }
    public List<InvestmentAllocation> allocations(long statementId) { return allocations.findByStatement(statementId); }
    public List<InvestmentPosition> positions(long statementId) { return positions.findByStatement(statementId); }
    public List<InvestmentTransaction> transactions(long statementId) { return transactions.findByStatement(statementId); }
    public void savePositions(InvestmentStatement statement, List<InvestmentPosition> values) {
        positions.replace(statement.getId(), values);
        double costBasisTotal = values.stream().mapToDouble(InvestmentPosition::getCostBasis).sum();
        double unrealizedGainLoss = values.stream().mapToDouble(InvestmentPosition::getUnrealizedGainLoss).sum();
        statement.setCostBasisTotal(costBasisTotal);
        statement.setUnrealizedGainLoss(unrealizedGainLoss);
        statements.updatePositionTotals(statement.getId(), costBasisTotal, unrealizedGainLoss);
    }
    public void saveTransactions(InvestmentStatement statement, List<InvestmentTransaction> values) {
        transactions.replace(statement.getId(), values);
    }
    public void deleteStatement(long statementId) { statements.delete(statementId); }
}
