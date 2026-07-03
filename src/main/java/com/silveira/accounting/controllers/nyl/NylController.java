package com.silveira.accounting.controllers.nyl;

import com.silveira.accounting.application.nyl.NylApplicationService;
import com.silveira.accounting.application.nyl.NylGateway;
import com.silveira.accounting.application.nyl.NylImportGateway;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.SourceTotals;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NylController {
    private final NylApplicationService application;

    public NylController(NylApplicationService application) {
        this.application = application;
    }

    public NylGateway.SaveResult saveAll(List<NylRecord> records) { return application.saveAll(records); }
    public void recordCorrection(String fingerprint, String field, String oldValue, String newValue, String reason) { application.recordCorrection(fingerprint, field, oldValue, newValue, reason); }
    public void updateRecord(NylRecord record) { application.updateRecord(record); }
    public void delete(long id) { application.delete(id); }
    public List<NylRecord> findPendingReview() { return application.findPendingReview(); }
    public List<NylRecord> find(Integer year, Integer month, String concept, String type) { return application.find(year, month, concept, type); }
    public List<NylRecord> findByFingerprints(Set<String> fingerprints) { return application.findByFingerprints(fingerprints); }
    public Set<String> existingFingerprints(List<NylRecord> records) { return application.existingFingerprints(records); }
    public SourceTotals totals(Integer year, Integer month) { return application.totals(year, month); }
    public List<MonthlySourceTotals> monthlyTotals(Integer year) { return application.monthlyTotals(year); }
    public Optional<Double> findPdfResult(int year, int month) { return application.findPdfResult(year, month); }
    public Optional<Double> findNylBank(int year, int month) { return application.findNylBank(year, month); }
    public void savePdfResult(int year, int month, double amount) { application.savePdfResult(year, month, amount); }
    public void saveNylBank(int year, int month, double amount) { application.saveNylBank(year, month, amount); }
    public NylImportGateway.ParsedDocument parse(java.nio.file.Path pdf) { return application.parse(pdf); }
    public NylImportGateway.ParsedDocument parseWithOcr(java.nio.file.Path pdf) { return application.parseWithOcr(pdf); }
    public NylImportGateway.ParsedDocument parseWithAi(java.nio.file.Path pdf) { return application.parseWithAi(pdf); }
    public double detectDeclaredTotal(String text) { return application.detectDeclaredTotal(text); }
    public List<String> validate(List<NylRecord> records, double declaredTotal) { return application.validate(records, declaredTotal); }
}
