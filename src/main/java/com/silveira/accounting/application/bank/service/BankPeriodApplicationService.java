package com.silveira.accounting.application.bank.service;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.application.bank.usecase.BuildBankPeriodSummariesUseCase;
import com.silveira.accounting.application.bank.usecase.GetBankMonthlyClosingUseCase;
import com.silveira.accounting.application.bank.usecase.SaveManualBankPeriodUseCase;
import com.silveira.accounting.application.bank.usecase.SaveBankMonthlyClosingUseCase;
import com.silveira.accounting.application.bank.usecase.UpdateBankStatementPeriodUseCase;
import com.silveira.accounting.models.bank.BankMonthlyClosing;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.repositories.BankMonthlyClosingRepository;
import com.silveira.accounting.repositories.BankStatementPeriodRepository;
import com.silveira.accounting.repositories.BankTransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BankPeriodApplicationService {
    private final BankStatementPeriodRepository periods;
    private final BankMonthlyClosingRepository closings;
    private final BuildBankPeriodSummariesUseCase buildPeriodSummaries;
    private final GetBankMonthlyClosingUseCase getMonthlyClosing;
    private final SaveManualBankPeriodUseCase saveManualPeriod;
    private final SaveBankMonthlyClosingUseCase saveMonthlyClosing;
    private final UpdateBankStatementPeriodUseCase updatePeriod;

    public BankPeriodApplicationService(
        BankStatementPeriodRepository periods,
        BankMonthlyClosingRepository closings,
        BankTransactionRepository transactions
    ) {
        this.periods = periods;
        this.closings = closings;
        buildPeriodSummaries = new BuildBankPeriodSummariesUseCase(transactions, periods, closings);
        getMonthlyClosing = new GetBankMonthlyClosingUseCase(closings);
        saveManualPeriod = new SaveManualBankPeriodUseCase(periods);
        saveMonthlyClosing = new SaveBankMonthlyClosingUseCase(closings);
        updatePeriod = new UpdateBankStatementPeriodUseCase(periods);
    }

    public Optional<BankStatementPeriod> findPeriod(String accountAlias, String sourcePdf) {
        return periods.find(accountAlias, sourcePdf);
    }

    public Optional<BankStatementPeriod> findOverlappingPeriod(String accountAlias, LocalDate start, LocalDate end) {
        return periods.findOverlapping(accountAlias, start, end);
    }

    public List<BankPeriodSummary> summaries(String accountAlias) {
        return buildPeriodSummaries.execute(accountAlias);
    }

    public void updatePeriod(
        String accountAlias,
        String sourcePdf,
        LocalDate start,
        LocalDate end,
        double openingBalance,
        double statementEndingBalance
    ) {
        updatePeriod.execute(accountAlias, sourcePdf, start, end, openingBalance, statementEndingBalance);
    }

    public void saveManualPeriod(String accountAlias, LocalDate start, LocalDate end, double openingBalance, double statementEndingBalance) {
        saveManualPeriod.execute(accountAlias, start, end, openingBalance, statementEndingBalance);
    }

    public void deletePeriod(String accountAlias, String sourcePdf) {
        periods.delete(accountAlias, sourcePdf);
    }

    public Optional<BankMonthlyClosing> findClosing(String accountAlias, int year, int month) {
        return closings.find(accountAlias, year, month);
    }

    public BankMonthlyClosing currentClosing(String accountAlias, int year, int month) {
        return getMonthlyClosing.execute(accountAlias, year, month);
    }

    public void saveClosing(String accountAlias, int year, int month, double openingBalance, double statementEndingBalance) {
        saveMonthlyClosing.execute(accountAlias, year, month, openingBalance, statementEndingBalance);
    }
}
