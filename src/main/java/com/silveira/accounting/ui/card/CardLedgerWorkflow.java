package com.silveira.accounting.ui.card;

import com.silveira.accounting.application.card.service.CardLedgerApplicationService;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;

public class CardLedgerWorkflow {
    private final CardLedgerApplicationService ledger;
    private final Config config;

    public CardLedgerWorkflow(CardLedgerApplicationService ledger, Config config) {
        this.ledger = ledger;
        this.config = config;
    }

    public void showLedger() {
        int year = config.selectedYear().get() == null ? LocalDate.now().getYear() : config.selectedYear().get();
        int month = config.selectedMonth().get() == null ? LocalDate.now().getMonthValue() : config.selectedMonth().get();
        showLedger(year, month);
    }

    private void showLedger(int year, int month) {
        config.selectedPeriodChanged().accept(year, month);
        config.setPage().accept(new CardLedgerView().build(
            ledger.snapshot(year, month),
            selectedYear -> showLedger(selectedYear, month),
            selectedMonth -> showLedger(year, selectedMonth),
            config.backButton().apply("Volver a Tarjetas", config.showCards())
        ));
    }

    public record Config(
        Consumer<Parent> setPage,
        BiFunction<String, Runnable, Node> backButton,
        Runnable showCards,
        Supplier<Integer> selectedYear,
        Supplier<Integer> selectedMonth,
        BiConsumer<Integer, Integer> selectedPeriodChanged
    ) {
    }
}
