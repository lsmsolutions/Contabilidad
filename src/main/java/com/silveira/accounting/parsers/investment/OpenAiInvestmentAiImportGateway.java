package com.silveira.accounting.parsers.investment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.silveira.accounting.application.importing.AiDocumentImportGateway;
import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentAllocation;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OpenAiInvestmentAiImportGateway implements AiDocumentImportGateway<InvestmentImportData> {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final PdfTextExtractor extractor;
    private final HttpClient httpClient;

    public OpenAiInvestmentAiImportGateway() {
        this(new PdfTextExtractor(), HttpClient.newHttpClient());
    }

    OpenAiInvestmentAiImportGateway(PdfTextExtractor extractor, HttpClient httpClient) {
        this.extractor = extractor;
        this.httpClient = httpClient;
    }

    @Override
    public Optional<InvestmentImportData> importPdf(Path pdf, RuntimeException parserFailure) {
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
        body.addProperty("max_output_tokens", 6000);
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

    private InvestmentImportData map(JsonObject root, Path pdf) {
        JsonObject accountJson = root.getAsJsonObject("account");
        InvestmentAccount account = new InvestmentAccount();
        account.setAlias(valueOrDefault(text(accountJson, "alias"), defaultAlias(accountJson)));
        account.setProviderName(valueOrDefault(text(accountJson, "providerName"), "Charles Schwab"));
        account.setAccountType(valueOrDefault(text(accountJson, "accountType"), "Brokerage"));
        account.setAccountNumber(text(accountJson, "accountNumber"));
        account.setNotes("Extraido por IA. Revisar contra PDF original.");

        JsonObject statementJson = root.getAsJsonObject("statement");
        InvestmentStatement statement = new InvestmentStatement();
        statement.setAccountAlias(account.getAlias());
        statement.setPeriodStart(date(statementJson, "periodStart"));
        statement.setPeriodEnd(date(statementJson, "periodEnd"));
        statement.setBeginningValue(decimal(statementJson, "beginningValue"));
        statement.setEndingValue(decimal(statementJson, "endingValue"));
        statement.setTransferOfSecurities(decimal(statementJson, "transferOfSecurities"));
        statement.setDividendsReinvested(decimal(statementJson, "dividendsReinvested"));
        statement.setCashActivity(decimal(statementJson, "cashActivity"));
        statement.setChangeInMarketValue(decimal(statementJson, "changeInMarketValue"));
        statement.setDeposits(decimal(statementJson, "deposits"));
        statement.setWithdrawals(decimal(statementJson, "withdrawals"));
        statement.setDividendsInterest(decimal(statementJson, "dividendsInterest"));
        statement.setMarketChange(decimal(statementJson, "marketChange"));
        statement.setExpenses(decimal(statementJson, "expenses"));
        statement.setCostBasisTotal(decimal(statementJson, "costBasisTotal"));
        statement.setUnrealizedGainLoss(decimal(statementJson, "unrealizedGainLoss"));
        statement.setSourcePdfPath(pdf.toAbsolutePath().toString());

        return new InvestmentImportData(
            account,
            statement,
            allocations(root.getAsJsonArray("allocations")),
            positions(root.getAsJsonArray("positions")),
            transactions(root.getAsJsonArray("transactions"))
        );
    }

    private List<InvestmentAllocation> allocations(JsonArray rows) {
        List<InvestmentAllocation> values = new ArrayList<>();
        if (rows == null) {
            return values;
        }
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            InvestmentAllocation value = new InvestmentAllocation();
            value.setCategory(text(row, "category"));
            value.setMarketValue(decimal(row, "marketValue"));
            value.setPercentage(decimal(row, "percentage"));
            values.add(value);
        }
        return values;
    }

    private List<InvestmentPosition> positions(JsonArray rows) {
        List<InvestmentPosition> values = new ArrayList<>();
        if (rows == null) {
            return values;
        }
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            InvestmentPosition value = new InvestmentPosition();
            value.setSymbol(text(row, "symbol"));
            value.setDescription(text(row, "description"));
            value.setAssetType(text(row, "assetType"));
            value.setQuantity(decimal(row, "quantity"));
            value.setPrice(decimal(row, "price"));
            value.setMarketValue(decimal(row, "marketValue"));
            value.setCostBasis(decimal(row, "costBasis"));
            value.setUnrealizedGainLoss(decimal(row, "unrealizedGainLoss"));
            values.add(value);
        }
        return values;
    }

    private List<InvestmentTransaction> transactions(JsonArray rows) {
        List<InvestmentTransaction> values = new ArrayList<>();
        if (rows == null) {
            return values;
        }
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            InvestmentTransaction value = new InvestmentTransaction();
            value.setTransactionDate(date(row, "transactionDate"));
            value.setCategory(text(row, "category"));
            value.setAction(text(row, "action"));
            value.setSymbol(text(row, "symbol"));
            value.setDescription(text(row, "description"));
            value.setQuantity(decimal(row, "quantity"));
            value.setPrice(decimal(row, "price"));
            value.setAmount(decimal(row, "amount"));
            value.setRealizedGainLoss(decimal(row, "realizedGainLoss"));
            values.add(value);
        }
        return values;
    }

    private JsonObject textFormat() {
        JsonObject wrapper = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.addProperty("name", "investment_import");
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
        properties.add("allocations", arrayOf(allocationSchema()));
        properties.add("positions", arrayOf(positionSchema()));
        properties.add("transactions", arrayOf(transactionSchema()));
        root.add("properties", properties);
        root.add("required", array("account", "statement", "allocations", "positions", "transactions"));
        root.addProperty("additionalProperties", false);
        return root;
    }

    private JsonObject accountSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("alias", string());
        properties.add("providerName", string());
        properties.add("accountType", string());
        properties.add("accountNumber", string());
        schema.add("properties", properties);
        schema.add("required", array("alias", "providerName", "accountType", "accountNumber"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject statementSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("periodStart", nullableString());
        properties.add("periodEnd", nullableString());
        for (String field : List.of("beginningValue", "endingValue", "transferOfSecurities", "dividendsReinvested", "cashActivity", "changeInMarketValue", "deposits", "withdrawals", "dividendsInterest", "marketChange", "expenses", "costBasisTotal", "unrealizedGainLoss")) {
            properties.add(field, number());
        }
        schema.add("properties", properties);
        schema.add("required", array("periodStart", "periodEnd", "beginningValue", "endingValue", "transferOfSecurities", "dividendsReinvested", "cashActivity", "changeInMarketValue", "deposits", "withdrawals", "dividendsInterest", "marketChange", "expenses", "costBasisTotal", "unrealizedGainLoss"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject allocationSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("category", string());
        properties.add("marketValue", number());
        properties.add("percentage", number());
        schema.add("properties", properties);
        schema.add("required", array("category", "marketValue", "percentage"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject positionSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("symbol", string());
        properties.add("description", string());
        properties.add("assetType", string());
        properties.add("quantity", number());
        properties.add("price", number());
        properties.add("marketValue", number());
        properties.add("costBasis", number());
        properties.add("unrealizedGainLoss", number());
        schema.add("properties", properties);
        schema.add("required", array("symbol", "description", "assetType", "quantity", "price", "marketValue", "costBasis", "unrealizedGainLoss"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject transactionSchema() {
        JsonObject schema = object();
        JsonObject properties = new JsonObject();
        properties.add("transactionDate", nullableString());
        properties.add("category", string());
        properties.add("action", string());
        properties.add("symbol", string());
        properties.add("description", string());
        properties.add("quantity", number());
        properties.add("price", number());
        properties.add("amount", number());
        properties.add("realizedGainLoss", number());
        schema.add("properties", properties);
        schema.add("required", array("transactionDate", "category", "action", "symbol", "description", "quantity", "price", "amount", "realizedGainLoss"));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private JsonObject arrayOf(JsonObject itemSchema) {
        JsonObject value = new JsonObject();
        value.addProperty("type", "array");
        value.add("items", itemSchema);
        return value;
    }

    private String instructions(RuntimeException parserFailure) {
        return """
            Extract one investment/brokerage statement from the PDF text.
            Return only fields in the requested JSON schema.
            Dates must be ISO yyyy-MM-dd when present, otherwise null.
            Money values must be numbers without currency symbols.
            For statement.transferOfSecurities, statement.dividendsReinvested, statement.cashActivity, statement.changeInMarketValue, use the Positions - Summary section exactly.
            Amounts for withdrawals/purchases should be negative when the statement indicates cash out.
            For investment transactions, set category to the statement section/category when available, for example Purchases, Dividends/Interest, Sales/Redemptions, Deposits, Withdrawals, Expenses/Fees, Other Activity.
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

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String defaultAlias(JsonObject accountJson) {
        String accountNumber = text(accountJson, "accountNumber");
        return accountNumber.isBlank() ? "Investment Account" : "Schwab One " + accountNumber;
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
