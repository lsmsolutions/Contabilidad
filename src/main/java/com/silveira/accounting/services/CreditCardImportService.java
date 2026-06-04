package com.silveira.accounting.services;

import com.silveira.accounting.parsers.CreditCardStatementParser;
import com.silveira.accounting.parsers.PdfTextExtractor;

import java.nio.file.Path;

public class CreditCardImportService {
    private final PdfTextExtractor textExtractor = new PdfTextExtractor();
    private final OcrService ocrService = new OcrService();
    private final CreditCardStatementParser parser = new CreditCardStatementParser();

    public CreditCardStatementParser.ParsedCreditCardStatement importPdf(Path pdf) {
        try {
            String text = textExtractor.extract(pdf);
            return parser.parse(text, pdf.toString(), "importado_auto");
        } catch (RuntimeException exception) {
            OcrService.OcrResult ocr = ocrService.extractText(pdf);
            return parser.parse(ocr.text(), pdf.toString(), "ocr_revisado");
        }
    }
}
