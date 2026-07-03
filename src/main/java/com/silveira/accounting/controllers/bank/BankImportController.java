package com.silveira.accounting.controllers.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankImportData;
import com.silveira.accounting.application.bank.dto.BankImportSaveResult;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.application.importing.DocumentImportService;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.parsers.bank.BankStatementParser;
import com.silveira.accounting.parsers.bank.OpenAiBankAiImportGateway;
import com.silveira.accounting.parsers.bank.RevolutBankStatementParser;
import com.silveira.accounting.services.OcrService;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankImportController {
    private final BankApplicationService bank;
    private final BankStatementParser parser;
    private final RevolutBankStatementParser revolutParser;
    private final OcrService ocrService;
    private final DocumentImportService<BankImportData> documentImports;
    private final Map<String, BankStatementPeriod> pendingPeriods = new HashMap<>();

    public BankImportController(BankApplicationService bank, OcrService ocrService) {
        this.bank = bank;
        this.ocrService = ocrService;
        this.parser = new BankStatementParser();
        this.revolutParser = new RevolutBankStatementParser();
        this.documentImports = new DocumentImportService<>(this::parseWithKnownReaders, new OpenAiBankAiImportGateway());
    }

    public List<BankTransaction> parsePdf(Path pdf) {
        return trackPeriod(documentImports.importPdf(pdf)).transactions();
    }

    public List<BankTransaction> parsePdfWithAi(Path pdf) {
        return trackPeriod(documentImports.importPdfWithAi(pdf)).transactions();
    }

    private BankImportData parseWithKnownReaders(Path pdf) {
        var revolut = revolutParser.parseIfRevolut(pdf);
        if (revolut.isPresent()) {
            BankStatementPeriod period = revolut.get().period();
            return new BankImportData(revolut.get().transactions(), java.util.Optional.of(period));
        }
        return BankImportData.transactionsOnly(parser.parse(pdf));
    }

    private BankImportData trackPeriod(BankImportData data) {
        data.period().ifPresent(period -> pendingPeriods.put(period.sourcePdf(), period));
        return data;
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
        BankImportSaveResult result = bank.imports().saveRows(rows);
        savePendingStatementPeriods(rows);
        return result;
    }

    private void savePendingStatementPeriods(List<BankTransaction> rows) {
        Map<String, String> accountBySource = new HashMap<>();
        for (BankTransaction row : rows) {
            if (row.getSourcePdf() != null && !row.getSourcePdf().isBlank()
                && row.getAccountAlias() != null && !row.getAccountAlias().isBlank()) {
                accountBySource.putIfAbsent(row.getSourcePdf(), row.getAccountAlias());
            }
        }
        for (Map.Entry<String, String> entry : accountBySource.entrySet()) {
            BankStatementPeriod period = pendingPeriods.get(entry.getKey());
            if (period == null) {
                continue;
            }
            bank.periods().updatePeriod(
                entry.getValue(),
                period.sourcePdf(),
                period.periodStart(),
                period.periodEnd(),
                period.openingBalance(),
                period.statementEndingBalance()
            );
            pendingPeriods.remove(entry.getKey());
        }
    }
}
