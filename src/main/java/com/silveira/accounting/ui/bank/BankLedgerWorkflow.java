package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;

public class BankLedgerWorkflow {
    private final BankApplicationService bank;
    private final Config config;

    public BankLedgerWorkflow(BankApplicationService bank, Config config) {
        this.bank = bank;
        this.config = config;
    }

    public void showLedger() {
        int year = config.selectedYear().get() == null ? LocalDate.now().getYear() : config.selectedYear().get();
        int month = config.selectedMonth().get() == null ? LocalDate.now().getMonthValue() : config.selectedMonth().get();
        showLedger(year, month);
    }

    private void showLedger(int year, int month) {
        config.selectedPeriodChanged().accept(year, month);
        config.setPage().accept(new BankLedgerView().build(
            bank.ledger().snapshot(year, month),
            selectedYear -> showLedger(selectedYear, month),
            selectedMonth -> showLedger(year, selectedMonth),
            config.backButton().apply("Volver a Banco", config.showBank())
        ));
    }

    public record Config(
        Consumer<Parent> setPage,
        BiFunction<String, Runnable, Node> backButton,
        Runnable showBank,
        Supplier<Integer> selectedYear,
        Supplier<Integer> selectedMonth,
        BiConsumer<Integer, Integer> selectedPeriodChanged
    ) {
    }
}
