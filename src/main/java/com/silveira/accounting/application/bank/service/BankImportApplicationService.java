package com.silveira.accounting.application.bank.service;

import com.silveira.accounting.application.bank.dto.BankImportReview;
import com.silveira.accounting.application.bank.dto.BankImportSaveResult;
import com.silveira.accounting.application.bank.usecase.CreateManualBankTransactionUseCase;
import com.silveira.accounting.application.bank.usecase.DeleteBankPeriodUseCase;
import com.silveira.accounting.application.bank.usecase.PrepareBankImportReviewUseCase;
import com.silveira.accounting.application.bank.usecase.SaveBankImportRowsUseCase;
import com.silveira.accounting.application.bank.usecase.SaveBankPendingReviewRowsUseCase;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.repositories.bank.BankStatementPeriodRepository;

import java.time.LocalDate;
import java.util.List;

public class BankImportApplicationService {
    private final PrepareBankImportReviewUseCase prepareReview;
    private final SaveBankImportRowsUseCase saveRows;
    private final CreateManualBankTransactionUseCase createManualTransaction;
    private final SaveBankPendingReviewRowsUseCase savePendingReviewRows;
    private final DeleteBankPeriodUseCase deletePeriod;

    public BankImportApplicationService(BankTransactionApplicationService transactions, BankAccountApplicationService accounts, BankStatementPeriodRepository periods) {
        prepareReview = new PrepareBankImportReviewUseCase(transactions, accounts);
        saveRows = new SaveBankImportRowsUseCase(transactions);
        createManualTransaction = new CreateManualBankTransactionUseCase(transactions);
        savePendingReviewRows = new SaveBankPendingReviewRowsUseCase(transactions);
        deletePeriod = new DeleteBankPeriodUseCase(transactions, periods);
    }

    public BankImportReview prepareReview(List<BankTransaction> parsed, String selectedAccountAlias) {
        return prepareReview.execute(parsed, selectedAccountAlias);
    }

    public BankImportSaveResult saveRows(List<BankTransaction> rows) {
        return saveRows.execute(rows);
    }

    public BankTransaction createManualTransaction(String selectedAccountAlias, LocalDate fallbackDate, BankStatementPeriod period) {
        return createManualTransaction.execute(selectedAccountAlias, fallbackDate, period);
    }

    public void savePendingReviewRows(List<BankTransaction> rows) {
        savePendingReviewRows.execute(rows);
    }

    public void deletePeriod(BankStatementPeriod period, List<BankTransaction> periodTransactions) {
        deletePeriod.execute(period, periodTransactions);
    }
}
