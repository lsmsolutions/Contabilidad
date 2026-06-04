package com.silveira.accounting.controllers;

import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.parsers.bank.BankStatementParser;
import com.silveira.accounting.parsers.NylPdfParser;

import java.nio.file.Path;
import java.util.List;

public class ImportController {
    private final BankStatementParser bankStatementParser = new BankStatementParser();
    private final NylPdfParser nylPdfParser = new NylPdfParser();

    public List<BankTransaction> previewBank(Path pdf) {
        return bankStatementParser.parse(pdf);
    }

    public List<NylRecord> previewNyl(Path pdf) {
        return nylPdfParser.parse(pdf);
    }
}
