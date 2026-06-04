package com.silveira.accounting.application.bank.dto;

import com.silveira.accounting.models.bank.BankTransaction;

import java.util.List;

public record BankImportReview(List<BankTransaction> newTransactions, int existingCount) {
}
