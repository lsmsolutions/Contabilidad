package com.silveira.accounting.services;

import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.ReconciliationItem;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExcelExportService {
    public void export(Path target, List<BankTransaction> bank, List<NylRecord> nyl, List<ReconciliationItem> reconciliations) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            bankSheet(workbook, headerStyle, bank);
            nylSheet(workbook, headerStyle, nyl);
            reconciliationSheet(workbook, headerStyle, reconciliations);

            try (OutputStream out = Files.newOutputStream(target)) {
                workbook.write(out);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo exportar Excel", exception);
        }
    }

    public void exportBankMonthly(Path target, List<BankTransaction> bank) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            bankSheet(workbook, headerStyle, bank);
            write(workbook, target);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo exportar Banco mensual", exception);
        }
    }

    public void exportNylMonthly(Path target, List<NylRecord> nyl) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            nylSheet(workbook, headerStyle, nyl);
            write(workbook, target);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo exportar NYL mensual", exception);
        }
    }

    public void exportCreditCardMonthly(Path target, List<CreditCardStatement> statements, List<CreditCardTransaction> transactions) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            creditCardStatementSheet(workbook, headerStyle, statements);
            creditCardTransactionSheet(workbook, headerStyle, transactions);
            write(workbook, target);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo exportar Tarjeta mensual", exception);
        }
    }

    public void exportMortgageMonthly(Path target, List<MortgageStatement> statements, List<MortgageTransaction> transactions) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            mortgageStatementSheet(workbook, headerStyle, statements);
            mortgageTransactionSheet(workbook, headerStyle, transactions);
            write(workbook, target);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo exportar Hipoteca mensual", exception);
        }
    }

    public void exportReconciliationMonthly(Path target, List<BankTransaction> bank, List<CreditCardStatement> cardStatements,
                                            List<CreditCardTransaction> cardTransactions, List<MortgageStatement> mortgageStatements,
                                            List<MortgageTransaction> mortgageTransactions, List<NylRecord> nyl,
                                            List<ReconciliationItem> reconciliations) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            reconciliationSummarySheet(workbook, headerStyle, bank, cardStatements, cardTransactions, mortgageStatements, mortgageTransactions, nyl, reconciliations);
            bankSheet(workbook, headerStyle, bank);
            creditCardStatementSheet(workbook, headerStyle, cardStatements);
            creditCardTransactionSheet(workbook, headerStyle, cardTransactions);
            mortgageStatementSheet(workbook, headerStyle, mortgageStatements);
            mortgageTransactionSheet(workbook, headerStyle, mortgageTransactions);
            nylSheet(workbook, headerStyle, nyl);
            reconciliationSheet(workbook, headerStyle, reconciliations);
            write(workbook, target);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo exportar conciliación mensual", exception);
        }
    }

    private void bankSheet(Workbook workbook, CellStyle headerStyle, List<BankTransaction> bank) {
        Sheet sheet = workbook.createSheet("Banco");
        header(sheet, headerStyle, "Fecha", "Descripción", "Importe", "Tipo", "Proveedor", "Referencia", "Mes", "Año", "PDF");
        int rowIndex = 1;
        for (BankTransaction item : bank) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getDate().toString());
            row.createCell(1).setCellValue(item.getDescription());
            row.createCell(2).setCellValue(item.getAmount());
            row.createCell(3).setCellValue(item.getMovementType());
            row.createCell(4).setCellValue(item.getProvider());
            row.createCell(5).setCellValue(item.getReference());
            row.createCell(6).setCellValue(item.getMonth());
            row.createCell(7).setCellValue(item.getYear());
            row.createCell(8).setCellValue(item.getSourcePdf());
        }
        autosize(sheet, 9);
    }

    private void creditCardStatementSheet(Workbook workbook, CellStyle headerStyle, List<CreditCardStatement> statements) {
        Sheet sheet = workbook.createSheet("Resumen Tarjeta");
        header(sheet, headerStyle, "Cuenta", "Banco", "Tarjeta", "Inicio ciclo", "Cierre ciclo", "Fecha límite de pago", "Deuda cierre",
            "Pago mínimo", "Saldo anterior", "Pagos", "Créditos", "Compras", "Cash advances", "Fees", "Intereses",
            "Límite banco", "Crédito disponible", "Revisión", "Notas", "PDF");
        int rowIndex = 1;
        for (CreditCardStatement item : statements) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getAccountAlias());
            row.createCell(1).setCellValue(item.getBankName());
            row.createCell(2).setCellValue(item.getCardName());
            row.createCell(3).setCellValue(item.getStatementStartDate() == null ? "" : item.getStatementStartDate().toString());
            row.createCell(4).setCellValue(item.getStatementEndDate() == null ? "" : item.getStatementEndDate().toString());
            row.createCell(5).setCellValue(item.getPaymentDueDate() == null ? "" : item.getPaymentDueDate().toString());
            row.createCell(6).setCellValue(item.getNewBalance());
            row.createCell(7).setCellValue(item.getMinimumPaymentDue());
            row.createCell(8).setCellValue(item.getPreviousBalance());
            row.createCell(9).setCellValue(item.getPayments());
            row.createCell(10).setCellValue(item.getOtherCredits());
            row.createCell(11).setCellValue(item.getTransactions());
            row.createCell(12).setCellValue(item.getCashAdvances());
            row.createCell(13).setCellValue(item.getFeesCharged());
            row.createCell(14).setCellValue(item.getInterestCharged());
            row.createCell(15).setCellValue(item.getCreditLimit());
            row.createCell(16).setCellValue(item.getAvailableCredit());
            row.createCell(17).setCellValue(item.isPendingReview() ? "Pdte revision" : "OK");
            row.createCell(18).setCellValue(item.getReviewNotes());
            row.createCell(19).setCellValue(item.getSourcePdfPath());
        }
        autosize(sheet, 20);
    }

    private void creditCardTransactionSheet(Workbook workbook, CellStyle headerStyle, List<CreditCardTransaction> transactions) {
        Sheet sheet = workbook.createSheet("Movimientos Tarjeta");
        header(sheet, headerStyle, "Fecha", "Posteo", "Descripción", "Importe", "Tipo", "Categoría", "Revisión", "Notas");
        int rowIndex = 1;
        for (CreditCardTransaction item : transactions) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getTransactionDate() == null ? "" : item.getTransactionDate().toString());
            row.createCell(1).setCellValue(item.getPostDate() == null ? "" : item.getPostDate().toString());
            row.createCell(2).setCellValue(item.getDescription());
            row.createCell(3).setCellValue(item.getAmount());
            row.createCell(4).setCellValue(item.getType());
            row.createCell(5).setCellValue(item.getCategory());
            row.createCell(6).setCellValue(item.isPendingReview() ? "Pdte revision" : "OK");
            row.createCell(7).setCellValue(item.getReviewNotes());
        }
        autosize(sheet, 8);
    }

    private void mortgageStatementSheet(Workbook workbook, CellStyle headerStyle, List<MortgageStatement> statements) {
        Sheet sheet = workbook.createSheet("Resumen Hipoteca");
        header(sheet, headerStyle, "Hipoteca", "Entidad", "Statement Date", "Fecha límite", "Total a pagar", "Principal",
            "Intereses", "Escrow", "Fees", "Deuda principal pendiente", "Tasa", "Maturity", "Escrow balance",
            "Unapplied", "Revisión", "Notas", "PDF");
        int rowIndex = 1;
        for (MortgageStatement item : statements) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getLoanAlias());
            row.createCell(1).setCellValue(item.getServicerName());
            row.createCell(2).setCellValue(item.getStatementDate() == null ? "" : item.getStatementDate().toString());
            row.createCell(3).setCellValue(item.getPaymentDueDate() == null ? "" : item.getPaymentDueDate().toString());
            row.createCell(4).setCellValue(item.getTotalDue() > 0 ? item.getTotalDue() : item.getPaymentAmountDue());
            row.createCell(5).setCellValue(item.getPrincipalDue());
            row.createCell(6).setCellValue(item.getInterestDue());
            row.createCell(7).setCellValue(item.getEscrowDue());
            row.createCell(8).setCellValue(item.getFees() + item.getOtherFeesAndCharges());
            row.createCell(9).setCellValue(item.getOutstandingPrincipalBalance());
            row.createCell(10).setCellValue(item.getInterestRate());
            row.createCell(11).setCellValue(item.getMaturityDate() == null ? "" : item.getMaturityDate().toString());
            row.createCell(12).setCellValue(item.getEscrowBalance());
            row.createCell(13).setCellValue(item.getUnappliedFunds());
            row.createCell(14).setCellValue(item.isPendingReview() ? "Pdte revision" : "OK");
            row.createCell(15).setCellValue(item.getReviewNotes());
            row.createCell(16).setCellValue(item.getSourcePdfPath());
        }
        autosize(sheet, 17);
    }

    private void mortgageTransactionSheet(Workbook workbook, CellStyle headerStyle, List<MortgageTransaction> transactions) {
        Sheet sheet = workbook.createSheet("Movimientos Hipoteca");
        header(sheet, headerStyle, "Fecha", "Descripción", "Total", "Principal", "Intereses", "Escrow", "Fees", "Unapplied",
            "Corporate advance", "Other", "Revisión", "Notas");
        int rowIndex = 1;
        for (MortgageTransaction item : transactions) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getTransactionDate() == null ? "" : item.getTransactionDate().toString());
            row.createCell(1).setCellValue(item.getDescription());
            row.createCell(2).setCellValue(item.getTotal());
            row.createCell(3).setCellValue(item.getPrincipal());
            row.createCell(4).setCellValue(item.getInterest());
            row.createCell(5).setCellValue(item.getEscrow());
            row.createCell(6).setCellValue(item.getFees());
            row.createCell(7).setCellValue(item.getUnapplied());
            row.createCell(8).setCellValue(item.getCorporateAdvance());
            row.createCell(9).setCellValue(item.getOther());
            row.createCell(10).setCellValue(item.isPendingReview() ? "Pdte revision" : "OK");
            row.createCell(11).setCellValue(item.getReviewNotes());
        }
        autosize(sheet, 12);
    }

    private void nylSheet(Workbook workbook, CellStyle headerStyle, List<NylRecord> nyl) {
        Sheet sheet = workbook.createSheet("New York Life");
        header(sheet, headerStyle, "Año", "Mes", "Concepto", "Tipo", "Importe", "PDF", "Estado", "Revisión", "Notas");
        int rowIndex = 1;
        for (NylRecord item : nyl) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getYear());
            row.createCell(1).setCellValue(item.getMonth());
            row.createCell(2).setCellValue(item.getConcept());
            row.createCell(3).setCellValue(item.getRecordType());
            row.createCell(4).setCellValue(item.getAmount());
            row.createCell(5).setCellValue(item.getSourcePdf());
            row.createCell(6).setCellValue(item.getImportStatus());
            row.createCell(7).setCellValue(item.isReviewRequired() ? "Revisar" : "OK");
            row.createCell(8).setCellValue(item.getReviewNotes());
        }
        autosize(sheet, 9);
    }

    private void reconciliationSheet(Workbook workbook, CellStyle headerStyle, List<ReconciliationItem> items) {
        Sheet sheet = workbook.createSheet("Conciliación");
        header(sheet, headerStyle, "Año", "Mes", "Banco", "NYL", "Banco importe", "NYL importe", "Diferencia", "Estado");
        int rowIndex = 1;
        for (ReconciliationItem item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.year());
            row.createCell(1).setCellValue(item.month());
            row.createCell(2).setCellValue(item.bankDescription());
            row.createCell(3).setCellValue(item.nylConcept());
            row.createCell(4).setCellValue(item.bankAmount());
            row.createCell(5).setCellValue(item.nylAmount());
            row.createCell(6).setCellValue(item.difference());
            row.createCell(7).setCellValue(item.status());
        }
        autosize(sheet, 8);
    }

    private void reconciliationSummarySheet(Workbook workbook, CellStyle headerStyle, List<BankTransaction> bank,
                                            List<CreditCardStatement> cardStatements, List<CreditCardTransaction> cardTransactions,
                                            List<MortgageStatement> mortgageStatements, List<MortgageTransaction> mortgageTransactions,
                                            List<NylRecord> nyl, List<ReconciliationItem> reconciliations) {
        Sheet sheet = workbook.createSheet("Resumen mensual");
        header(sheet, headerStyle, "Area", "Métrica", "Valor");
        int rowIndex = 1;
        rowIndex = summaryRow(sheet, rowIndex, "Banco", "Depósitos revisados", bank.stream().filter(b -> !b.isPendingReview() && b.getAmount() > 0).mapToDouble(BankTransaction::getAmount).sum());
        rowIndex = summaryRow(sheet, rowIndex, "Banco", "Salidas revisadas", Math.abs(bank.stream().filter(b -> !b.isPendingReview() && b.getAmount() < 0).mapToDouble(BankTransaction::getAmount).sum()));
        rowIndex = summaryRow(sheet, rowIndex, "Banco", "Pendientes", bank.stream().filter(BankTransaction::isPendingReview).count());
        rowIndex = summaryRow(sheet, rowIndex, "Tarjetas", "Deuda revisada", cardStatements.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getNewBalance).sum());
        rowIndex = summaryRow(sheet, rowIndex, "Tarjetas", "Intereses revisados", cardTransactions.stream().filter(t -> !t.isPendingReview() && "interes".equalsIgnoreCase(t.getType())).mapToDouble(CreditCardTransaction::getAmount).sum());
        rowIndex = summaryRow(sheet, rowIndex, "Tarjetas", "Pendientes", cardTransactions.stream().filter(CreditCardTransaction::isPendingReview).count() + cardStatements.stream().filter(CreditCardStatement::isPendingReview).count());
        rowIndex = summaryRow(sheet, rowIndex, "Hipotecas", "Deuda a pagar revisada", mortgageStatements.stream().filter(s -> !s.isPendingReview()).mapToDouble(s -> s.getTotalDue() > 0 ? s.getTotalDue() : s.getPaymentAmountDue()).sum());
        rowIndex = summaryRow(sheet, rowIndex, "Hipotecas", "Principal revisado", mortgageStatements.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getPrincipalDue).sum());
        rowIndex = summaryRow(sheet, rowIndex, "Hipotecas", "Intereses revisados", mortgageStatements.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getInterestDue).sum());
        rowIndex = summaryRow(sheet, rowIndex, "Hipotecas", "Pendientes", mortgageTransactions.stream().filter(MortgageTransaction::isPendingReview).count() + mortgageStatements.stream().filter(MortgageStatement::isPendingReview).count());
        rowIndex = summaryRow(sheet, rowIndex, "NYL", "Comisiones revisadas", nyl.stream().filter(n -> !n.isPendingReview() && n.getAmount() > 0).mapToDouble(NylRecord::getAmount).sum());
        rowIndex = summaryRow(sheet, rowIndex, "NYL", "Deducciones revisadas", Math.abs(nyl.stream().filter(n -> !n.isPendingReview() && n.getAmount() < 0).mapToDouble(NylRecord::getAmount).sum()));
        rowIndex = summaryRow(sheet, rowIndex, "NYL", "Pendientes", nyl.stream().filter(NylRecord::isPendingReview).count());
        summaryRow(sheet, rowIndex, "Conciliación", "Diferencias / posibles coincidencias", reconciliations.stream().filter(item -> !"Conciliado".equals(item.status())).count());
        autosize(sheet, 3);
    }

    private int summaryRow(Sheet sheet, int rowIndex, String area, String metric, double value) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(area);
        row.createCell(1).setCellValue(metric);
        row.createCell(2).setCellValue(value);
        return rowIndex;
    }

    private void header(Sheet sheet, CellStyle style, String... labels) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < labels.length; i++) {
            row.createCell(i).setCellValue(labels[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        return headerStyle;
    }

    private void write(Workbook workbook, Path target) throws IOException {
        try (OutputStream out = Files.newOutputStream(target)) {
            workbook.write(out);
        }
    }

    private void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}

