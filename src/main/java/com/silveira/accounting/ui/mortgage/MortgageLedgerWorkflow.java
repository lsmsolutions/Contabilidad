package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import java.time.LocalDate;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.Parent;

public class MortgageLedgerWorkflow {
    private final MortgageApplicationService mortgage;
    private final Config config;

    public MortgageLedgerWorkflow(MortgageApplicationService mortgage, Config config) {
        this.mortgage = mortgage;
        this.config = config;
    }

    public void showLedger() {
        String selectedLoan = mortgage.statements().findLoanAliases().stream().findFirst().orElse("");
        showLedger(selectedLoan, LocalDate.now().getYear());
    }

    private void showLedger(String loan, int year) {
        config.setPage().accept(new MortgageLedgerView().build(
            mortgage.ledger().snapshot(loan, year),
            selectedLoan -> showLedger(selectedLoan, year),
            selectedYear -> showLedger(loan, selectedYear),
            config.backButton().apply("Volver a Hipotecas", config.showMortgages())
        ));
    }

    public record Config(
        Consumer<Parent> setPage,
        BiFunction<String, Runnable, Node> backButton,
        Runnable showMortgages
    ) {
    }
}
