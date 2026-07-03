package com.silveira.accounting.parsers.vehiclelease;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.silveira.accounting.application.importing.AiDocumentImportGateway;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import com.silveira.accounting.parsers.PdfTextExtractor;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class OpenAiVehicleLeaseAiImportGateway implements AiDocumentImportGateway<VehicleLeaseImportData> {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final PdfTextExtractor extractor;
    private final HttpClient httpClient;

    public OpenAiVehicleLeaseAiImportGateway() {
        this(new PdfTextExtractor(), HttpClient.newHttpClient());
    }

    OpenAiVehicleLeaseAiImportGateway(PdfTextExtractor extractor, HttpClient httpClient) {
        this.extractor = extractor;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<VehicleLeaseImportData> importPdf(Path pdf, RuntimeException parserFailure) {
        String apiKey = env("OPENAI_API_KEY");
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY no esta configurada. Define la clave antes de intentar leer con IA.", parserFailure);
        }
        String text = extractor.extract(pdf);
        JsonObject response = requestExtraction(apiKey, model(), text, parserFailure);
        VehicleLeaseImportData data = map(response, pdf);
        return Optional.of(data);
    }

    private JsonObject requestExtraction(String apiKey, String model, String pdfText, RuntimeException parserFailure) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("instructions", instructions(parserFailure));
        body.addProperty("input", pdfText);
        body.add("text", textFormat());
        body.addProperty("max_output_tokens", 1800);

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

    private VehicleLeaseImportData map(JsonObject root, Path pdf) {
        JsonObject accountJson = root.getAsJsonObject("account");
        JsonObject statementJson = root.getAsJsonObject("statement");
        VehicleLeaseAccount account = new VehicleLeaseAccount();
        account.setProviderName(valueOrDefault(text(accountJson, "providerName"), "Vehicle Lease Provider"));
        account.setVehicleYear(integer(accountJson, "vehicleYear"));
        account.setMake(valueOrDefault(text(accountJson, "make"), "Volvo"));
        account.setModel(text(accountJson, "model"));
        account.setTrim(text(accountJson, "trim"));
        account.setAccountNumber(text(accountJson, "accountNumber"));
        account.setVin(text(accountJson, "vin"));
        account.setMaturityDate(date(accountJson, "maturityDate"));
        account.setAlias(defaultAlias(account));
        account.setNotes("Extraido por IA. Revisar contra PDF original.");

        VehicleLeaseStatement statement = new VehicleLeaseStatement();
        statement.setAccountAlias(account.getAlias());
        statement.setStatementDate(date(statementJson, "statementDate"));
        statement.setDueDate(date(statementJson, "dueDate"));
        statement.setTotalAmountDue(decimal(statementJson, "totalAmountDue"));
        statement.setLastPaymentDate(date(statementJson, "lastPaymentDate"));
        statement.setLastPaymentAmount(decimal(statementJson, "lastPaymentAmount"));
        statement.setPaymentsMade(integer(statementJson, "paymentsMade"));
        statement.setPaymentsRemaining(integer(statementJson, "paymentsRemaining"));
        statement.setLeasePayment(decimal(statementJson, "leasePayment"));
        statement.setSalesUseTax(decimal(statementJson, "salesUseTax"));
        statement.setPropertyTax(decimal(statementJson, "propertyTax"));
        statement.setParkingTickets(decimal(statementJson, "parkingTickets"));
        statement.setReturnedCheckFees(decimal(statementJson, "returnedCheckFees"));
        statement.setMiscellaneousCharges(decimal(statementJson, "miscellaneousCharges"));
        statement.setPastDueAmount(decimal(statementJson, "pastDueAmount"));
        statement.setLateCharges(decimal(statementJson, "lateCharges"));
        statement.setSourcePdfPath(pdf.toAbsolutePath().toString());
        statement.setReviewRequired(true);
        statement.setPendingReview(true);
        statement.setReviewNotes("Extraido por IA. Revisar contra el PDF original antes de guardar como revisado.");
        return new VehicleLeaseImportData(account, statement);
    }

    private JsonObject textFormat() {
        JsonObject wrapper = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "vehicle_lease_import");
        format.addProperty("strict", true);
        format.add("schema", schema());
        wrapper.add("format", format);
        return wrapper;
    }

    private JsonObject schema() {
        JsonObject root = object();
        JsonObject properties = new JsonObject();
        properties.add("account", accountSchema());
        properties.add("statement", statementSchema());
        root.add("properties", properties);
        root.add("required", array("account", "statement"));
        root.addProperty("additionalProperties", false);
        return root;
    }

    private JsonObject accountSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("providerName", string());
        properties.add("vehicleYear", integerSchema());
        properties.add("make", string());
        properties.add("model", string());
        properties.add("trim", string());
        properties.add("accountNumber", string());
        properties.add("vin", string());
        properties.add("maturityDate", nullableString());
        schema.add("properties", properties);
        schema.add("required", array("providerName", "vehicleYear", "make", "model", "trim", "accountNumber", "vin", "maturityDate"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject statementSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("statementDate", nullableString());
        properties.add("dueDate", nullableString());
        properties.add("totalAmountDue", number());
        properties.add("lastPaymentDate", nullableString());
        properties.add("lastPaymentAmount", number());
        properties.add("paymentsMade", integerSchema());
        properties.add("paymentsRemaining", integerSchema());
        properties.add("leasePayment", number());
        properties.add("salesUseTax", number());
        properties.add("propertyTax", number());
        properties.add("parkingTickets", number());
        properties.add("returnedCheckFees", number());
        properties.add("miscellaneousCharges", number());
        properties.add("pastDueAmount", number());
        properties.add("lateCharges", number());
        schema.add("properties", properties);
        schema.add("required", array(
            "statementDate",
            "dueDate",
            "totalAmountDue",
            "lastPaymentDate",
            "lastPaymentAmount",
            "paymentsMade",
            "paymentsRemaining",
            "leasePayment",
            "salesUseTax",
            "propertyTax",
            "parkingTickets",
            "returnedCheckFees",
            "miscellaneousCharges",
            "pastDueAmount",
            "lateCharges"
        ));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private String instructions(RuntimeException parserFailure) {
        return """
            Extract one vehicle lease statement from the PDF text.
            Return only fields in the requested JSON schema.
            Dates must be ISO yyyy-MM-dd when present, otherwise null.
            Money values must be numbers without currency symbols.
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

    private JsonObject integerSchema() {
        JsonObject integer = new JsonObject();
        integer.addProperty("type", "integer");
        return integer;
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

    private int integer(JsonObject object, String field) {
        JsonElement element = object == null ? null : object.get(field);
        return element == null || element.isJsonNull() ? 0 : element.getAsInt();
    }

    private String text(JsonObject object, String field) {
        JsonElement element = object == null ? null : object.get(field);
        return element == null || element.isJsonNull() ? "" : element.getAsString().trim();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String defaultAlias(VehicleLeaseAccount account) {
        String make = account.getMake() == null || account.getMake().isBlank() ? "Vehicle" : account.getMake().trim();
        String model = account.getModel() == null || account.getModel().isBlank() ? "Lease" : account.getModel().trim();
        return (make + " " + model).trim();
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
