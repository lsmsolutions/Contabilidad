package com.silveira.accounting.application.internalmovement;

import com.silveira.accounting.models.InternalMovementRecord;
import java.util.List;

public interface InternalMovementGateway {
    List<InternalMovementRecord> findManual(Integer year, Integer month);
    long save(InternalMovementRecord movement);
    void delete(long id);
}
