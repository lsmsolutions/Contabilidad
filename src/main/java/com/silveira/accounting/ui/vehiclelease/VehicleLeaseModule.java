package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.application.vehiclelease.VehicleLeaseApplicationService;
import com.silveira.accounting.controllers.vehiclelease.VehicleLeaseController;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.parsers.vehiclelease.VolvoVehicleLeaseParser;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseAccountRepository;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseFieldReviewRepository;
import com.silveira.accounting.repositories.vehiclelease.VehicleLeaseStatementRepository;

public class VehicleLeaseModule {
    private final VehicleLeaseController controller;

    public VehicleLeaseModule(DatabaseManager databaseManager) {
        controller = new VehicleLeaseController(new VehicleLeaseApplicationService(
            new VehicleLeaseAccountRepository(databaseManager),
            new VehicleLeaseStatementRepository(databaseManager),
            new VehicleLeaseFieldReviewRepository(databaseManager),
            new VolvoVehicleLeaseParser()
        ));
    }

    public VehicleLeaseController controller() {
        return controller;
    }
}
