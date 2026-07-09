package com.silveira.accounting.application.mortgage;

import com.silveira.accounting.application.mortgage.service.HouseExpenseApplicationService;
import com.silveira.accounting.application.mortgage.service.MortgageAlertApplicationService;
import com.silveira.accounting.application.mortgage.service.MortgageFieldReviewApplicationService;
import com.silveira.accounting.application.mortgage.service.MortgageLedgerApplicationService;
import com.silveira.accounting.application.mortgage.service.MortgageStatementApplicationService;
import com.silveira.accounting.application.mortgage.service.MortgageTransactionApplicationService;
import com.silveira.accounting.repositories.mortgage.HouseExpenseRepository;
import com.silveira.accounting.repositories.mortgage.MortgageAlertRepository;
import com.silveira.accounting.repositories.mortgage.MortgageStatementFieldReviewRepository;
import com.silveira.accounting.repositories.mortgage.MortgageStatementRepository;
import com.silveira.accounting.repositories.mortgage.MortgageTransactionRepository;

public class MortgageApplicationService {
    private final MortgageStatementApplicationService statements;
    private final MortgageTransactionApplicationService transactions;
    private final MortgageAlertApplicationService alerts;
    private final MortgageFieldReviewApplicationService fieldReviews;
    private final HouseExpenseApplicationService houseExpenses;
    private final MortgageLedgerApplicationService ledger;

    public MortgageApplicationService(
        MortgageStatementRepository statements,
        MortgageTransactionRepository transactions,
        MortgageAlertRepository alerts,
        MortgageStatementFieldReviewRepository fieldReviews,
        HouseExpenseRepository houseExpenses
    ) {
        this.statements = new MortgageStatementApplicationService(statements);
        this.transactions = new MortgageTransactionApplicationService(transactions);
        this.alerts = new MortgageAlertApplicationService(alerts);
        this.fieldReviews = new MortgageFieldReviewApplicationService(fieldReviews);
        this.houseExpenses = new HouseExpenseApplicationService(houseExpenses);
        this.ledger = new MortgageLedgerApplicationService(this.statements);
    }

    public MortgageStatementApplicationService statements() {
        return statements;
    }

    public MortgageTransactionApplicationService transactions() {
        return transactions;
    }

    public MortgageAlertApplicationService alerts() {
        return alerts;
    }

    public MortgageFieldReviewApplicationService fieldReviews() {
        return fieldReviews;
    }

    public HouseExpenseApplicationService houseExpenses() {
        return houseExpenses;
    }

    public MortgageLedgerApplicationService ledger() {
        return ledger;
    }

    public void renameLoan(String oldAlias, String newAlias) {
        statements.renameLoan(oldAlias, newAlias);
        houseExpenses.renameLoan(oldAlias, newAlias);
    }

    public void deleteLoan(String alias) {
        houseExpenses.deleteByLoan(alias);
        statements.deleteLoan(alias);
    }
}
