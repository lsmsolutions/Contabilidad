package com.silveira.accounting.application.importing;

import java.nio.file.Path;

public class DocumentImportService<T> {
    private final DocumentImportGateway<T> knownParser;
    private final AiDocumentImportGateway<T> aiFallback;

    public DocumentImportService(DocumentImportGateway<T> knownParser, AiDocumentImportGateway<T> aiFallback) {
        this.knownParser = knownParser;
        this.aiFallback = aiFallback;
    }

    public T importPdf(Path pdf) {
        return knownParser.importPdf(pdf);
    }

    public T importPdfWithAi(Path pdf) {
        try {
            return knownParser.importPdf(pdf);
        } catch (RuntimeException parserFailure) {
            return aiFallback.importPdf(pdf, parserFailure).orElseThrow(() -> parserFailure);
        }
    }
}
