package com.silveira.accounting.controllers.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankImportSaveResult;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.parsers.bank.BankStatementParser;
import com.silveira.accounting.services.OcrService;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class BankImportController {
    private final BankApplicationService bank;
    private final BankStatementParser parser;
    private final OcrService ocrService;

    public BankImportController(BankApplicationService bank, OcrService ocrService) {
        this.bank = bank;
        this.ocrService = ocrService;
        this.parser = new BankStatementParser();
    }

    public List<BankTransaction> parsePdf(Path pdf) {
        return parser.parse(pdf);
    }

    public List<BankTransaction> parseScannedPdf(Path pdf) {
        OcrService.OcrResult ocr = ocrService.extractText(pdf);
        return parser.parseText(ocr.text(), pdf.getFileName().toString(), "ocr_revisado", true);
    }

    public void saveManualPeriod(String accountAlias, LocalDate start, LocalDate end, double openingBalance, double statementEndingBalance) {
        bank.periods().saveManualPeriod(accountAlias, start, end, openingBalance, statementEndingBalance);
    }

    public void deletePeriod(BankPeriodSummary selectedPeriod) {
        bank.imports().deletePeriod(selectedPeriod.statementPeriod(), selectedPeriod.transactions());
    }

    public BankTransaction createManualTransaction(String accountAlias, LocalDate fallbackDate, BankStatementPeriod period) {
        return bank.imports().createManualTransaction(accountAlias, fallbackDate, period);
    }

    public void savePendingReviewRows(List<BankTransaction> rows) {
        bank.imports().savePendingReviewRows(rows);
    }

    public List<BankTransaction> findPendingReviewRows() {
        return bank.transactions().findPendingReview();
    }

    public BankImportSaveResult saveRows(List<BankTransaction> rows) {
        return bank.imports().saveRows(rows);
    }
}
