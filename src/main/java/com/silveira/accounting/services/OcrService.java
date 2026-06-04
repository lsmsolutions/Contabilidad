package com.silveira.accounting.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OcrService {
    public OcrResult extractText(Path pdf) {
        Path tesseract = findTesseract();
        if (tesseract == null) {
            throw new IllegalStateException("""
                No se encontro Tesseract OCR instalado.

                Instala Tesseract para Windows y asegúrate de que tesseract.exe esté en el PATH,
                o en C:\\Program Files\\Tesseract-OCR\\tesseract.exe.
                """);
        }

        try {
            Path tempDir = Files.createTempDirectory("silveira-ocr-");
            StringBuilder text = new StringBuilder();
            try (PDDocument document = PDDocument.load(pdf.toFile())) {
                PDFRenderer renderer = new PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);
                    text.append("\n--- PAGE ").append(page + 1).append(" ---\n");
                    text.append(bestOcrForPage(tesseract, tempDir, image, page + 1));
                }
            }
            return new OcrResult(text.toString(), tesseract.toString());
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("No se pudo ejecutar OCR sobre el PDF", exception);
        }
    }

    private String bestOcrForPage(Path tesseract, Path tempDir, BufferedImage image, int page) throws IOException, InterruptedException {
        OcrCandidate best = new OcrCandidate("", Integer.MIN_VALUE);
        int[] rotations = {0, 90, 180, 270};
        String[] modes = {"6", "11", "12"};
        for (int rotation : rotations) {
            BufferedImage rotated = rotate(image, rotation);
            Path imagePath = tempDir.resolve("page-" + page + "-r" + rotation + ".png");
            ImageIO.write(rotated, "png", imagePath.toFile());
            for (String mode : modes) {
                String text = runTesseract(tesseract, imagePath, mode);
                int score = score(text);
                if (score > best.score()) {
                    best = new OcrCandidate(text, score);
                }
            }
        }
        return best.text();
    }

    private String runTesseract(Path tesseract, Path image, String pageSegmentationMode) throws IOException, InterruptedException {
        List<String> command = List.of(tesseract.toString(), image.toString(), "stdout", "-l", "eng", "--psm", pageSegmentationMode);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Tesseract fallo leyendo una pagina: " + String.join("\n", lines));
        }
        return String.join("\n", lines);
    }

    private int score(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        int score = 0;
        score += count(lower, "new york") * 50;
        score += count(lower, "life") * 30;
        score += count(lower, "commission") * 25;
        score += count(lower, "ledger") * 25;
        score += count(lower, "deduction") * 25;
        score += count(lower, "payment due") * 40;
        score += count(lower, "discover") * 45;
        score += count(lower, "disc®ver") * 45;
        score += count(lower, "account summary") * 45;
        score += count(lower, "payment information") * 45;
        score += count(lower, "credit line") * 35;
        score += count(lower, "cashback") * 35;
        score += count(lower, "transactions") * 35;
        score += count(lower, "payments and credits") * 30;
        score += count(lower, "principal") * 35;
        score += count(lower, "escrow") * 35;
        score += count(lower, "interest") * 25;
        score += count(lower, "total due") * 35;
        score += count(lower, "mortgage") * 35;
        score += count(lower, "uwm") * 35;
        score += count(lower, "jan") * 10;
        score += count(lower, "feb") * 10;
        score += count(lower, "mar") * 10;
        score += count(lower, "apr") * 10;
        score += lower.replaceAll("[^0-9]", "").length();
        score += lower.replaceAll("[^a-z]", "").length() / 4;
        score -= count(lower, "oooo") * 10;
        score -= count(lower, "cccc") * 10;
        return score;
    }

    private int count(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    private BufferedImage rotate(BufferedImage image, int degrees) {
        if (degrees == 0) {
            return image;
        }
        double radians = Math.toRadians(degrees);
        int width = image.getWidth();
        int height = image.getHeight();
        int newWidth = degrees == 180 ? width : height;
        int newHeight = degrees == 180 ? height : width;
        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
        java.awt.Graphics2D graphics = rotated.createGraphics();
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(0, 0, newWidth, newHeight);
        graphics.translate(newWidth / 2.0, newHeight / 2.0);
        graphics.rotate(radians);
        graphics.translate(-width / 2.0, -height / 2.0);
        graphics.drawRenderedImage(image, null);
        graphics.dispose();
        return rotated;
    }

    private Path findTesseract() {
        List<Path> candidates = List.of(
            Path.of("C:\\Program Files\\Tesseract-OCR\\tesseract.exe"),
            Path.of("C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        for (String entry : path.split(";")) {
            Path candidate = Path.of(entry, "tesseract.exe");
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public record OcrResult(String text, String enginePath) {
    }

    private record OcrCandidate(String text, int score) {
    }
}
