package com.silveira.accounting.application.bank.usecase;

import com.silveira.accounting.application.bank.dto.BankImportAccountResolution;
import java.util.List;

public class ResolveBankImportAccountUseCase {
    public BankImportAccountResolution execute(String selectedAlias, String configuredAccountNumber, List<String> detectedAliases) {
        String configuredEnding = lastFour(configuredAccountNumber);
        List<String> detectedEndings = detectedAliases.stream().map(this::detectedEnding).toList();
        boolean comparable = !configuredEnding.isBlank()
            && !detectedEndings.isEmpty()
            && detectedEndings.stream().allMatch(ending -> ending.length() == 4);
        BankImportAccountResolution.Status status = !comparable
            ? BankImportAccountResolution.Status.UNKNOWN
            : detectedEndings.stream().allMatch(configuredEnding::equals)
                ? BankImportAccountResolution.Status.MATCH
                : BankImportAccountResolution.Status.MISMATCH;
        return new BankImportAccountResolution(status, selectedAlias, configuredEnding, List.copyOf(detectedAliases), detectedEndings);
    }

    private String detectedEnding(String alias) {
        if (alias == null || !alias.toLowerCase(java.util.Locale.ROOT).startsWith("cuenta_")) return "";
        return lastFour(alias);
    }

    private String lastFour(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() < 4 ? "" : digits.substring(digits.length() - 4);
    }
}
