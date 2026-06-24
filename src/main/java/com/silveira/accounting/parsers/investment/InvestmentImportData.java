package com.silveira.accounting.parsers.investment;

import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import java.util.List;

public record InvestmentImportData(
    InvestmentAccount account,
    InvestmentStatement statement,
    List<InvestmentAllocation> allocations,
    List<InvestmentPosition> positions,
    List<InvestmentTransaction> transactions
) {
}
