package com.silveira.accounting.application.card.service;

import com.silveira.accounting.application.card.dto.CardLedgerRow;
import com.silveira.accounting.application.card.dto.CardLedgerSnapshot;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.CreditCardStatement;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class CardLedgerApplicationService {
    private final CardAccountApplicationService accounts;
    private final CardStatementApplicationService statements;

    public CardLedgerApplicationService(CardAccountApplicationService accounts, CardStatementApplicationService statements) {
        this.accounts = accounts;
        this.statements = statements;
    }

    public CardLedgerSnapshot snapshot(int year, int month) {
        List<CardLedgerRow> rows = accounts.findAll().stream()
            .map(account -> rowFor(account, year, month))
            .sorted(Comparator.comparing(CardLedgerRow::account, String.CASE_INSENSITIVE_ORDER))
            .toList();
        return new CardLedgerSnapshot(year, month, availableYears(), rows);
    }

    private CardLedgerRow rowFor(CreditCardAccount account, int year, int month) {
        Optional<CreditCardStatement> monthStatement = statements.findByAccount(account.getAlias(), year, month).stream()
            .filter(statement -> statement.getStatementEndDate() != null)
            .max(Comparator.comparing(CreditCardStatement::getStatementEndDate).thenComparingLong(CreditCardStatement::getId));
        double accumulatedInterest = statements.findByAccount(account.getAlias(), year, null).stream()
            .filter(statement -> statement.getStatementEndDate() != null)
            .filter(statement -> statement.getStatementEndDate().getMonthValue() <= month)
            .mapToDouble(CreditCardStatement::getInterestCharged)
            .sum();
        return new CardLedgerRow(
            text(account.getAlias()),
            text(account.getBankName()),
            lastDigits(account.getAccountLastDigits()),
            monthStatement.map(CreditCardStatement::getCreditLimit).orElse(0.0),
            monthStatement.map(CreditCardStatement::getNewBalance).orElse(0.0),
            monthStatement.map(CreditCardStatement::getAvailableCredit).orElse(0.0),
            monthStatement.map(CreditCardStatement::getInterestCharged).orElse(0.0),
            accumulatedInterest
        );
    }

    public List<Integer> availableYears() {
        TreeSet<Integer> years = new TreeSet<>();
        for (CreditCardAccount account : accounts.findAll()) {
            statements.findByAccount(account.getAlias()).stream()
                .map(CreditCardStatement::getStatementEndDate)
                .filter(date -> date != null)
                .map(LocalDate::getYear)
                .forEach(years::add);
        }
        years.add(LocalDate.now().getYear());
        return years.reversed().stream().toList();
    }

    private String lastDigits(String value) {
        String digits = text(value).replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return digits;
        }
        return digits.substring(digits.length() - 4);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
