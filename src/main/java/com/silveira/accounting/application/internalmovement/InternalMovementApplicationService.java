package com.silveira.accounting.application.internalmovement;

import com.silveira.accounting.models.InternalMovementRecord;
import java.time.LocalDate;
import java.util.List;

public class InternalMovementApplicationService {
    private final InternalMovementGateway repository;

    public InternalMovementApplicationService(InternalMovementGateway repository) {
        this.repository = repository;
    }

    public List<InternalMovementRecord> findManual(Integer year, Integer month) {
        return repository.findManual(year, month);
    }

    public InternalMovementRecord createManual() {
        return new InternalMovementRecord(
            0,
            "manual",
            System.nanoTime(),
            LocalDate.now(),
            "",
            "",
            0,
            "Movimiento manual",
            "pendiente",
            false,
            true
        );
    }

    public void saveVisible(List<InternalMovementRecord> movements) {
        for (InternalMovementRecord movement : movements) {
            movement.setManual(true);
            long id = repository.save(movement);
            movement.setId(id);
        }
    }

    public void delete(InternalMovementRecord movement) {
        if (movement.getId() > 0) {
            repository.delete(movement.getId());
        }
    }
}
