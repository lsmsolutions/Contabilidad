package com.silveira.accounting.application.dashboard;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import java.util.List;

public interface DashboardGateway {
    List<CreditCardAccount> cardAccounts();
    List<CreditCardStatement> cardStatements(String alias);
    List<BankAccount> bankAccounts();
    List<BankPeriodSummary> bankPeriodSummaries(String alias);
    List<NylRecord> nylRecords();
    List<String> mortgageAliases();
    List<MortgageStatement> mortgageStatements(String alias);
    List<InvestmentAccount> investmentAccounts();
    List<InvestmentStatement> investmentStatements(String alias);
    List<VehicleLeaseAccount> vehicleLeaseAccounts();
    List<VehicleLeaseStatement> vehicleLeaseStatements(String alias);
}
