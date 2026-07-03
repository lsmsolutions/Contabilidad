package com.silveira.accounting.parsers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.silveira.accounting.application.importing.AiDocumentImportGateway;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
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

public class OpenAiCreditCardAiImportGateway implements AiDocumentImportGateway<CreditCardStatementParser.ParsedCreditCardStatement> {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final PdfTextExtractor extractor;
    private final HttpClient httpClient;

    public OpenAiCreditCardAiImportGateway() {
        this(new PdfTextExtractor(), HttpClient.newHttpClient());
    }

    OpenAiCreditCardAiImportGateway(PdfTextExtractor extractor, HttpClient httpClient) {
        this.extractor = extractor;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<CreditCardStatementParser.ParsedCreditCardStatement> importPdf(Path pdf, RuntimeException parserFailure) {
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
        body.addProperty("max_output_tokens", 5000);
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

    private CreditCardStatementParser.ParsedCreditCardStatement map(JsonObject root, Path pdf) {
        JsonObject statementJson = root.getAsJsonObject("statement");
        CreditCardStatement statement = new CreditCardStatement();
        statement.setBankName(text(statementJson, "bankName"));
        statement.setCardName(text(statementJson, "cardName"));
        statement.setAccountLastDigits(text(statementJson, "accountLastDigits"));
        statement.setStatementStartDate(date(statementJson, "statementStartDate"));
        statement.setStatementEndDate(date(statementJson, "statementEndDate"));
        statement.setPaymentDueDate(date(statementJson, "paymentDueDate"));
        statement.setNextClosingDate(date(statementJson, "nextClosingDate"));
        statement.setPreviousBalance(decimal(statementJson, "previousBalance"));
        statement.setPayments(decimal(statementJson, "payments"));
        statement.setOtherCredits(decimal(statementJson, "otherCredits"));
        statement.setTransactions(decimal(statementJson, "transactions"));
        statement.setBalanceTransfers(decimal(statementJson, "balanceTransfers"));
        statement.setCashAdvances(decimal(statementJson, "cashAdvances"));
        statement.setFeesCharged(decimal(statementJson, "feesCharged"));
        statement.setInterestCharged(decimal(statementJson, "interestCharged"));
        statement.setNewBalance(decimal(statementJson, "newBalance"));
        statement.setMinimumPaymentDue(decimal(statementJson, "minimumPaymentDue"));
        statement.setCreditLimit(decimal(statementJson, "creditLimit"));
        statement.setAvailableCredit(decimal(statementJson, "availableCredit"));
        statement.setCashAdvanceLimit(decimal(statementJson, "cashAdvanceLimit"));
        statement.setAvailableCashAdvanceCredit(decimal(statementJson, "availableCashAdvanceCredit"));
        statement.setRewardsBalance(decimal(statementJson, "rewardsBalance"));
        statement.setRewardsPreviousBalance(decimal(statementJson, "rewardsPreviousBalance"));
        statement.setRewardsEarned(decimal(statementJson, "rewardsEarned"));
        statement.setRewardsRedeemed(decimal(statementJson, "rewardsRedeemed"));
        statement.setSourcePdfPath(pdf.toAbsolutePath().toString());
        statement.setImportStatus("ia_en_revision");
        statement.setReviewRequired(true);
        statement.setPendingReview(true);
        statement.setReviewNotes("Extraido por IA. Revisar contra el PDF original antes de guardar como revisado.");

        List<CreditCardTransaction> transactions = new ArrayList<>();
        JsonArray rows = root.getAsJsonArray("transactions");
        if (rows != null) {
            for (JsonElement element : rows) {
                JsonObject row = element.getAsJsonObject();
                CreditCardTransaction transaction = new CreditCardTransaction(
                    0,
                    0,
                    date(row, "transactionDate"),
                    date(row, "postDate"),
                    text(row, "description"),
                    decimal(row, "amount"),
                    text(row, "type"),
                    text(row, "category")
                );
                transaction.setReviewRequired(true);
                transaction.setPendingReview(true);
                transaction.setReviewNotes("Extraido por IA. Revisar contra el PDF original.");
                transactions.add(transaction);
            }
        }
        return new CreditCardStatementParser.ParsedCreditCardStatement(statement, transactions);
    }

    private JsonObject textFormat() {
        JsonObject wrapper = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "credit_card_import");
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
        properties.add("bankName", string());
        properties.add("cardName", string());
        properties.add("accountLastDigits", string());
        properties.add("statementStartDate", nullableString());
        properties.add("statementEndDate", nullableString());
        properties.add("paymentDueDate", nullableString());
        properties.add("nextClosingDate", nullableString());
        for (String field : List.of(
            "previousBalance", "payments", "otherCredits", "transactions", "balanceTransfers", "cashAdvances",
            "feesCharged", "interestCharged", "newBalance", "minimumPaymentDue", "creditLimit", "availableCredit",
            "cashAdvanceLimit", "availableCashAdvanceCredit", "rewardsBalance", "rewardsPreviousBalance",
            "rewardsEarned", "rewardsRedeemed"
        )) {
            properties.add(field, number());
        }
        schema.add("properties", properties);
        schema.add("required", array(
            "bankName", "cardName", "accountLastDigits", "statementStartDate", "statementEndDate", "paymentDueDate",
            "nextClosingDate", "previousBalance", "payments", "otherCredits", "transactions", "balanceTransfers",
            "cashAdvances", "feesCharged", "interestCharged", "newBalance", "minimumPaymentDue", "creditLimit",
            "availableCredit", "cashAdvanceLimit", "availableCashAdvanceCredit", "rewardsBalance",
            "rewardsPreviousBalance", "rewardsEarned", "rewardsRedeemed"
        ));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject transactionSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("transactionDate", nullableString());
        properties.add("postDate", nullableString());
        properties.add("description", string());
        properties.add("amount", number());
        properties.add("type", string());
        properties.add("category", string());
        schema.add("properties", properties);
        schema.add("required", array("transactionDate", "postDate", "description", "amount", "type", "category"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private String instructions(RuntimeException parserFailure) {
        return """
            Extract one credit card statement from the PDF text.
            Return only fields in the requested JSON schema.
            Dates must be ISO yyyy-MM-dd when present, otherwise null.
            Money values must be numbers without currency symbols.
            Transaction charges should be positive amounts unless the PDF clearly marks credits/payments as negative.
            If a numeric field is not present, return 0.
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
