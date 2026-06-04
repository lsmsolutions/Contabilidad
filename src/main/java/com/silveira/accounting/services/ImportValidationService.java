package com.silveira.accounting.services;

import com.silveira.accounting.models.NylRecord;

import java.util.ArrayList;
import java.util.List;

public class ImportValidationService {
    public List<String> validateNyl(List<NylRecord> records, double declaredTotal) {
        List<String> warnings = new ArrayList<>();
        if (records.isEmpty()) {
            warnings.add("No se detectaron registros para guardar.");
            return warnings;
        }
        long reviewCount = records.stream().filter(NylRecord::isReviewRequired).count();
        if (reviewCount > 0) {
            warnings.add(reviewCount + " registros requieren revision manual antes de guardar.");
        }
        double calculated = records.stream().mapToDouble(NylRecord::getAmount).sum();
        if (!Double.isNaN(declaredTotal) && Math.abs(calculated - declaredTotal) > 0.01) {
            warnings.add("La suma detectada (" + calculated + ") no coincide con el total declarado (" + declaredTotal + ").");
        }
        records.stream()
            .filter(record -> Math.abs(record.getAmount()) > 100_000)
            .findFirst()
            .ifPresent(record -> warnings.add("Hay importes inusualmente altos. Revisa antes de confirmar."));
        return warnings;
    }
}
