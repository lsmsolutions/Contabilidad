package com.silveira.accounting.parsers.nyl;

import com.silveira.accounting.application.importing.DocumentImportService;
import com.silveira.accounting.application.nyl.NylImportGateway;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.parsers.NylPdfParser;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.services.ImportValidationService;
import com.silveira.accounting.services.OcrService;
import java.nio.file.Path;
import java.util.List;

public class NylImportAdapter implements NylImportGateway {
    private final NylPdfParser parser;
    private final PdfTextExtractor textExtractor;
    private final OcrService ocr;
    private final ImportValidationService validation;
    private final DocumentImportService<ParsedDocument> imports;

    public NylImportAdapter(
        NylPdfParser parser,
        PdfTextExtractor textExtractor,
        OcrService ocr,
        ImportValidationService validation
    ) {
        this.parser = parser;
        this.textExtractor = textExtractor;
        this.ocr = ocr;
        this.validation = validation;
        this.imports = new DocumentImportService<>(this::parseKnown, new OpenAiNylAiImportGateway());
    }

    @Override
    public ParsedDocument parse(Path pdf) {
        return imports.importPdf(pdf);
    }

    private ParsedDocument parseKnown(Path pdf) {
        return new ParsedDocument(parser.parse(pdf), textExtractor.extract(pdf));
    }

    @Override
    public ParsedDocument parseWithOcr(Path pdf) {
        OcrService.OcrResult result = ocr.extractText(pdf);
        List<NylRecord> records = parser.parseText(
            result.text(),
            pdf.getFileName().toString(),
            "ocr_revisado",
            true
        );
        return new ParsedDocument(records, result.text());
    }

    @Override
    public ParsedDocument parseWithAi(Path pdf) {
        return imports.importPdfWithAi(pdf);
    }

    @Override
    public double detectDeclaredTotal(String text) {
        return parser.detectDeclaredTotal(text);
    }

    @Override
    public List<String> validate(List<NylRecord> records, double declaredTotal) {
        return validation.validateNyl(records, declaredTotal);
    }
}
