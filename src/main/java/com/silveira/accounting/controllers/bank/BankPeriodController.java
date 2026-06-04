package com.silveira.accounting.controllers.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.models.bank.BankMonthlyClosing;
import java.time.LocalDate;

public class BankPeriodController {
    private final BankApplicationService bank;

    public BankPeriodController(BankApplicationService bank) {
        this.bank = bank;
    }

    public void updatePeriod(
        String accountAlias,
        String sourcePdf,
        LocalDate start,
        LocalDate end,
        double openingBalance,
        double statementEndingBalance
    ) {
        bank.periods().updatePeriod(accountAlias, sourcePdf, start, end, openingBalance, statementEndingBalance);
    }

    public BankMonthlyClosing currentClosing(String accountAlias, int year, int month) {
        return bank.periods().currentClosing(accountAlias, year, month);
    }

    public void saveClosing(String accountAlias, int year, int month, double openingBalance, double statementEndingBalance) {
        bank.periods().saveClosing(accountAlias, year, month, openingBalance, statementEndingBalance);
    }
}
