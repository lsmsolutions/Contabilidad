package com.silveira.accounting.parsers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.silveira.accounting.application.importing.AiDocumentImportGateway;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OpenAiMortgageAiImportGateway implements AiDocumentImportGateway<MortgageStatementParser.ParsedMortgageStatement> {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final PdfTextExtractor extractor;
    private final HttpClient httpClient;

    public OpenAiMortgageAiImportGateway() {
        this(new PdfTextExtractor(), HttpClient.newHttpClient());
    }

    OpenAiMortgageAiImportGateway(PdfTextExtractor extractor, HttpClient httpClient) {
        this.extractor = extractor;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<MortgageStatementParser.ParsedMortgageStatement> importPdf(Path pdf, RuntimeException parserFailure) {
        String apiKey = env("OPENAI_API_KEY");
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY no esta configurada. Define la clave antes de intentar leer con IA.", parserFailure);
        }
        String text = extractor.extract(pdf);
        JsonObject extracted = requestExtraction(apiKey, model(), text, parserFailure);
        return Optional.of(map(extracted, pdf));
    }

    private JsonObject requestExtraction(String apiKey, String model, String pdfText, RuntimeException parserFailure) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("instructions", instructions(parserFailure));
        body.addProperty("input", pdfText);
        body.add("text", textFormat());
        body.addProperty("max_output_tokens", 3500);
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI devolvio HTTP " + response.statusCode() + ": " + response.body());
            }
            return JsonParser.parseString(outputText(response.body())).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo conectar con OpenAI.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La lectura con IA fue interrumpida.", exception);
        }
    }

    private String outputText(String responseBody) {
        JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
        JsonArray output = root.getAsJsonArray("output");
        if (output == null) {
            throw new IllegalStateException("OpenAI no devolvio contenido estructurado.");
        }
        for (JsonElement outputItem : output) {
            JsonArray content = outputItem.getAsJsonObject().getAsJsonArray("content");
            if (content == null) {
                continue;
            }
            for (JsonElement contentItem : content) {
                JsonObject item = contentItem.getAsJsonObject();
                if ("output_text".equals(text(item, "type"))) {
                    return text(item, "text");
                }
            }
        }
        throw new IllegalStateException("OpenAI no devolvio texto JSON para importar.");
    }

    private MortgageStatementParser.ParsedMortgageStatement map(JsonObject root, Path pdf) {
        JsonObject statementJson = root.getAsJsonObject("statement");
        MortgageStatement statement = new MortgageStatement();
        statement.setServicerName(text(statementJson, "servicerName"));
        statement.setStatementDate(date(statementJson, "statementDate"));
        statement.setPaymentDueDate(date(statementJson, "paymentDueDate"));
        statement.setPaymentAmountDue(decimal(statementJson, "paymentAmountDue"));
        statement.setLateFeeDate(date(statementJson, "lateFeeDate"));
        statement.setLateFeeAmount(decimal(statementJson, "lateFeeAmount"));
        statement.setLoanNumber(text(statementJson, "loanNumber"));
        statement.setPropertyAddress(text(statementJson, "propertyAddress"));
        statement.setOriginalPrincipalBalance(decimal(statementJson, "originalPrincipalBalance"));
        statement.setOutstandingPrincipalBalance(decimal(statementJson, "outstandingPrincipalBalance"));
        statement.setMaturityDate(date(statementJson, "maturityDate"));
        statement.setInterestRate(decimal(statementJson, "interestRate"));
        statement.setEscrowBalance(decimal(statementJson, "escrowBalance"));
        statement.setUnappliedFunds(decimal(statementJson, "unappliedFunds"));
        statement.setCurrentPaymentDue(decimal(statementJson, "currentPaymentDue"));
        statement.setPrincipalDue(decimal(statementJson, "principalDue"));
        statement.setInterestDue(decimal(statementJson, "interestDue"));
        statement.setEscrowDue(decimal(statementJson, "escrowDue"));
        statement.setRegularMonthlyPayment(decimal(statementJson, "regularMonthlyPayment"));
        statement.setPastDueAmount(decimal(statementJson, "pastDueAmount"));
        statement.setFees(decimal(statementJson, "fees"));
        statement.setOtherFeesAndCharges(decimal(statementJson, "otherFeesAndCharges"));
        statement.setTotalDue(decimal(statementJson, "totalDue"));
        statement.setPastPaidPrincipalSinceLastStatement(decimal(statementJson, "pastPaidPrincipalSinceLastStatement"));
        statement.setPastPaidPrincipalYearToDate(decimal(statementJson, "pastPaidPrincipalYearToDate"));
        statement.setPastPaidInterestSinceLastStatement(decimal(statementJson, "pastPaidInterestSinceLastStatement"));
        statement.setPastPaidInterestYearToDate(decimal(statementJson, "pastPaidInterestYearToDate"));
        statement.setPastPaidEscrowSinceLastStatement(decimal(statementJson, "pastPaidEscrowSinceLastStatement"));
        statement.setPastPaidEscrowYearToDate(decimal(statementJson, "pastPaidEscrowYearToDate"));
        statement.setPastPaidTotalSinceLastStatement(decimal(statementJson, "pastPaidTotalSinceLastStatement"));
        statement.setPastPaidTotalYearToDate(decimal(statementJson, "pastPaidTotalYearToDate"));
        statement.setSourcePdfPath(pdf.toAbsolutePath().toString());
        statement.setImportStatus("ia_en_revision");
        statement.setReviewRequired(true);
        statement.setPendingReview(true);
        statement.setReviewNotes("Extraido por IA. Revisar contra el PDF original antes de guardar como revisado.");

        List<MortgageTransaction> transactions = new ArrayList<>();
        JsonArray transactionRows = root.getAsJsonArray("transactions");
        if (transactionRows != null) {
            for (JsonElement element : transactionRows) {
                JsonObject row = element.getAsJsonObject();
                MortgageTransaction transaction = new MortgageTransaction(
                    0,
                    0,
                    date(row, "transactionDate"),
                    text(row, "description"),
                    decimal(row, "total"),
                    decimal(row, "principal"),
                    decimal(row, "interest"),
                    decimal(row, "escrow"),
                    decimal(row, "fees"),
                    decimal(row, "unapplied"),
                    decimal(row, "corporateAdvance"),
                    decimal(row, "other")
                );
                transaction.setReviewRequired(true);
                transaction.setPendingReview(true);
                transaction.setReviewNotes("Extraido por IA. Revisar contra el PDF original.");
                transactions.add(transaction);
            }
        }
        return new MortgageStatementParser.ParsedMortgageStatement(statement, transactions);
    }

    private JsonObject textFormat() {
        JsonObject wrapper = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "mortgage_import");
        format.addProperty("strict", true);
        format.add("schema", schema());
        wrapper.add("format", format);
        return wrapper;
    }

    private JsonObject schema() {
        JsonObject root = object();
        JsonObject properties = new JsonObject();
        properties.add("statement", statementSchema());
        JsonObject transactions = new JsonObject();
        transactions.addProperty("type", "array");
        transactions.add("items", transactionSchema());
        properties.add("transactions", transactions);
        root.add("properties", properties);
        root.add("required", array("statement", "transactions"));
        root.addProperty("additionalProperties", false);
        return root;
    }

    private JsonObject statementSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("servicerName", string());
        properties.add("statementDate", nullableString());
        properties.add("paymentDueDate", nullableString());
        properties.add("paymentAmountDue", number());
        properties.add("lateFeeDate", nullableString());
        properties.add("lateFeeAmount", number());
        properties.add("loanNumber", string());
        properties.add("propertyAddress", string());
        properties.add("originalPrincipalBalance", number());
        properties.add("outstandingPrincipalBalance", number());
        properties.add("maturityDate", nullableString());
        properties.add("interestRate", number());
        properties.add("escrowBalance", number());
        properties.add("unappliedFunds", number());
        properties.add("currentPaymentDue", number());
        properties.add("principalDue", number());
        properties.add("interestDue", number());
        properties.add("escrowDue", number());
        properties.add("regularMonthlyPayment", number());
        properties.add("pastDueAmount", number());
        properties.add("fees", number());
        properties.add("otherFeesAndCharges", number());
        properties.add("totalDue", number());
        properties.add("pastPaidPrincipalSinceLastStatement", number());
        properties.add("pastPaidPrincipalYearToDate", number());
        properties.add("pastPaidInterestSinceLastStatement", number());
        properties.add("pastPaidInterestYearToDate", number());
        properties.add("pastPaidEscrowSinceLastStatement", number());
        properties.add("pastPaidEscrowYearToDate", number());
        properties.add("pastPaidTotalSinceLastStatement", number());
        properties.add("pastPaidTotalYearToDate", number());
        schema.add("properties", properties);
        schema.add("required", array(
            "servicerName", "statementDate", "paymentDueDate", "paymentAmountDue", "lateFeeDate", "lateFeeAmount",
            "loanNumber", "propertyAddress", "originalPrincipalBalance", "outstandingPrincipalBalance", "maturityDate",
            "interestRate", "escrowBalance", "unappliedFunds", "currentPaymentDue", "principalDue", "interestDue",
            "escrowDue", "regularMonthlyPayment", "pastDueAmount", "fees", "otherFeesAndCharges", "totalDue",
            "pastPaidPrincipalSinceLastStatement", "pastPaidPrincipalYearToDate", "pastPaidInterestSinceLastStatement",
            "pastPaidInterestYearToDate", "pastPaidEscrowSinceLastStatement", "pastPaidEscrowYearToDate",
            "pastPaidTotalSinceLastStatement", "pastPaidTotalYearToDate"
        ));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject transactionSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("transactionDate", nullableString());
        properties.add("description", string());
        properties.add("total", number());
        properties.add("principal", number());
        properties.add("interest", number());
        properties.add("escrow", number());
        properties.add("fees", number());
        properties.add("unapplied", number());
        properties.add("corporateAdvance", number());
        properties.add("other", number());
        schema.add("properties", properties);
        schema.add("required", array("transactionDate", "description", "total", "principal", "interest", "escrow", "fees", "unapplied", "corporateAdvance", "other"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private String instructions(RuntimeException parserFailure) {
        return """
            Extract one mortgage statement from the PDF text.
            Return only fields in the requested JSON schema.
            Dates must be ISO yyyy-MM-dd when present, otherwise null.
            Money values must be numbers without currency symbols.
            Interest rate must be a number, e.g. 6.875 for 6.875%%.
            If a numeric field is not present, return 0.
            Transactions should come from payment/transaction activity only.
            This result will be manually reviewed before accounting use.
            Parser failure context: %s
            """.formatted(parserFailure == null ? "" : parserFailure.getMessage());
    }

    private JsonObject object() {
        JsonObject object = new JsonObject();
        object.addProperty("type", "object");
        return object;
    }

    private JsonObject string() {
        JsonObject string = new JsonObject();
        string.addProperty("type", "string");
        return string;
    }

    private JsonObject nullableString() {
        JsonObject value = new JsonObject();
        JsonArray types = new JsonArray();
        types.add("string");
        types.add("null");
        value.add("type", types);
        return value;
    }

    private JsonObject number() {
        JsonObject number = new JsonObject();
        number.addProperty("type", "number");
        return number;
    }

    private JsonArray array(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private LocalDate date(JsonObject object, String field) {
        String value = text(object, field);
        if (value.isBlank()) {
            return null;
        }
        if (value.contains("/")) {
            return LocalDate.parse(value, SHORT_DATE);
        }
        return LocalDate.parse(value);
    }

    private double decimal(JsonObject object, String field) {
        JsonElement element = object == null ? null : object.get(field);
        return element == null || element.isJsonNull() ? 0 : element.getAsDouble();
    }

    private String text(JsonObject object, String field) {
        JsonElement element = object == null ? null : object.get(field);
        return element == null || element.isJsonNull() ? "" : element.getAsString().trim();
    }

    private String env(String name) {
        String value = System.getenv(name);
        return value == null ? "" : value.trim();
    }

    private String model() {
        String configured = env("OPENAI_MODEL");
        return configured.isBlank() ? DEFAULT_MODEL : configured;
    }
}
