package com.silveira.accounting.parsers.vehiclelease;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.utils.Money;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VolvoVehicleLeaseParser {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d/yyyy");
    private final PdfTextExtractor extractor = new PdfTextExtractor();

    public VehicleLeaseImportData parse(Path pdf) {
        String text = extractor.extract(pdf);
        if (!text.toLowerCase().contains("volvo car financial services")) {
            throw new IllegalArgumentException("El PDF no corresponde a Volvo Car Financial Services.");
        }

        VehicleLeaseAccount account = new VehicleLeaseAccount();
        Matcher vehicleParts = Pattern.compile("(?m)^(\\d{4})\\s+VOLVO\\s+(.+?)$", Pattern.CASE_INSENSITIVE).matcher(text);
        if (vehicleParts.find()) {
            account.setVehicleYear(Integer.parseInt(vehicleParts.group(1)));
            String[] modelParts = vehicleParts.group(2).trim().split("\\s+", 2);
            account.setModel(modelParts[0]);
            account.setTrim(modelParts.length > 1 ? modelParts[1] : "");
        }
        account.setMake("Volvo");
        account.setProviderName("Volvo Car Financial Services");
        account.setAccountNumber(match(text, "Acct#:\\s*(\\d+)"));
        account.setVin(match(text, "VIN:\\s*([A-Z0-9]+)"));
        account.setMaturityDate(date(text, "Maturity Date:\\s*(\\d{1,2}/\\d{1,2}/\\d{4})"));
        account.setAlias(defaultAlias(account));

        VehicleLeaseStatement statement = new VehicleLeaseStatement();
        statement.setAccountAlias(account.getAlias());
        statement.setStatementDate(date(text, "Statement Date:\\s*(\\d{1,2}/\\d{1,2}/\\d{4})"));
        statement.setDueDate(date(text, "(?m)^Due Date\\s+(\\d{1,2}/\\d{1,2}/\\d{4})$"));
        statement.setTotalAmountDue(money(text, "(?m)^Total Amount Due\\s+\\$([\\d,]+\\.\\d{2})$"));
        statement.setLastPaymentDate(date(text, "Received On\\s+(\\d{1,2}/\\d{1,2}/\\d{4})"));
        statement.setLastPaymentAmount(money(text, "Received Amount\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setPaymentsMade(integer(text, "Payments Made:\\s*(\\d+)"));
        statement.setPaymentsRemaining(integer(text, "Payments Remaining:\\s*(\\d+)"));
        statement.setLeasePayment(money(text, "Lease Payment\\(s\\)\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setSalesUseTax(money(text, "Sales Use /Tax\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setPropertyTax(money(text, "Property Tax\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setParkingTickets(money(text, "Parking Ticket\\(s\\)\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setReturnedCheckFees(money(text, "Returned Check Fee\\(s\\)\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setMiscellaneousCharges(money(text, "Miscellaneous Charge\\(s\\)\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setPastDueAmount(money(text, "Past Due Amount\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setLateCharges(money(text, "Late Charge\\(s\\)\\s+\\$([\\d,]+\\.\\d{2})"));
        statement.setSourcePdfPath(pdf.toAbsolutePath().toString());
        statement.setReviewRequired(true);
        statement.setPendingReview(true);
        statement.setReviewNotes("Revisar contra el PDF original");
        return new VehicleLeaseImportData(account, statement);
    }

    private String defaultAlias(VehicleLeaseAccount account) {
        String model = account.getModel() == null || account.getModel().isBlank() ? "Vehicle" : account.getModel();
        return "Volvo " + model;
    }

    private LocalDate date(String text, String regex) {
        String value = match(text, regex);
        return value.isBlank() ? null : LocalDate.parse(value, DATE);
    }

    private int integer(String text, String regex) {
        String value = match(text, regex);
        return value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private double money(String text, String regex) {
        String value = match(text, regex);
        return value.isBlank() ? 0 : Money.parse(value);
    }

    private String match(String text, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
