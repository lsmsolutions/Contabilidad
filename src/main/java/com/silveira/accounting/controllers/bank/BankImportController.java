package com.silveira.accounting.controllers.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankImportSaveResult;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.parsers.bank.BankStatementParser;
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
    private final Map<String, BankStatementPeriod> pendingPeriods = new HashMap<>();

    public BankImportController(BankApplicationService bank, OcrService ocrService) {
        this.bank = bank;
        this.ocrService = ocrService;
        this.parser = new BankStatementParser();
        this.revolutParser = new RevolutBankStatementParser();
    }

    public List<BankTransaction> parsePdf(Path pdf) {
        var revolut = revolutParser.parseIfRevolut(pdf);
        if (revolut.isPresent()) {
            BankStatementPeriod period = revolut.get().period();
            pendingPeriods.put(period.sourcePdf(), period);
            return revolut.get().transactions();
        }
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
