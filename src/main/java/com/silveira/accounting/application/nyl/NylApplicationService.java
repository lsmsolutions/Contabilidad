package com.silveira.accounting.application.nyl;

import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.SourceTotals;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NylApplicationService {
    private final NylGateway gateway;
    private final NylImportGateway imports;

    public NylApplicationService(NylGateway gateway, NylImportGateway imports) {
        this.gateway = gateway;
        this.imports = imports;
    }

    public NylGateway.SaveResult saveAll(List<NylRecord> records) {
        return gateway.saveAll(records);
    }

    public void recordCorrection(String fingerprint, String field, String oldValue, String newValue, String reason) {
        gateway.recordCorrection(fingerprint, field, oldValue, newValue, reason);
    }

    public void updateRecord(NylRecord record) {
        gateway.updateRecord(record);
    }

    public void delete(long id) {
        gateway.delete(id);
    }

    public List<NylRecord> findPendingReview() {
        return gateway.findPendingReview();
    }

    public List<NylRecord> find(Integer year, Integer month, String concept, String type) {
        return gateway.find(year, month, concept, type);
    }

    public List<NylRecord> findByFingerprints(Set<String> fingerprints) {
        return gateway.findByFingerprints(fingerprints);
    }

    public Set<String> existingFingerprints(List<NylRecord> records) {
        return gateway.existingFingerprints(records);
    }

    public SourceTotals totals(Integer year, Integer month) {
        return gateway.totals(year, month);
    }

    public List<MonthlySourceTotals> monthlyTotals(Integer year) {
        return gateway.monthlyTotals(year);
    }

    public Optional<Double> findPdfResult(int year, int month) {
        return gateway.findPdfResult(year, month);
    }

    public Optional<Double> findNylBank(int year, int month) {
        return gateway.findNylBank(year, month);
    }

    public void savePdfResult(int year, int month, double amount) {
        gateway.savePdfResult(year, month, amount);
    }

    public void saveNylBank(int year, int month, double amount) {
        gateway.saveNylBank(year, month, amount);
    }

    public NylImportGateway.ParsedDocument parse(java.nio.file.Path pdf) {
        return imports.parse(pdf);
    }

    public NylImportGateway.ParsedDocument parseWithOcr(java.nio.file.Path pdf) {
        return imports.parseWithOcr(pdf);
    }

    public NylImportGateway.ParsedDocument parseWithAi(java.nio.file.Path pdf) {
        return imports.parseWithAi(pdf);
    }

    public double detectDeclaredTotal(String text) {
        return imports.detectDeclaredTotal(text);
    }

    public List<String> validate(List<NylRecord> records, double declaredTotal) {
        return imports.validate(records, declaredTotal);
    }
}
