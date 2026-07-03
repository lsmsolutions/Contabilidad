package com.silveira.accounting.application.card.service;

import com.silveira.accounting.application.importing.DocumentImportService;
import com.silveira.accounting.parsers.OpenAiCreditCardAiImportGateway;
import com.silveira.accounting.parsers.CreditCardStatementParser;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.services.OcrService;

import java.nio.file.Path;

public class CreditCardImportService {
    private final PdfTextExtractor textExtractor = new PdfTextExtractor();
    private final OcrService ocrService = new OcrService();
    private final CreditCardStatementParser parser = new CreditCardStatementParser();
    private final DocumentImportService<CreditCardStatementParser.ParsedCreditCardStatement> imports =
        new DocumentImportService<>(this::importPdfWithKnownReaders, new OpenAiCreditCardAiImportGateway());

    public CreditCardStatementParser.ParsedCreditCardStatement importPdf(Path pdf) {
        return imports.importPdf(pdf);
    }

    public CreditCardStatementParser.ParsedCreditCardStatement importPdfWithAi(Path pdf) {
        return imports.importPdfWithAi(pdf);
    }

    private CreditCardStatementParser.ParsedCreditCardStatement importPdfWithKnownReaders(Path pdf) {
        try {
            String text = textExtractor.extract(pdf);
            return parser.parse(text, pdf.toString(), "importado_auto");
        } catch (RuntimeException exception) {
            OcrService.OcrResult ocr = ocrService.extractText(pdf);
            return parser.parse(ocr.text(), pdf.toString(), "ocr_revisado");
        }
    }
}
