package com.silveira.accounting.application.dashboard;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.bank.BankAccount;
import java.util.List;

public interface DashboardGateway {
    List<CreditCardAccount> cardAccounts();
    List<CreditCardStatement> cardStatements(String alias);
    List<BankAccount> bankAccounts();
    List<BankPeriodSummary> bankPeriodSummaries(String alias);
    List<NylRecord> nylRecords();
    List<MortgageStatement> mortgageStatements(String alias);
}
