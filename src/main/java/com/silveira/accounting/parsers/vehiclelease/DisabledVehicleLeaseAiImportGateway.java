package com.silveira.accounting.parsers.vehiclelease;

import com.silveira.accounting.application.importing.AiDocumentImportGateway;
import java.nio.file.Path;
import java.util.Optional;

public class DisabledVehicleLeaseAiImportGateway implements AiDocumentImportGateway<VehicleLeaseImportData> {
    @Override
    public Optional<VehicleLeaseImportData> importPdf(Path pdf, RuntimeException parserFailure) {
        return Optional.empty();
    }
}
