package com.silveira.accounting;

import com.silveira.accounting.parsers.bank.BankStatementParser;
import com.silveira.accounting.parsers.NylPdfParser;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.repositories.NylRecordRepository;
import com.silveira.accounting.services.OcrService;
import com.silveira.accounting.application.card.service.CreditCardImportService;
import com.silveira.accounting.application.card.service.CreditCardAnalysisService;
import com.silveira.accounting.services.MortgageImportService;
import com.silveira.accounting.services.MortgageAnalysisService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.nio.file.Path;

public class ParserSmokeCheck {
    public static void main(String[] args) {
        Path bankPath = Path.of(args[0]);
        Path nylPath = Path.of(args[1]);
        if (args.length > 2 && args[2].equals("--db-nyl")) {
            DatabaseManager databaseManager = new DatabaseManager();
            databaseManager.initialize();
            var records = new NylRecordRepository(databaseManager).find(null, null, null, null);
            System.out.println("DB_NYL=" + records.size());
            records.stream().limit(5).forEach(r ->
                System.out.println(r.getYear() + "-" + r.getMonth() + " | " + r.getSection() + " | " + r.getConcept()));
            return;
        }
        if (args.length > 2 && args[2].equals("--dump-bank")) {
            String text = new PdfTextExtractor().extract(bankPath);
            text.lines().limit(240).forEach(System.out::println);
            return;
        }
        if (args.length > 2 && args[2].equals("--dump-card")) {
            Path cardPath = Path.of(args[3]);
            String text = new PdfTextExtractor().extract(cardPath);
            System.out.println("CARD_TEXT_CHARS=" + text.strip().length());
            text.lines().limit(260).forEach(System.out::println);
            return;
        }
        if (args.length > 2 && args[2].equals("--dump-all")) {
            Path pdfPath = Path.of(args[3]);
            String text = new PdfTextExtractor().extract(pdfPath);
            System.out.println("TEXT_CHARS=" + text.strip().length());
            text.lines().forEach(System.out::println);
            return;
        }
        if (args.length > 2 && args[2].equals("--ocr-card")) {
            Path cardPath = Path.of(args[3]);
            String text = new OcrService().extractText(cardPath).text();
            System.out.println("CARD_OCR_CHARS=" + text.strip().length());
            text.lines().limit(260).forEach(System.out::println);
            return;
        }
        if (args.length > 2 && args[2].equals("--parse-card")) {
            Path cardPath = Path.of(args[3]);
            var parsed = new CreditCardImportService().importPdf(cardPath);
            var statement = parsed.statement();
            var analysis = new CreditCardAnalysisService().analyze(statement);
            System.out.println("CARD_ALIAS=" + statement.getAccountAlias());
            System.out.println("DUE=" + statement.getPaymentDueDate());
            System.out.println("PERIOD=" + statement.getStatementStartDate() + " to " + statement.getStatementEndDate());
            System.out.println("NEW_BALANCE=" + statement.getNewBalance());
            System.out.println("MINIMUM=" + statement.getMinimumPaymentDue());
            System.out.println("PREVIOUS=" + statement.getPreviousBalance());
            System.out.println("PAYMENTS=" + statement.getPayments());
            System.out.println("CREDITS=" + statement.getOtherCredits());
            System.out.println("TRANSACTIONS=" + statement.getTransactions());
            System.out.println("INTEREST=" + statement.getInterestCharged());
            System.out.println("LIMIT=" + statement.getCreditLimit());
            System.out.println("UTILIZATION=" + analysis.creditUtilizationPercent());
            System.out.println("ALERTS=" + analysis.alerts().size());
            System.out.println("TX=" + parsed.transactions().size());
            return;
        }
        if (args.length > 2 && args[2].equals("--parse-mortgage")) {
            Path mortgagePath = Path.of(args[3]);
            var parsed = new MortgageImportService().importPdf(mortgagePath);
            var statement = parsed.statement();
            var analysis = new MortgageAnalysisService().analyze(statement);
            System.out.println("MORTGAGE_ALIAS=" + statement.getLoanAlias());
            System.out.println("SERVICER=" + statement.getServicerName());
            System.out.println("STATEMENT_DATE=" + statement.getStatementDate());
            System.out.println("DUE=" + statement.getPaymentDueDate());
            System.out.println("TOTAL_DUE=" + statement.getTotalDue());
            System.out.println("PRINCIPAL=" + statement.getPrincipalDue());
            System.out.println("INTEREST=" + statement.getInterestDue());
            System.out.println("ESCROW=" + statement.getEscrowDue());
            System.out.println("OUTSTANDING=" + statement.getOutstandingPrincipalBalance());
            System.out.println("DAYS=" + analysis.daysUntilDue());
            System.out.println("ALERTS=" + analysis.alerts().size());
            System.out.println("TX=" + parsed.transactions().size());
            return;
        }
        if (args.length > 2 && args[2].equals("--parse-bank")) {
            var bank = new BankStatementParser().parse(bankPath);
            System.out.println("BANK=" + bank.size());
            bank.stream().limit(80).forEach(t -> System.out.println(t.getDate() + " | " + t.getMovementType() + " | " + t.getAmount() + " | " + t.getProvider() + " | " + t.getDescription()));
            return;
        }
        if (args.length > 2 && args[2].equals("--ocr-parse-bank")) {
            Path bankPdf = Path.of(args[3]);
            String text = new OcrService().extractText(bankPdf).text();
            var bank = new BankStatementParser().parseText(text, bankPdf.getFileName().toString(), "ocr_revisado", true);
            System.out.println("BANK_OCR=" + bank.size());
            bank.stream().limit(80).forEach(t -> System.out.println(t.getDate() + " | " + t.getMovementType() + " | " + t.getAmount() + " | " + t.getProvider() + " | " + t.getDescription()));
            return;
        }
        if (args.length > 2 && args[2].equals("--inspect-nyl")) {
            try (PDDocument document = PDDocument.load(nylPath.toFile())) {
                String text = new PDFTextStripper().getText(document);
                System.out.println("PDF=true");
                System.out.println("PAGES=" + document.getNumberOfPages());
                System.out.println("TEXT_CHARS=" + text.strip().length());
                System.out.println("PDF_VERSION=" + document.getVersion());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return;
        }
        if (args.length > 2 && args[2].equals("--ocr-nyl")) {
            String text = new OcrService().extractText(nylPath).text();
            System.out.println("OCR_CHARS=" + text.strip().length());
            text.lines().limit(220).forEach(System.out::println);
            return;
        }
        if (args.length > 2 && args[2].equals("--ocr-parse-nyl")) {
            String text = new OcrService().extractText(nylPath).text();
            var records = new NylPdfParser().parseText(text, nylPath.getFileName().toString(), "ocr_revisado", true);
            System.out.println("NYL_OCR_RECORDS=" + records.size());
            records.stream().limit(30).forEach(r ->
                System.out.println(r.getYear() + "-" + r.getMonth() + " | " + r.getConcept() + " | " + r.getRecordType() + " | " + r.getAmount() + " | " + r.getReviewNotes()));
            return;
        }
        if (args.length > 2 && args[2].equals("--dump-nyl")) {
            String text = new PdfTextExtractor().extract(nylPath);
            text.lines().limit(160).forEach(System.out::println);
            return;
        }
        var bank = new BankStatementParser().parse(bankPath);
        var nyl = new NylPdfParser().parse(nylPath);
        System.out.println("BANK=" + bank.size());
        bank.stream().limit(3).forEach(t ->
            System.out.println(t.getDate() + " | " + t.getDescription() + " | " + t.getAmount() + " | " + t.getProvider()));
        System.out.println("NYL=" + nyl.size());
        nyl.stream().limit(5).forEach(r ->
            System.out.println(r.getYear() + "-" + r.getMonth() + " | " + r.getConcept() + " | " + r.getRecordType() + " | " + r.getAmount()));
    }
}
