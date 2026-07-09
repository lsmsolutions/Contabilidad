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
        RuntimeException aiRequest = new IllegalArgumentException("Lectura solicitada con IA.");
        return aiFallback.importPdf(pdf, aiRequest).orElseThrow(() -> aiRequest);
    }
}
