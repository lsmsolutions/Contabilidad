package com.silveira.accounting.ui.dashboard;

import com.silveira.accounting.application.dashboard.DashboardSnapshot.LatestStatement;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/** Printable statement request with repeated headings and wrapped table cells. */
final class StatementsPdfExporter {
    private static final float[] WIDTHS = {90, 140, 100, 382};
    private static final float LEFT = 40, TOP = 555, BOTTOM = 40, FONT_SIZE = 9, LINE = 13;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color NAVY = new Color(11, 39, 66), BLUE = new Color(25, 105, 145), LIGHT = new Color(237, 246, 251);

    private StatementsPdfExporter() { }

    static void export(Path path, List<LatestStatement> statements) throws IOException {
        try (PDDocument document = new PDDocument()) {
            Path fonts = Path.of(System.getenv().getOrDefault("WINDIR", "C:\\Windows"), "Fonts");
            PDFont regular = PDType0Font.load(document, fonts.resolve("arial.ttf").toFile());
            PDFont bold = PDType0Font.load(document, fonts.resolve("arialbd.ttf").toFile());
            Writer writer = new Writer(document, regular, bold);
            try {
                writer.newPage();
                for (LatestStatement statement : statements) {
                    String[] values = {statement.source(), statement.account(), statement.date() == null ? "None" : DATE.format(statement.date()), statement.missingPeriods()};
                    List<List<String>> columns = new ArrayList<>();
                    int lines = 1;
                    for (int i = 0; i < values.length; i++) {
                        List<String> wrapped = wrap(values[i], regular, WIDTHS[i] - 12);
                        columns.add(wrapped);
                        lines = Math.max(lines, wrapped.size());
                    }
                    int offset = 0;
                    while (offset < lines) {
                        int available = (int) ((writer.y - BOTTOM - 10) / LINE);
                        if (available < 1) { writer.newPage(); continue; }
                        int count = Math.min(lines - offset, available);
                        writer.row(columns, offset, count);
                        offset += count;
                    }
                }
            } finally {
                writer.close();
            }
            document.save(path.toFile());
        }
    }

    private static List<String> wrap(String value, PDFont font, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            for (int index = 0; index < word.length();) {
                String prefix = line.length() == 0 ? "" : " ";
                String remaining = word.substring(index);
                if (font.getStringWidth(line + prefix + remaining) * FONT_SIZE / 1000 <= width) {
                    line.append(prefix).append(remaining);
                    break;
                }
                if (line.length() > 0) { lines.add(line.toString()); line.setLength(0); continue; }
                int end = index + 1;
                while (end < word.length() && font.getStringWidth(word.substring(index, end + 1)) * FONT_SIZE / 1000 <= width) end++;
                lines.add(word.substring(index, end));
                index = end;
            }
        }
        if (line.length() > 0 || lines.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static final class Writer implements AutoCloseable {
        private final PDDocument document;
        private final PDFont regular, bold;
        private PDPageContentStream stream;
        private float y;
        private int rowNumber;

        Writer(PDDocument document, PDFont regular, PDFont bold) { this.document = document; this.regular = regular; this.bold = bold; }
        void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            stream.setNonStrokingColor(NAVY); stream.addRect(LEFT, TOP - 8, 712, 38); stream.fill();
            text("Statements to request", bold, 16, LEFT + 10, TOP + 5, Color.WHITE);
            text("Prepared on " + DATE.format(LocalDate.now()) + " | Includes the current month", regular, 9, LEFT, TOP - 27, NAVY);
            y = TOP - 40;
            stream.setNonStrokingColor(BLUE); stream.addRect(LEFT, y - 24, 712, 24); stream.fill();
            String[] headers = {"Type", "Account", "Last statement", "Statements to request"};
            float x = LEFT;
            for (int i = 0; i < headers.length; i++) { text(headers[i], bold, 9, x + 6, y - 16, Color.WHITE); x += WIDTHS[i]; }
            y -= 24;
        }
        void row(List<List<String>> columns, int offset, int count) throws IOException {
            float height = count * LINE + 10;
            if (rowNumber++ % 2 == 0) { stream.setNonStrokingColor(LIGHT); stream.addRect(LEFT, y - height, 712, height); stream.fill(); }
            float x = LEFT;
            for (int col = 0; col < columns.size(); col++) {
                List<String> lines = columns.get(col);
                for (int i = offset; i < Math.min(lines.size(), offset + count); i++) text(lines.get(i), regular, FONT_SIZE, x + 6, y - 14 - (i - offset) * LINE, NAVY);
                x += WIDTHS[col];
            }
            y -= height;
        }
        void text(String value, PDFont font, float size, float x, float baseline, Color color) throws IOException {
            stream.beginText(); stream.setNonStrokingColor(color); stream.setFont(font, size); stream.newLineAtOffset(x, baseline); stream.showText(value); stream.endText();
        }
        public void close() throws IOException { if (stream != null) stream.close(); }
    }
}
