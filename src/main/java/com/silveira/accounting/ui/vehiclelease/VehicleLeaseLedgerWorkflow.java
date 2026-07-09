package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.controllers.vehiclelease.VehicleLeaseController;
import java.time.LocalDate;
import java.util.function.Consumer;
import javafx.scene.Parent;

public class VehicleLeaseLedgerWorkflow {
    private final VehicleLeaseController controller;
    private final Config config;

    public VehicleLeaseLedgerWorkflow(VehicleLeaseController controller, Config config) {
        this.controller = controller;
        this.config = config;
    }

    public void showLedger() {
        String account = controller.accounts().stream().findFirst().map(value -> value.getAlias()).orElse("");
        showLedger(account, LocalDate.now().getYear());
    }

    private void showLedger(String account, int year) {
        config.setPage().accept(new VehicleLeaseLedgerView().build(
            controller.ledger(account, year),
            selectedAccount -> showLedger(selectedAccount, year),
            selectedYear -> showLedger(account, selectedYear),
            config.showVehicleLeases()
        ));
    }

    public record Config(
        Consumer<Parent> setPage,
        Runnable showVehicleLeases
    ) {
    }
}
