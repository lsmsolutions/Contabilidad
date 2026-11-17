package com.silveira.accounting.application.bank.dto;

import java.util.List;

public record BankImportAccountResolution(
    Status status,
    String selectedAlias,
    String configuredEnding,
    List<String> detectedAliases,
    List<String> detectedEndings
) {
    public enum Status { MATCH, MISMATCH, UNKNOWN }
}
