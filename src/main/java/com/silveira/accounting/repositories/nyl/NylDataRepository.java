package com.silveira.accounting.repositories.nyl;

import com.silveira.accounting.application.nyl.NylGateway;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.repositories.NylMonthlyResultRepository;
import com.silveira.accounting.repositories.NylRecordRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NylDataRepository implements NylGateway {
    private final NylRecordRepository records;
    private final NylMonthlyResultRepository monthlyResults;

    public NylDataRepository(NylRecordRepository records, NylMonthlyResultRepository monthlyResults) {
        this.records = records;
        this.monthlyResults = monthlyResults;
    }

    @Override
    public SaveResult saveAll(List<NylRecord> values) {
        NylRecordRepository.SaveResult result = records.saveAll(values);
        return new SaveResult(result.inserted(), result.newConcepts());
    }

    @Override public void recordCorrection(String fingerprint, String field, String oldValue, String newValue, String reason) { records.recordCorrection(fingerprint, field, oldValue, newValue, reason); }
    @Override public void updateRecord(NylRecord record) { records.updateRecord(record); }
    @Override public void delete(long id) { records.delete(id); }
    @Override public List<NylRecord> findPendingReview() { return records.findPendingReview(); }
    @Override public List<NylRecord> find(Integer year, Integer month, String concept, String type) { return records.find(year, month, concept, type); }
    @Override public List<NylRecord> findByFingerprints(Set<String> fingerprints) { return records.findByFingerprints(fingerprints); }
    @Override public Set<String> existingFingerprints(List<NylRecord> values) { return records.existingFingerprints(values); }
    @Override public SourceTotals totals(Integer year, Integer month) { return records.totals(year, month); }
    @Override public List<MonthlySourceTotals> monthlyTotals(Integer year) { return records.monthlyTotals(year); }
    @Override public Optional<Double> findPdfResult(int year, int month) { return monthlyResults.findPdfResult(year, month); }
    @Override public Optional<Double> findNylBank(int year, int month) { return monthlyResults.findNylBank(year, month); }
    @Override public void savePdfResult(int year, int month, double amount) { monthlyResults.savePdfResult(year, month, amount); }
    @Override public void saveNylBank(int year, int month, double amount) { monthlyResults.saveNylBank(year, month, amount); }
}
