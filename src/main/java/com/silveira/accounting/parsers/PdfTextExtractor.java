package com.silveira.accounting.parsers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;

public class PdfTextExtractor {
    public String extract(Path pdf) {
        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.strip().isEmpty()) {
                throw new IllegalArgumentException("El PDF no contiene texto extraible. Puede ser un escaneo; conviertelo con OCR o usa entrada manual.");
            }
            return text;
        } catch (IOException exception) {
            throw new IllegalArgumentException("No se pudo leer el PDF: " + pdf.getFileName(), exception);
        }
    }
}
