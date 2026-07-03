package com.silveira.accounting.application.importing;

import java.nio.file.Path;
import java.util.Optional;

public interface AiDocumentImportGateway<T> {
    Optional<T> importPdf(Path pdf, RuntimeException parserFailure);
}
