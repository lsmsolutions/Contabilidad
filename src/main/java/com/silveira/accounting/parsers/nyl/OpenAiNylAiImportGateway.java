package com.silveira.accounting.parsers.nyl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.silveira.accounting.application.importing.AiDocumentImportGateway;
import com.silveira.accounting.application.nyl.NylImportGateway;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.parsers.PdfTextExtractor;
import com.silveira.accounting.utils.Fingerprint;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OpenAiNylAiImportGateway implements AiDocumentImportGateway<NylImportGateway.ParsedDocument> {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";

    private final PdfTextExtractor extractor;
    private final HttpClient httpClient;

    public OpenAiNylAiImportGateway() {
        this(new PdfTextExtractor(), HttpClient.newHttpClient());
    }

    OpenAiNylAiImportGateway(PdfTextExtractor extractor, HttpClient httpClient) {
        this.extractor = extractor;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<NylImportGateway.ParsedDocument> importPdf(Path pdf, RuntimeException parserFailure) {
        String apiKey = env("OPENAI_API_KEY");
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY no esta configurada. Define la clave antes de intentar leer con IA.", parserFailure);
        }
        String text = extractor.extract(pdf);
        JsonObject extracted = requestExtraction(apiKey, model(), text, parserFailure);
        return Optional.of(new NylImportGateway.ParsedDocument(records(extracted.getAsJsonArray("records"), pdf), text));
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

    private List<NylRecord> records(JsonArray rows, Path pdf) {
        List<NylRecord> values = new ArrayList<>();
        if (rows == null) {
            return values;
        }
        String sourcePdf = pdf.getFileName().toString();
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            int year = integer(row, "year");
            int month = integer(row, "month");
            String concept = text(row, "concept");
            String section = valueOrDefault(text(row, "section"), defaultSection(text(row, "recordType")));
            String type = valueOrDefault(text(row, "recordType"), "credito");
            double amount = decimal(row, "amount");
            if ("deduccion".equals(type) && amount > 0) {
                amount = -amount;
            }
            String fingerprint = Fingerprint.of(year + "|" + month + "|" + concept + "|" + type + "|" + amount);
            values.add(new NylRecord(
                0,
                year,
                month,
                concept,
                section,
                type,
                amount,
                sourcePdf,
                fingerprint,
                "ia_en_revision",
                true,
                true,
                "Extraido por IA. Revisar contra el PDF original."
            ));
        }
        return values;
    }

    private JsonObject textFormat() {
        JsonObject wrapper = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "nyl_import");
        format.addProperty("strict", true);
        format.add("schema", schema());
        wrapper.add("format", format);
        return wrapper;
    }

    private JsonObject schema() {
        JsonObject root = object();
        JsonObject properties = new JsonObject();
        JsonObject records = new JsonObject();
        records.addProperty("type", "array");
        records.add("items", recordSchema());
        properties.add("records", records);
        root.add("properties", properties);
        root.add("required", array("records"));
        root.addProperty("additionalProperties", false);
        return root;
    }

    private JsonObject recordSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("year", integerSchema());
        properties.add("month", integerSchema());
        properties.add("concept", string());
        properties.add("section", string());
        properties.add("recordType", string());
        properties.add("amount", number());
        schema.add("properties", properties);
        schema.add("required", array("year", "month", "concept", "section", "recordType", "amount"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private String instructions(RuntimeException parserFailure) {
        return """
            Extract New York Life compensation/ledger records from the PDF text.
            Return only fields in the requested JSON schema.
            recordType must be one of: comision, credito, deduccion, withdrawal, ajuste, otro.
            Use section names such as Creditos, Deducciones, Withdrawals, Ajustes, Tax Withholding, Group Plan Contributions, Office Expenses, Technology Expense, Deferred Compensation, Other Deductions when clear.
            Deductions and withdrawals must be negative amounts.
            If a record appears by month, create one record per month.
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

    private int integer(JsonObject object, String field) {
        JsonElement element = object == null ? null : object.get(field);
        return element == null || element.isJsonNull() ? 0 : element.getAsInt();
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

    private String defaultSection(String type) {
        if ("deduccion".equals(type) || "withdrawal".equals(type)) {
            return "Deducciones";
        }
        if ("ajuste".equals(type)) {
            return "Ajustes";
        }
        return "Creditos";
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
