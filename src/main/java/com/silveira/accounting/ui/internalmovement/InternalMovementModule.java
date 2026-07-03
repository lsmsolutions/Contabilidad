package com.silveira.accounting.ui.internalmovement;

import com.silveira.accounting.application.internalmovement.InternalMovementApplicationService;
import com.silveira.accounting.controllers.internalmovement.InternalMovementController;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.repositories.InternalMovementRepository;

public class InternalMovementModule {
    private final InternalMovementController controller;

    public InternalMovementModule(DatabaseManager databaseManager) {
        controller = new InternalMovementController(
            new InternalMovementApplicationService(
                new InternalMovementRepository(databaseManager)
            )
        );
    }

    public InternalMovementController controller() {
        return controller;
    }
}
