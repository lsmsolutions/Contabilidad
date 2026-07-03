package com.silveira.accounting.parsers.bank;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.silveira.accounting.application.bank.dto.BankImportData;
import com.silveira.accounting.application.importing.AiDocumentImportGateway;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.parsers.ProviderDetector;
import com.silveira.accounting.utils.Fingerprint;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OpenAiBankAiImportGateway implements AiDocumentImportGateway<BankImportData> {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final PdfTextExtractor extractor;
    private final HttpClient httpClient;
    private final ProviderDetector providerDetector = new ProviderDetector();

    public OpenAiBankAiImportGateway() {
        this(new PdfTextExtractor(), HttpClient.newHttpClient());
    }

    OpenAiBankAiImportGateway(PdfTextExtractor extractor, HttpClient httpClient) {
        this.extractor = extractor;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<BankImportData> importPdf(Path pdf, RuntimeException parserFailure) {
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

    private BankImportData map(JsonObject root, Path pdf) {
        String sourcePdf = pdf.getFileName().toString();
        JsonObject periodJson = root.getAsJsonObject("period");
        String accountAlias = valueOrDefault(text(periodJson, "accountAlias"), "sin_cuenta");
        Optional<BankStatementPeriod> period = Optional.empty();
        LocalDate periodStart = date(periodJson, "periodStart");
        LocalDate periodEnd = date(periodJson, "periodEnd");
        if (periodStart != null && periodEnd != null) {
            period = Optional.of(new BankStatementPeriod(
                accountAlias,
                sourcePdf,
                periodStart,
                periodEnd,
                decimal(periodJson, "openingBalance"),
                decimal(periodJson, "statementEndingBalance")
            ));
        }

        List<BankTransaction> transactions = new ArrayList<>();
        Map<String, Integer> occurrenceByBaseFingerprint = new HashMap<>();
        JsonArray rows = root.getAsJsonArray("transactions");
        if (rows != null) {
            for (JsonElement element : rows) {
                JsonObject row = element.getAsJsonObject();
                LocalDate date = date(row, "date");
                double amount = decimal(row, "amount");
                String description = text(row, "description");
                String movementType = valueOrDefault(text(row, "movementType"), providerDetector.movementType(description, amount));
                String provider = valueOrDefault(text(row, "provider"), providerDetector.detect(description));
                String reference = text(row, "reference");
                String rowAccountAlias = valueOrDefault(text(row, "accountAlias"), accountAlias);
                String baseFingerprint = rowAccountAlias + "|" + date + "|" + description + "|" + amount + "|" + reference;
                int occurrence = occurrenceByBaseFingerprint.merge(baseFingerprint, 1, Integer::sum);
                String fingerprint = Fingerprint.of(occurrence == 1 ? baseFingerprint : baseFingerprint + "|occurrence:" + occurrence);
                BankTransaction transaction = new BankTransaction(
                    0,
                    date,
                    description,
                    amount,
                    movementType,
                    provider,
                    reference,
                    date == null ? 0 : date.getMonthValue(),
                    date == null ? 0 : date.getYear(),
                    sourcePdf,
                    fingerprint,
                    false
                );
                transaction.setAccountAlias(rowAccountAlias);
                transaction.setImportStatus("ia_en_revision");
                transaction.setReviewRequired(true);
                transaction.setPendingReview(true);
                transaction.setReviewNotes("Extraido por IA. Revisar contra el PDF original.");
                transactions.add(transaction);
            }
        }
        return new BankImportData(transactions, period);
    }

    private JsonObject textFormat() {
        JsonObject wrapper = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "bank_import");
        format.addProperty("strict", true);
        format.add("schema", schema());
        wrapper.add("format", format);
        return wrapper;
    }

    private JsonObject schema() {
        JsonObject root = object();
        JsonObject properties = new JsonObject();
        properties.add("period", periodSchema());
        JsonObject transactions = new JsonObject();
        transactions.addProperty("type", "array");
        transactions.add("items", transactionSchema());
        properties.add("transactions", transactions);
        root.add("properties", properties);
        root.add("required", array("period", "transactions"));
        root.addProperty("additionalProperties", false);
        return root;
    }

    private JsonObject periodSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("accountAlias", string());
        properties.add("periodStart", nullableString());
        properties.add("periodEnd", nullableString());
        properties.add("openingBalance", number());
        properties.add("statementEndingBalance", number());
        schema.add("properties", properties);
        schema.add("required", array("accountAlias", "periodStart", "periodEnd", "openingBalance", "statementEndingBalance"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject transactionSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("date", nullableString());
        properties.add("description", string());
        properties.add("amount", number());
        properties.add("movementType", string());
        properties.add("provider", string());
        properties.add("reference", string());
        properties.add("accountAlias", string());
        schema.add("properties", properties);
        schema.add("required", array("date", "description", "amount", "movementType", "provider", "reference", "accountAlias"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private String instructions(RuntimeException parserFailure) {
        return """
            Extract bank statement transactions from the PDF text.
            Return only fields in the requested JSON schema.
            Dates must be ISO yyyy-MM-dd when present, otherwise null.
            Amounts must be signed numbers: deposits positive, withdrawals/fees/card purchases negative.
            Use accountAlias from the statement when clear, otherwise "sin_cuenta".
            If the statement period or balances are not present, use null dates and 0 balances in period.
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

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
