package com.silveira.accounting.application.nyl;

import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.SourceTotals;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NylGateway {
    SaveResult saveAll(List<NylRecord> records);
    void recordCorrection(String fingerprint, String field, String oldValue, String newValue, String reason);
    void updateRecord(NylRecord record);
    void delete(long id);
    List<NylRecord> findPendingReview();
    List<NylRecord> find(Integer year, Integer month, String concept, String type);
    List<NylRecord> findByFingerprints(Set<String> fingerprints);
    Set<String> existingFingerprints(List<NylRecord> records);
    SourceTotals totals(Integer year, Integer month);
    List<MonthlySourceTotals> monthlyTotals(Integer year);
    Optional<Double> findPdfResult(int year, int month);
    Optional<Double> findNylBank(int year, int month);
    void savePdfResult(int year, int month, double amount);
    void saveNylBank(int year, int month, double amount);

    record SaveResult(int inserted, Set<String> newConcepts) {
    }
}
