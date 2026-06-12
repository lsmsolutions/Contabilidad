package com.silveira.accounting.application.card.service;

import com.silveira.accounting.application.card.usecase.ImportCardPdfUseCase;
import com.silveira.accounting.application.card.usecase.SaveImportedCardStatementUseCase;
import com.silveira.accounting.parsers.CreditCardStatementParser;
import java.nio.file.Path;

public class CardImportApplicationService {
    private final ImportCardPdfUseCase importPdf;
    private final SaveImportedCardStatementUseCase saveImported;

    public CardImportApplicationService(
        CardStatementApplicationService statements,
        CardTransactionApplicationService transactions,
        CardAlertApplicationService alerts
    ) {
        CreditCardAnalysisService analysis = new CreditCardAnalysisService();
        importPdf = new ImportCardPdfUseCase(new CreditCardImportService());
        saveImported = new SaveImportedCardStatementUseCase(statements, transactions, alerts, analysis);
    }

    public CreditCardStatementParser.ParsedCreditCardStatement importPdf(Path pdf) {
        return importPdf.execute(pdf);
    }

    public long saveImported(String accountAlias, CreditCardStatementParser.ParsedCreditCardStatement parsed) {
        return saveImported.execute(accountAlias, parsed);
    }
}
