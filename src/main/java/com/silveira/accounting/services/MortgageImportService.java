package com.silveira.accounting.services;

import com.silveira.accounting.parsers.MortgageStatementParser;
import com.silveira.accounting.parsers.PdfTextExtractor;

import java.nio.file.Path;

public class MortgageImportService {
    private final PdfTextExtractor textExtractor = new PdfTextExtractor();
    private final OcrService ocrService = new OcrService();
    private final MortgageStatementParser parser = new MortgageStatementParser();

    public MortgageStatementParser.ParsedMortgageStatement importPdf(Path pdf) {
        String text = textExtractor.extract(pdf);
        MortgageStatementParser.ParsedMortgageStatement direct = parser.parse(text, pdf.toString(), "importado_en_revision");
        if (hasUsefulMortgageData(direct)) {
            return direct;
        }
        OcrService.OcrResult ocr = ocrService.extractText(pdf);
        MortgageStatementParser.ParsedMortgageStatement parsed = parser.parse(ocr.text(), pdf.toString(), "ocr_en_revision");
        parsed.statement().setReviewNotes("OCR: revisar contra el PDF original");
        return parsed;
    }

    private boolean hasUsefulMortgageData(MortgageStatementParser.ParsedMortgageStatement parsed) {
        return parsed.statement().getPaymentDueDate() != null
            || parsed.statement().getStatementDate() != null
            || parsed.statement().getOutstandingPrincipalBalance() > 0
            || parsed.statement().getTotalDue() > 0
            || parsed.statement().getPaymentAmountDue() > 0;
    }
}
