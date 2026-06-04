package com.silveira.accounting.services;

import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.ReconciliationItem;
import com.silveira.accounting.repositories.BankTransactionRepository;
import com.silveira.accounting.repositories.NylRecordRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReconciliationService {
    private final BankTransactionRepository bankRepository;
    private final NylRecordRepository nylRepository;

    public ReconciliationService(BankTransactionRepository bankRepository, NylRecordRepository nylRepository) {
        this.bankRepository = bankRepository;
        this.nylRepository = nylRepository;
    }

    public List<ReconciliationItem> preview(Integer year, Integer month) {
        List<BankTransaction> bank = bankRepository.find(year, month, "New York Life", null);
        bank = bank.stream().filter(transaction -> !transaction.isPendingReview()).toList();
        List<NylRecord> nyl = nylRepository.find(year, month, null, null).stream()
            .filter(record -> !record.isPendingReview())
            .filter(record -> !"deduccion".equals(record.getRecordType()))
            .sorted(Comparator.comparingDouble(record -> Math.abs(record.getAmount())))
            .toList();
        List<ReconciliationItem> items = new ArrayList<>();
        for (BankTransaction bankTransaction : bank) {
            NylRecord match = nyl.stream()
                .filter(record -> record.getYear() == bankTransaction.getYear() && record.getMonth() == bankTransaction.getMonth())
                .min(Comparator.comparingDouble(record -> Math.abs(Math.abs(bankTransaction.getAmount()) - Math.abs(record.getAmount()))))
                .orElse(null);
            if (match == null) {
                items.add(new ReconciliationItem(bankTransaction.getId(), 0, bankTransaction.getYear(), bankTransaction.getMonth(),
                    bankTransaction.getDescription(), "", bankTransaction.getAmount(), 0, bankTransaction.getAmount(), "Pendiente"));
                continue;
            }
            double difference = Math.abs(bankTransaction.getAmount()) - Math.abs(match.getAmount());
            String status = Math.abs(difference) < 0.01 ? "Conciliado" : "Posible coincidencia";
            if (bankTransaction.getDescription().toLowerCase().contains("cash loan")) {
                status = "Revisar Cash Loan";
            }
            items.add(new ReconciliationItem(bankTransaction.getId(), match.getId(), bankTransaction.getYear(), bankTransaction.getMonth(),
                bankTransaction.getDescription(), match.getConcept(), bankTransaction.getAmount(), match.getAmount(), difference, status));
        }
        return items;
    }
}
