package com.silveira.accounting.ui.nyl;

import com.silveira.accounting.application.nyl.NylApplicationService;
import com.silveira.accounting.controllers.nyl.NylController;
import com.silveira.accounting.repositories.NylMonthlyResultRepository;
import com.silveira.accounting.repositories.NylRecordRepository;
import com.silveira.accounting.repositories.nyl.NylDataRepository;
import com.silveira.accounting.parsers.NylPdfParser;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.parsers.nyl.NylImportAdapter;
import com.silveira.accounting.services.ImportValidationService;
import com.silveira.accounting.services.OcrService;

public class NylModule {
    private final NylController controller;

    public NylModule(
        NylRecordRepository records,
        NylMonthlyResultRepository monthlyResults,
        OcrService ocrService
    ) {
        controller = new NylController(new NylApplicationService(
            new NylDataRepository(records, monthlyResults),
            new NylImportAdapter(
                new NylPdfParser(),
                new PdfTextExtractor(),
                ocrService,
                new ImportValidationService()
            )
        ));
    }

    public NylController controller() {
        return controller;
    }
}
