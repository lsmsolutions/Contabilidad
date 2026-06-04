package com.silveira.accounting.services;

import com.silveira.accounting.models.DashboardSummary;
import com.silveira.accounting.repositories.BankTransactionRepository;
import com.silveira.accounting.repositories.NylRecordRepository;

public class DashboardService {
    private final BankTransactionRepository bankRepository;
    private final NylRecordRepository nylRepository;

    public DashboardService(BankTransactionRepository bankRepository, NylRecordRepository nylRepository) {
        this.bankRepository = bankRepository;
        this.nylRepository = nylRepository;
    }

    public DashboardSummary summary(Integer year, Integer month) {
        String bankPeriod = periodWhere(year, month);
        Object[] params = periodParams(year, month);
        double bankIncome = bankRepository.sum(bankPeriod + " AND amount > 0", params);
        double bankExpenses = bankRepository.sum(bankPeriod + " AND amount < 0", params);
        double nylReceivedBank = bankRepository.sum(bankPeriod + " AND provider='New York Life'", params);
        double nylCredits = nylRepository.sum(periodWhere(year, month) + " AND record_type IN ('comision','credito')", params);
        double nylDeductions = nylRepository.sum(periodWhere(year, month) + " AND record_type='deduccion'", params);
        double withdrawals = nylRepository.sum(periodWhere(year, month) + " AND record_type='withdrawal'", params);
        double nylNet = nylCredits + nylDeductions + withdrawals;
        return new DashboardSummary(bankIncome, nylReceivedBank, nylCredits, nylDeductions, nylNet, bankExpenses, nylReceivedBank - nylNet);
    }

    private String periodWhere(Integer year, Integer month) {
        String where = "1=1";
        if (year != null) {
            where += " AND year=?";
        }
        if (month != null) {
            where += " AND month=?";
        }
        return where;
    }

    private Object[] periodParams(Integer year, Integer month) {
        if (year != null && month != null) {
            return new Object[]{year, month};
        }
        if (year != null) {
            return new Object[]{year};
        }
        if (month != null) {
            return new Object[]{month};
        }
        return new Object[]{};
    }
}
