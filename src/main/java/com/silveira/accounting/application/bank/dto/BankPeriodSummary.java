package com.silveira.accounting.application.bank.dto;

import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.SourceTotals;

import java.util.List;
import java.util.Objects;

public record BankPeriodSummary(
    BankStatementPeriod statementPeriod,
    List<BankTransaction> transactions,
    SourceTotals totals
) {
    public boolean samePeriodAs(BankStatementPeriod other) {
        return other != null
            && Objects.equals(statementPeriod.accountAlias(), other.accountAlias())
            && Objects.equals(statementPeriod.sourcePdf(), other.sourcePdf())
            && Objects.equals(statementPeriod.periodStart(), other.periodStart())
            && Objects.equals(statementPeriod.periodEnd(), other.periodEnd());
    }
}
