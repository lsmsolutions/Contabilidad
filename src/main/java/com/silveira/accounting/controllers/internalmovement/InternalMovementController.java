package com.silveira.accounting.controllers.internalmovement;

import com.silveira.accounting.application.internalmovement.InternalMovementApplicationService;
import com.silveira.accounting.models.InternalMovementRecord;
import java.util.List;

public class InternalMovementController {
    private final InternalMovementApplicationService application;

    public InternalMovementController(InternalMovementApplicationService application) {
        this.application = application;
    }

    public List<InternalMovementRecord> findManual(Integer year, Integer month) {
        return application.findManual(year, month);
    }

    public InternalMovementRecord createManual() {
        return application.createManual();
    }

    public void saveVisible(List<InternalMovementRecord> movements) {
        application.saveVisible(movements);
    }

    public void delete(InternalMovementRecord movement) {
        application.delete(movement);
    }
}
