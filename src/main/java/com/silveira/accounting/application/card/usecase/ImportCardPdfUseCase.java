package com.silveira.accounting.application.card.usecase;

import com.silveira.accounting.application.card.service.CreditCardImportService;
import com.silveira.accounting.parsers.CreditCardStatementParser;
import java.nio.file.Path;

public class ImportCardPdfUseCase {
    private final CreditCardImportService importService;

    public ImportCardPdfUseCase(CreditCardImportService importService) {
        this.importService = importService;
    }

    public CreditCardStatementParser.ParsedCreditCardStatement execute(Path pdf) {
        return importService.importPdf(pdf);
    }
}
