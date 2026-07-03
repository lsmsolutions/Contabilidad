package com.silveira.accounting.controllers.investment;

import com.silveira.accounting.application.investment.InvestmentApplicationService;
import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class InvestmentController {
    private final InvestmentApplicationService application;

    public InvestmentController(InvestmentApplicationService application) {
        this.application = application;
    }

    public InvestmentStatement importPdf(Path pdf, String alias) { return application.importPdf(pdf, alias); }
    public InvestmentStatement importPdfWithAi(Path pdf, String alias) { return application.importPdfWithAi(pdf, alias); }
    public List<InvestmentAccount> accounts() { return application.accounts(); }
    public Optional<InvestmentAccount> account(String alias) { return application.account(alias); }
    public void saveAccount(InvestmentAccount account) { application.saveAccount(account); }
    public void updateAccount(String originalAlias, InvestmentAccount account) { application.updateAccount(originalAlias, account); }
    public void deleteAccount(String alias) { application.deleteAccount(alias); }
    public void updateStatement(InvestmentStatement statement) { application.updateStatement(statement); }
    public List<InvestmentStatement> statements(String alias) { return application.statements(alias); }
    public List<InvestmentAllocation> allocations(long statementId) { return application.allocations(statementId); }
    public List<InvestmentPosition> positions(long statementId) { return application.positions(statementId); }
    public List<InvestmentTransaction> transactions(long statementId) { return application.transactions(statementId); }
    public void savePositions(InvestmentStatement statement, List<InvestmentPosition> values) { application.savePositions(statement, values); }
    public void saveTransactions(InvestmentStatement statement, List<InvestmentTransaction> values) { application.saveTransactions(statement, values); }
    public void deleteStatement(long statementId) { application.deleteStatement(statementId); }
}
