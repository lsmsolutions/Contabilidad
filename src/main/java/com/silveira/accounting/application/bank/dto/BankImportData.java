package com.silveira.accounting.application.bank.dto;

import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import java.util.List;
import java.util.Optional;

public record BankImportData(List<BankTransaction> transactions, Optional<BankStatementPeriod> period) {
    public static BankImportData transactionsOnly(List<BankTransaction> transactions) {
        return new BankImportData(transactions, Optional.empty());
    }
}
