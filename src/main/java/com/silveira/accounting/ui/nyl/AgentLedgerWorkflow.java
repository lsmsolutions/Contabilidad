package com.silveira.accounting.ui.nyl;

import com.silveira.accounting.controllers.nyl.NylController;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.utils.Fingerprint;
import com.silveira.accounting.utils.Money;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class AgentLedgerWorkflow {
    private static final String[] SECTION_OPTIONS = {
        "Creditos",
        "Deducciones",
        "Tax Withholding",
        "Group Plan Contributions",
        "Office Expenses",
        "Technology Expense",
        "Deferred Compensation",
        "Other Deductions"
    };
    private static final List<AgentLedgerTemplate> TEMPLATES = List.of(
        AgentLedgerTemplate.header("Credits", "agent-ledger-credit-header"),
        AgentLedgerTemplate.section("Commissions"),
        AgentLedgerTemplate.input("FYC", "Creditos", "credito"),
        AgentLedgerTemplate.input("EAGLE Fees", "Creditos", "credito"),
        AgentLedgerTemplate.input("Renewals", "Creditos", "credito"),
        AgentLedgerTemplate.input("Trails/Other", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Commissions", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Expense Allowance"),
        AgentLedgerTemplate.input("Life & Annuity Expense Allowance", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Expense Allowance", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Premium Drawing"),
        AgentLedgerTemplate.input("Premium Drawing Nylic", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Premium Drawing", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Other Income"),
        AgentLedgerTemplate.input("Life & Annuity Persistency Bonus", "Creditos", "credito"),
        AgentLedgerTemplate.input("NYLAZ Override", "Creditos", "credito"),
        AgentLedgerTemplate.input("ARD", "Creditos", "credito"),
        AgentLedgerTemplate.input("Miscellaneous", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Other Income", "agent-ledger-total-row"),
        AgentLedgerTemplate.grandTotal("TOTAL CREDITS", "agent-ledger-credit-total"),
        AgentLedgerTemplate.header("Deductions", "agent-ledger-deduction-header"),
        AgentLedgerTemplate.section("Tax Withholding"),
        AgentLedgerTemplate.input("FICA - Medicare", "Tax Withholding", "deduccion"),
        AgentLedgerTemplate.input("FICA - OASDI", "Tax Withholding", "deduccion"),
        AgentLedgerTemplate.total("Total Tax Withholding", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Group Plan Contributions"),
        AgentLedgerTemplate.input("Medical", "Group Plan Contributions", "deduccion"),
        AgentLedgerTemplate.input("Dental", "Group Plan Contributions", "deduccion"),
        AgentLedgerTemplate.input("LTD", "Group Plan Contributions", "deduccion"),
        AgentLedgerTemplate.total("Total Group Plan Contributions", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Office Expenses"),
        AgentLedgerTemplate.input("Rent", "Office Expenses", "deduccion"),
        AgentLedgerTemplate.input("Telephone - Equipment", "Office Expenses", "deduccion"),
        AgentLedgerTemplate.total("Total Office Expenses", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Technology Expenses"),
        AgentLedgerTemplate.input("FT Support", "Technology Expenses", "deduccion"),
        AgentLedgerTemplate.total("Total Technology Expenses", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Deferred Compensation"),
        AgentLedgerTemplate.input("401K Contributions", "Deferred Compensation", "deduccion"),
        AgentLedgerTemplate.total("Total Deferred Compensation", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Other Deductions"),
        AgentLedgerTemplate.input("NYL-A-PLAN", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("NYLIFE ADM LTC Deduction", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("Navigator Membership", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("Lead Generation Campaigns", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("401K Loan Repayment", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("Miscellaneous", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.total("Total Other Deductions", "agent-ledger-total-row"),
        AgentLedgerTemplate.grandTotal("TOTAL DEDUCTIONS", "agent-ledger-deduction-total"),
        AgentLedgerTemplate.header("Withdrawals", "agent-ledger-withdrawal-header"),
        AgentLedgerTemplate.input("Jan", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Feb", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Mar", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Apr", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("May", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Jun", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Jul", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Aug", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Sep", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Oct", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Nov", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Dec", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.grandTotal("TOTAL WITHDRAWALS", "agent-ledger-withdrawal-total"),
        AgentLedgerTemplate.grandTotal("NET BALANCE (Credits - Deductions - Withdrawals)", "agent-ledger-net-total")
    );

    private final NylController controller;
    private final Config config;

    public AgentLedgerWorkflow(NylController controller, Config config) {
        this.controller = controller;
        this.config = config;
    }

    public void showAgentLedger() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        config.setSelectedYearValue().accept(year);
        TableView<AgentLedgerRow> table = agentLedgerTable(year);
        ComboBox<Integer> yearFilter = new ComboBox<>(FXCollections.observableArrayList(2022, 2023, 2024, 2025, 2026, LocalDate.now().getYear()));
        yearFilter.setValue(year);
        yearFilter.getStyleClass().add("compact-combo");
        Button apply = new Button("Aplicar año");
        apply.getStyleClass().add("primary");
        apply.setOnAction(event -> {
            config.setSelectedYearValue().accept(yearFilter.getValue());
            showAgentLedger();
        });
        Button add = new Button("Añadir registro");
        add.setOnAction(event -> showAddDialog(yearFilter.getValue() == null ? LocalDate.now().getYear() : yearFilter.getValue()));
        HBox actions = new HBox(10, new Label("Año"), yearFilter, apply, add);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(table, Priority.ALWAYS);
        config.setPage().accept(page(
            "Agent Ledger",
            config.backButton().create("Volver a New York Life", config.showNylHub()),
            actions,
            table
        ));
    }

    private TableView<AgentLedgerRow> agentLedgerTable(int year) {
        TableView<AgentLedgerRow> table = new TableView<>(FXCollections.observableArrayList(agentLedgerRows(year)));
        table.setEditable(true);
        table.getStyleClass().add("agent-ledger-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(view -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(AgentLedgerRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("agent-ledger-"));
                if (!empty && item != null && item.template().styleClass() != null) {
                    getStyleClass().add(item.template().styleClass());
                }
            }
        });
        TableColumn<AgentLedgerRow, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().categoryText()));
        category.setPrefWidth(190);
        TableColumn<AgentLedgerRow, String> concept = new TableColumn<>("Sub-Category");
        concept.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().conceptText()));
        concept.setPrefWidth(270);
        table.getColumns().add(category);
        table.getColumns().add(concept);
        TableColumn<AgentLedgerRow, String> firstMonthColumn = null;
        for (int month = 1; month <= 12; month++) {
            final int monthValue = month;
            TableColumn<AgentLedgerRow, String> monthColumn = new TableColumn<>(agentLedgerMonthName(month));
            monthColumn.setCellValueFactory(data -> new SimpleStringProperty(agentLedgerCellText(data.getValue(), monthValue)));
            monthColumn.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
            monthColumn.setOnEditCommit(event -> updateAgentLedgerCell(event.getRowValue(), year, monthValue, event.getNewValue()));
            monthColumn.setPrefWidth(104);
            table.getColumns().add(monthColumn);
            if (firstMonthColumn == null) {
                firstMonthColumn = monthColumn;
            }
        }
        TableColumn<AgentLedgerRow, String> ytd = new TableColumn<>("YTD Total");
        ytd.setCellValueFactory(data -> new SimpleStringProperty(agentLedgerYtdText(data.getValue())));
        ytd.setPrefWidth(120);
        table.getColumns().add(ytd);
        TableColumn<AgentLedgerRow, Void> actions = new TableColumn<>("Acciones");
        TableColumn<AgentLedgerRow, String> editableColumn = firstMonthColumn;
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button edit = new Button("Editar");
            private final Button save = new Button("Guardar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, edit, save, delete);
            {
                edit.setOnAction(event -> {
                    AgentLedgerRow row = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(row);
                    getTableView().scrollTo(row);
                    getTableView().requestFocus();
                    if (editableColumn != null) {
                        getTableView().edit(getIndex(), editableColumn);
                    }
                });
                save.setOnAction(event -> {
                    AgentLedgerRow row = getTableView().getItems().get(getIndex());
                    saveAgentLedgerRow(row);
                    getTableView().refresh();
                });
                delete.setOnAction(event -> deleteAgentLedgerRow(getTableView().getItems().get(getIndex()), year));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                AgentLedgerRow row = empty ? null : getTableView().getItems().get(getIndex());
                setGraphic(row != null && row.template().editable() ? buttons : null);
            }
        });
        actions.setPrefWidth(230);
        table.getColumns().add(actions);
        return table;
    }

    private List<AgentLedgerRow> agentLedgerRows(int year) {
        List<AgentLedgerRow> rows = TEMPLATES.stream().map(AgentLedgerRow::new).toList();
        Map<String, AgentLedgerRow> byKey = rows.stream()
            .filter(row -> row.template().editable())
            .collect(Collectors.toMap(row -> agentLedgerKey(row.template()), row -> row, (left, right) -> left, LinkedHashMap::new));
        Map<String, AgentLedgerRow> byConceptAndType = rows.stream()
            .filter(row -> row.template().editable())
            .collect(Collectors.toMap(row -> agentLedgerConceptTypeKey(row.template().label(), row.template().type()), row -> row, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<AgentLedgerRow>> byConcept = rows.stream()
            .filter(row -> row.template().editable())
            .collect(Collectors.groupingBy(row -> normalizeAgentLedgerToken(row.template().label()), LinkedHashMap::new, Collectors.toList()));
        for (NylRecord record : controller.find(year, null, null, null)) {
            AgentLedgerRow row = byKey.get(agentLedgerKey(record.getSection(), record.getConcept(), record.getRecordType()));
            if (row == null) {
                row = byConceptAndType.get(agentLedgerConceptTypeKey(record.getConcept(), record.getRecordType()));
            }
            if (row == null) {
                List<AgentLedgerRow> conceptMatches = byConcept.get(normalizeAgentLedgerToken(record.getConcept()));
                if (conceptMatches != null && conceptMatches.size() == 1) {
                    row = conceptMatches.get(0);
                }
            }
            if (row != null) {
                row.records(record.getMonth()).add(record);
            }
        }
        recalculateAgentLedgerRows(rows, year);
        return rows;
    }

    private void recalculateAgentLedgerRows(List<AgentLedgerRow> rows, int year) {
        double[] sectionTotals = new double[13];
        double[] creditTotals = new double[13];
        double[] deductionTotals = new double[13];
        double[] withdrawalTotals = new double[13];
        for (AgentLedgerRow row : rows) {
            AgentLedgerTemplate template = row.template();
            if ("SECTION".equals(template.kind())) {
                sectionTotals = new double[13];
            } else if ("INPUT".equals(template.kind())) {
                for (int month = 1; month <= 12; month++) {
                    double value = agentLedgerInputValue(row, year, month);
                    row.setDisplayValue(month, value);
                    sectionTotals[month] += value;
                    if ("deduccion".equals(template.type())) {
                        deductionTotals[month] += value;
                    } else if ("withdrawal".equals(template.type())) {
                        withdrawalTotals[month] += value;
                    } else {
                        creditTotals[month] += value;
                    }
                }
            } else if ("TOTAL".equals(template.kind())) {
                for (int month = 1; month <= 12; month++) row.setDisplayValue(month, sectionTotals[month]);
                sectionTotals = new double[13];
            } else if (template.label().equals("TOTAL CREDITS")) {
                for (int month = 1; month <= 12; month++) row.setDisplayValue(month, creditTotals[month]);
            } else if (template.label().equals("TOTAL DEDUCTIONS")) {
                for (int month = 1; month <= 12; month++) row.setDisplayValue(month, deductionTotals[month]);
            } else if (template.label().equals("TOTAL WITHDRAWALS")) {
                for (int month = 1; month <= 12; month++) row.setDisplayValue(month, withdrawalTotals[month]);
            } else if (template.label().startsWith("NET BALANCE")) {
                for (int month = 1; month <= 12; month++) {
                    row.setDisplayValue(month, creditTotals[month] - deductionTotals[month] - withdrawalTotals[month]);
                }
            }
        }
    }

    private double agentLedgerInputValue(AgentLedgerRow row, int year, int month) {
        double raw = row.rawValue(month);
        if (!"withdrawal".equals(row.template().type())) {
            return Math.abs(raw);
        }
        int rowMonth = agentLedgerMonthNumber(row.template().label());
        if (rowMonth != month) {
            return 0;
        }
        if (Math.abs(raw) >= 0.005) {
            return Math.abs(raw);
        }
        return controller.findPdfResult(year, month).map(Math::abs).orElse(0.0);
    }

    private String agentLedgerCellText(AgentLedgerRow row, int month) {
        if (!row.template().hasValues()) return "";
        double value = row.displayValue(month);
        return Math.abs(value) < 0.005 ? "-" : String.format(java.util.Locale.US, "%.2f", value);
    }

    private String agentLedgerYtdText(AgentLedgerRow row) {
        if (!row.template().hasValues()) return "";
        double total = 0;
        for (int month = 1; month <= 12; month++) total += row.displayValue(month);
        return Math.abs(total) < 0.005 ? "-" : String.format(java.util.Locale.US, "%.2f", total);
    }

    private void updateAgentLedgerCell(AgentLedgerRow row, int year, int month, String rawValue) {
        if (row == null || !row.template().editable()) return;
        String value = rawValue == null ? "" : rawValue.trim();
        List<NylRecord> records = new ArrayList<>(row.records(month));
        if (value.isBlank() || "-".equals(value)) {
            if (!records.isEmpty() && config.confirm().confirm(
                "Eliminar registro",
                "Se eliminaran los registros de " + row.template().label() + " en " + monthName(month) + " " + year + ".",
                "Eliminar"
            )) {
                records.stream().filter(record -> record.getId() > 0).forEach(record -> controller.delete(record.getId()));
            }
            showAgentLedger();
            return;
        }
        double amount = normalizeAgentLedgerAmount(row.template(), Money.parse(value));
        if (records.isEmpty()) {
            String fingerprint = Fingerprint.of(year + "|" + month + "|" + row.template().label() + "|" + row.template().type() + "|" + amount + "|agent-ledger");
            NylRecord record = new NylRecord(0, year, month, row.template().label(), row.template().section(), row.template().type(), amount, "agent_ledger", fingerprint, "manual", false, false, "Agent Ledger");
            controller.saveAll(List.of(record));
        } else {
            double current = records.stream().mapToDouble(NylRecord::getAmount).sum();
            NylRecord first = records.get(0);
            first.setAmount(first.getAmount() + (amount - current));
            first.setSection(row.template().section());
            first.setRecordType(row.template().type());
            controller.updateRecord(first);
        }
        showAgentLedger();
    }

    private void deleteAgentLedgerRow(AgentLedgerRow row, int year) {
        List<NylRecord> records = new ArrayList<>();
        for (int month = 1; month <= 12; month++) records.addAll(row.records(month));
        if (records.isEmpty()) {
            config.alert().show(Alert.AlertType.INFORMATION, "Agent Ledger", "No hay registros para eliminar en esta fila.");
            return;
        }
        if (!config.confirm().confirm(
            "Eliminar fila",
            "Se eliminaran " + records.size() + " registros de '" + row.template().label() + "' en " + year + ".",
            "Eliminar"
        )) return;
        records.stream().filter(record -> record.getId() > 0).forEach(record -> controller.delete(record.getId()));
        showAgentLedger();
    }

    private void saveAgentLedgerRow(AgentLedgerRow row) {
        if (row == null || !row.template().editable()) return;
        for (int month = 1; month <= 12; month++) {
            for (NylRecord record : row.records(month)) {
                record.setSection(row.template().section());
                record.setRecordType(row.template().type());
                record.setAmount(normalizeAgentLedgerAmount(row.template(), record.getAmount()));
                if (record.getId() > 0) controller.updateRecord(record);
            }
        }
    }

    private void showAddDialog(int year) {
        DatePicker date = new DatePicker(LocalDate.of(year, Math.max(1, selectedMonth() == null ? 1 : selectedMonth()), 1));
        TextField concept = new TextField();
        concept.setPromptText("Concepto");
        ComboBox<String> section = new ComboBox<>(FXCollections.observableArrayList(SECTION_OPTIONS));
        section.setValue("Creditos");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("comision", "credito", "deduccion", "withdrawal", "ajuste", "otro"));
        type.setValue("credito");
        TextField amount = new TextField();
        amount.setPromptText("Importe");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Fecha"), date);
        form.addRow(1, new Label("Concepto"), concept);
        form.addRow(2, new Label("Apartado"), section);
        form.addRow(3, new Label("Tipo"), type);
        form.addRow(4, new Label("Importe"), amount);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Añadir registro Agent Ledger");
        dialog.setHeaderText("Añadir registro Agent Ledger");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            LocalDate value = date.getValue();
            AgentLedgerTemplate template = new AgentLedgerTemplate("INPUT", concept.getText(), section.getValue(), type.getValue(), "", true, true);
            double parsedAmount = normalizeAgentLedgerAmount(template, Money.parse(amount.getText()));
            String fingerprint = Fingerprint.of(value.getYear() + "|" + value.getMonthValue() + "|" + concept.getText() + "|" + type.getValue() + "|" + parsedAmount + "|agent-ledger-manual");
            NylRecord record = new NylRecord(0, value.getYear(), value.getMonthValue(), concept.getText(), section.getValue(), type.getValue(), parsedAmount, "agent_ledger", fingerprint, "manual", false, false, "Agent Ledger");
            controller.saveAll(List.of(record));
            config.setSelectedYearValue().accept(value.getYear());
            config.setSelectedMonthValue().accept(value.getMonthValue());
            showAgentLedger();
        });
    }

    private double normalizeAgentLedgerAmount(AgentLedgerTemplate template, double amount) {
        return "deduccion".equalsIgnoreCase(template.type()) || "withdrawal".equalsIgnoreCase(template.type())
            ? -Math.abs(amount)
            : Math.abs(amount);
    }

    private String agentLedgerKey(AgentLedgerTemplate template) {
        return agentLedgerKey(template.section(), template.label(), template.type());
    }

    private String agentLedgerKey(String section, String concept, String type) {
        return normalizeAgentLedgerToken(section) + "|" + normalizeAgentLedgerToken(concept) + "|" + normalizeAgentLedgerToken(type);
    }

    private String agentLedgerConceptTypeKey(String concept, String type) {
        return normalizeAgentLedgerToken(concept) + "|" + normalizeAgentLedgerToken(type);
    }

    private String normalizeAgentLedgerToken(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase();
        return switch (normalized) {
            case "lifeannuitypersistencybonus" -> "lifeannuitypersistencybonus";
            case "telephone" -> "telephoneequipment";
            case "nylifeadmltc" -> "nylifeadmltcdeduction";
            case "401kloadreapyment" -> "401kloanrepayment";
            case "technologyexpense" -> "technologyexpenses";
            default -> normalized;
        };
    }

    private String agentLedgerMonthName(int month) {
        return switch (month) {
            case 1 -> "Jan";
            case 2 -> "Feb";
            case 3 -> "Mar";
            case 4 -> "Apr";
            case 5 -> "May";
            case 6 -> "Jun";
            case 7 -> "Jul";
            case 8 -> "Aug";
            case 9 -> "Sep";
            case 10 -> "Oct";
            case 11 -> "Nov";
            case 12 -> "Dec";
            default -> "";
        };
    }

    private int agentLedgerMonthNumber(String label) {
        return switch (normalizeAgentLedgerToken(label)) {
            case "jan" -> 1;
            case "feb" -> 2;
            case "mar" -> 3;
            case "apr" -> 4;
            case "may" -> 5;
            case "jun" -> 6;
            case "jul" -> 7;
            case "aug" -> 8;
            case "sep" -> 9;
            case "oct" -> 10;
            case "nov" -> 11;
            case "dec" -> 12;
            default -> 0;
        };
    }

    private String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> "Mes " + month;
        };
    }

    private VBox page(String title, Node... nodes) {
        Label heading = new Label(title);
        heading.getStyleClass().add("heading");
        VBox box = new VBox(18);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("page");
        return box;
    }

    private StringConverter<String> stringConverter() {
        return new StringConverter<>() {
            @Override public String toString(String value) { return value == null ? "" : value; }
            @Override public String fromString(String value) { return value == null ? "" : value; }
        };
    }

    private <S, T> javafx.util.Callback<TableColumn<S, T>, TableCell<S, T>> commitOnFocusLostCellFactory(StringConverter<T> converter) {
        return column -> new TableCell<>() {
            private TextField textField;

            @Override
            public void startEdit() {
                if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
                super.startEdit();
                if (textField == null) {
                    textField = new TextField();
                    textField.setOnAction(event -> commitCurrentEdit());
                    textField.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ESCAPE) cancelEdit();
                    });
                    textField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                        if (wasFocused && !isFocused && isEditing()) commitCurrentEdit();
                    });
                }
                textField.setText(converter.toString(getItem()));
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(converter.toString(getItem()));
                setGraphic(null);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    if (textField != null) textField.setText(converter.toString(item));
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(converter.toString(item));
                    setGraphic(null);
                }
            }

            private void commitCurrentEdit() {
                try {
                    commitEdit(converter.fromString(textField.getText()));
                } catch (RuntimeException exception) {
                    cancelEdit();
                    config.alert().show(Alert.AlertType.ERROR, "Valor no valido", "Revisa el valor introducido antes de guardar.");
                }
            }
        };
    }

    private Integer selectedYear() {
        return config.selectedYearValue().get();
    }

    private Integer selectedMonth() {
        return config.selectedMonthValue().get();
    }

    private record AgentLedgerTemplate(String kind, String label, String section, String type, String styleClass, boolean editable, boolean hasValues) {
        static AgentLedgerTemplate header(String label, String styleClass) { return new AgentLedgerTemplate("HEADER", label, "", "", styleClass, false, false); }
        static AgentLedgerTemplate section(String label) { return new AgentLedgerTemplate("SECTION", label, "", "", "agent-ledger-section-row", false, false); }
        static AgentLedgerTemplate input(String label, String section, String type) { return new AgentLedgerTemplate("INPUT", label, section, type, "agent-ledger-input-row", true, true); }
        static AgentLedgerTemplate total(String label, String styleClass) { return new AgentLedgerTemplate("TOTAL", label, "", "", styleClass, false, true); }
        static AgentLedgerTemplate grandTotal(String label, String styleClass) { return new AgentLedgerTemplate("GRAND", label, "", "", styleClass, false, true); }
    }

    private static class AgentLedgerRow {
        private final AgentLedgerTemplate template;
        private final Map<Integer, List<NylRecord>> recordsByMonth = new LinkedHashMap<>();
        private final double[] displayValues = new double[13];

        AgentLedgerRow(AgentLedgerTemplate template) {
            this.template = template;
            for (int month = 1; month <= 12; month++) recordsByMonth.put(month, new ArrayList<>());
        }

        AgentLedgerTemplate template() { return template; }
        String categoryText() { return "HEADER".equals(template.kind()) || "GRAND".equals(template.kind()) ? template.label() : ""; }
        String conceptText() { return "HEADER".equals(template.kind()) || "GRAND".equals(template.kind()) ? "" : template.label(); }
        List<NylRecord> records(int month) { return recordsByMonth.getOrDefault(month, List.of()); }
        double rawValue(int month) { return records(month).stream().mapToDouble(NylRecord::getAmount).sum(); }
        double displayValue(int month) { return displayValues[month]; }
        void setDisplayValue(int month, double value) { displayValues[month] = value; }
    }

    public record Config(
        Supplier<Integer> selectedYearValue,
        Supplier<Integer> selectedMonthValue,
        Consumer<Integer> setSelectedYearValue,
        Consumer<Integer> setSelectedMonthValue,
        Consumer<Parent> setPage,
        Runnable showNylHub,
        BackButtonFactory backButton,
        AlertAction alert,
        ConfirmAction confirm
    ) {
    }

    @FunctionalInterface public interface BackButtonFactory { Button create(String text, Runnable action); }
    @FunctionalInterface public interface AlertAction { void show(Alert.AlertType type, String title, String message); }
    @FunctionalInterface public interface ConfirmAction { boolean confirm(String title, String message, String confirmText); }
}
