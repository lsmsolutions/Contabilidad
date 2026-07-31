package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.service.HouseExpenseApplicationService;
import com.silveira.accounting.models.HouseExpense;
import com.silveira.accounting.utils.Money;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class HouseExpensePageView {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final double HOUSE_EXPENSE_ROW_HEIGHT = 38;
    private static final double HOUSE_EXPENSE_TABLE_HEADER_HEIGHT = 44;
    private static final double HOUSE_EXPENSE_TABLE_EXTRA_HEIGHT = 20;
    private static final double HOUSE_EXPENSE_TABLE_WIDTH = 1475;

    private final HouseExpenseApplicationService houseExpenses;
    private final Config config;

    public HouseExpensePageView(HouseExpenseApplicationService houseExpenses, Config config) {
        this.houseExpenses = houseExpenses;
        this.config = config;
    }

    public Content build() {
        return build(null);
    }

    public Content build(String loanAlias) {
        Map<Long, String> originalRows = new HashMap<>();
        Label totalValue = new Label();
        Runnable[] refreshTotal = new Runnable[1];
        TableView<HouseExpense> table = houseExpenseTable(() -> refreshTotal[0].run(), originalRows);
        refreshTotal[0] = () -> totalValue.setText(Money.format(totalHouseExpenses(table)));
        Runnable refresh = () -> {
            table.setItems(FXCollections.observableArrayList(houseExpenses.findByLoan(loanAlias, null, null)));
            captureHouseExpenseRows(table, originalRows);
            updateHouseExpenseTableHeight(table);
            refreshTotal[0].run();
        };
        refresh.run();
        Button add = new Button("Add expense");
        add.getStyleClass().add("primary");
        sizeActionButton(add);
        add.setOnAction(event -> {
            HouseExpense expense = new HouseExpense(0, loanAlias == null ? "" : loanAlias, LocalDate.now(), "Gasto manual", "", 0, "", "");
            table.getItems().add(0, expense);
            table.getSelectionModel().select(expense);
            table.scrollTo(0);
            table.requestFocus();
            if (!table.getColumns().isEmpty()) {
                table.edit(0, table.getColumns().get(0));
            }
            updateHouseExpenseTableHeight(table);
            refreshTotal[0].run();
        });
        Button save = new Button("Save changes");
        sizeActionButton(save);
        save.setOnAction(event -> saveHouseExpenseChanges(table, originalRows));
        Region spacer = new Region();
        spacer.setPrefWidth(12);
        return new Content(new HBox(12, add, save, spacer, totalCard("Total expenses", totalValue)), table);
    }

    private void sizeActionButton(Button button) {
        button.setMinWidth(188);
        button.setPrefWidth(188);
        button.setMaxWidth(188);
        button.setMinHeight(42);
        button.setPrefHeight(42);
    }

    private double totalHouseExpenses(TableView<HouseExpense> table) {
        return table.getItems().stream().mapToDouble(HouseExpense::getAmount).sum();
    }

    private HBox totalCard(String title, Label totalValue) {
        Label label = new Label(title);
        label.getStyleClass().add("mini-total-title");
        totalValue.getStyleClass().add("mini-total-value");
        totalValue.setStyle("-fx-text-fill: #126ba3;");
        HBox box = new HBox(16, label, totalValue);
        box.getStyleClass().addAll("mini-total", "neutral-total");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private TableView<HouseExpense> houseExpenseTable(Runnable rowsChanged, Map<Long, String> originalRows) {
        TableView<HouseExpense> table = new TableView<>();
        table.getStyleClass().add("house-expenses-table");
        table.setFixedCellSize(HOUSE_EXPENSE_ROW_HEIGHT);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setMinWidth(HOUSE_EXPENSE_TABLE_WIDTH);
        table.setPrefWidth(HOUSE_EXPENSE_TABLE_WIDTH);
        table.setEditable(true);
        TableColumn<HouseExpense, String> date = new TableColumn<>("Date");
        configureDateHeader(date);
        date.setCellValueFactory(data -> new SimpleStringProperty(formatShortDate(data.getValue().getExpenseDate())));
        date.setComparator((left, right) -> compareDates(parseDateOrNull(left), parseDateOrNull(right)));
        date.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        date.setOnEditCommit(event -> {
            event.getRowValue().setExpenseDate(parseDateOrNull(event.getNewValue()));
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        date.setPrefWidth(145);
        date.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> mortgageColumn = new TableColumn<>("Loan");
        mortgageColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoanAlias()));
        mortgageColumn.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        mortgageColumn.setOnEditCommit(event -> {
            event.getRowValue().setLoanAlias(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        mortgageColumn.setPrefWidth(70);
        mortgageColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> description = new TableColumn<>("Description");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        description.setPrefWidth(215);
        description.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> provider = new TableColumn<>("Provider");
        provider.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProvider()));
        provider.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        provider.setOnEditCommit(event -> {
            event.getRowValue().setProvider(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        provider.setPrefWidth(130);
        provider.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Double> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> {
            event.getRowValue().setAmount(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        amount.setPrefWidth(85);
        amount.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<HouseExpense, String> invoice = new TableColumn<>("Invoice");
        invoice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInvoice()));
        invoice.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        invoice.setOnEditCommit(event -> {
            event.getRowValue().setInvoice(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        invoice.setPrefWidth(105);
        invoice.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> paymentSource = new TableColumn<>("Paid With");
        paymentSource.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentSource()));
        paymentSource.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(config.paymentSourceOptions().get())));
        paymentSource.setOnEditCommit(event -> {
            event.getRowValue().setPaymentSource(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        paymentSource.setPrefWidth(140);
        paymentSource.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Boolean> reviewed = houseExpenseReviewedColumn(rowsChanged);
        reviewed.setEditable(false);
        reviewed.setPrefWidth(76);
        reviewed.setStyle("-fx-alignment: CENTER;");
        TableColumn<HouseExpense, String> notes = new TableColumn<>("Notes");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setNotes(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        notes.setPrefWidth(124);
        notes.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Void> document = houseExpenseDocumentColumn(rowsChanged, originalRows);
        document.setEditable(false);
        TableColumn<HouseExpense, Void> actions = new TableColumn<>("Actions");
        actions.setEditable(false);
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button save = iconButton("\uD83D\uDCBE", "Save");
            private final Button edit = iconButton("\u270E", "Edit");
            private final Button delete = iconButton("\uD83D\uDDD1", "Delete");
            private final HBox buttons = new HBox(6, save, edit, delete);
            {
                save.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    saveHouseExpense(expense);
                    originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
                    refreshRowsChanged(rowsChanged);
                    getTableView().refresh();
                });
                edit.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(expense);
                    getTableView().scrollTo(expense);
                    getTableView().requestFocus();
                    getTableView().edit(getIndex(), date);
                });
                delete.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    if (!confirmHouseExpenseDeletion(expense)) {
                        return;
                    }
                    if (expense.getId() > 0) {
                        deleteHouseExpenseDocumentFile(expense);
                        houseExpenses.delete(expense.getId());
                        originalRows.remove(expense.getId());
                    }
                    getTableView().getItems().remove(expense);
                    updateHouseExpenseTableHeight(getTableView());
                    refreshRowsChanged(rowsChanged);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actions.setPrefWidth(150);
        table.getColumns().setAll(date, mortgageColumn, description, provider, amount, invoice, paymentSource, document, reviewed, actions, notes);
        return table;
    }

    private boolean confirmHouseExpenseDeletion(HouseExpense expense) {
        String description = expense.getDescription() == null || expense.getDescription().isBlank()
            ? "este gasto"
            : "\"" + expense.getDescription() + "\"";
        Alert alert = new Alert(
            Alert.AlertType.WARNING,
            "Se eliminará " + description + " por " + Money.format(expense.getAmount())
                + ".\n\nEsta acción no se puede deshacer.",
            new ButtonType("Eliminar"),
            ButtonType.CANCEL
        );
        ButtonType delete = alert.getButtonTypes().get(0);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Quieres eliminar este gasto?");
        if (config.owner().get() != null) {
            alert.initOwner(config.owner().get());
        }
        alert.getDialogPane().setMinWidth(560);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == delete;
    }

    private void updateHouseExpenseTableHeight(TableView<HouseExpense> table) {
        double height = HOUSE_EXPENSE_TABLE_HEADER_HEIGHT
            + Math.max(1, table.getItems().size()) * HOUSE_EXPENSE_ROW_HEIGHT
            + HOUSE_EXPENSE_TABLE_EXTRA_HEIGHT;
        table.setMinHeight(height);
        table.setPrefHeight(height);
        table.setMaxHeight(height);
    }

    private void configureDateHeader(TableColumn<HouseExpense, String> date) {
        Label title = new Label("Date");
        Label inputFormat = new Label("(dd/MM/yyyy)");
        inputFormat.setStyle("-fx-font-size: 10px; -fx-text-fill: #66736e;");
        HBox header = new HBox(6, title, inputFormat);
        header.setAlignment(Pos.CENTER_LEFT);
        Tooltip.install(header, new Tooltip("Enter dates as dd/MM/yyyy"));
        date.setText(null);
        date.setGraphic(header);
    }

    private Button iconButton(String icon, String tooltip) {
        Button button = new Button(icon);
        button.setTooltip(new Tooltip(tooltip));
        button.setMinWidth(34);
        button.setPrefWidth(34);
        button.setMinHeight(28);
        button.setPrefHeight(28);
        return button;
    }

    private Button textButton(String text, String tooltip) {
        Button button = new Button(text);
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private TableColumn<HouseExpense, Void> houseExpenseDocumentColumn(Runnable rowsChanged, Map<Long, String> originalRows) {
        TableColumn<HouseExpense, Void> document = new TableColumn<>("Document");
        document.setCellFactory(column -> new TableCell<>() {
            private final Button attach = textButton("Attach", "Attach document");
            private final Button view = textButton("View", "View document");
            private final Button change = textButton("Change", "Change document");
            private final Button remove = textButton("Remove", "Remove document");
            private final HBox buttons = new HBox(6);
            {
                attach.setOnAction(event -> {
                    attachHouseExpenseDocument(currentExpense(), originalRows, rowsChanged);
                    getTableView().refresh();
                });
                view.setOnAction(event -> openHouseExpenseDocument(currentExpense()));
                change.setOnAction(event -> {
                    attachHouseExpenseDocument(currentExpense(), originalRows, rowsChanged);
                    getTableView().refresh();
                });
                remove.setOnAction(event -> {
                    removeHouseExpenseDocument(currentExpense(), originalRows, rowsChanged);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                buttons.getChildren().clear();
                if (!empty) {
                    HouseExpense expense = currentExpense();
                    if (hasHouseExpenseDocument(expense)) {
                        buttons.getChildren().addAll(view, change, remove);
                    } else {
                        buttons.getChildren().add(attach);
                    }
                }
                setGraphic(empty ? null : buttons);
            }

            private HouseExpense currentExpense() {
                return getTableView().getItems().get(getIndex());
            }
        });
        document.setPrefWidth(235);
        return document;
    }

    private TableColumn<HouseExpense, Boolean> houseExpenseReviewedColumn(Runnable rowsChanged) {
        TableColumn<HouseExpense, Boolean> reviewed = new TableColumn<>("Reviewed");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                setAlignment(Pos.CENTER);
                checkBox.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    expense.setPendingReview(!isReviewed);
                    expense.setReviewRequired(!isReviewed);
                    updateHouseExpenseIfSaved(expense, rowsChanged);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        return reviewed;
    }

    private void updateHouseExpenseIfSaved(HouseExpense expense, Runnable rowsChanged) {
        refreshRowsChanged(rowsChanged);
    }

    private void captureHouseExpenseRows(TableView<HouseExpense> table, Map<Long, String> originalRows) {
        originalRows.clear();
        for (HouseExpense expense : table.getItems()) {
            if (expense.getId() > 0) {
                originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
            }
        }
    }

    private void saveHouseExpenseChanges(TableView<HouseExpense> table, Map<Long, String> originalRows) {
        for (HouseExpense expense : table.getItems()) {
            if (expense.getId() == 0 || !houseExpenseSnapshot(expense).equals(originalRows.get(expense.getId()))) {
                saveHouseExpense(expense);
                originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
            }
        }
        table.refresh();
    }

    private void saveHouseExpense(HouseExpense expense) {
        if (expense.getId() == 0) {
            long id = houseExpenses.save(expense);
            expense.setId(id);
        } else {
            houseExpenses.update(expense);
        }
    }

    private void attachHouseExpenseDocument(HouseExpense expense, Map<Long, String> originalRows, Runnable rowsChanged) {
        if (expense == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Adjuntar documento");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documentos e imagenes", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp", "*.tif", "*.tiff", "*.doc", "*.docx", "*.xls", "*.xlsx"),
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        Window owner = config.owner().get();
        File selected = chooser.showOpenDialog(owner);
        if (selected == null) {
            return;
        }
        try {
            if (expense.getId() == 0) {
                saveHouseExpense(expense);
            }
            Path folder = Path.of("data", "documentos", "casa-gastos", String.valueOf(expense.getId()));
            Files.createDirectories(folder);
            String originalName = selected.getName();
            Path target = folder.resolve(UUID.randomUUID() + extension(originalName));
            Files.copy(selected.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            deleteHouseExpenseDocumentFile(expense);
            expense.setDocumentPath(target.toString());
            expense.setDocumentName(originalName);
            houseExpenses.update(expense);
            originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
            refreshRowsChanged(rowsChanged);
        } catch (IOException | RuntimeException exception) {
            config.alert().show(Alert.AlertType.ERROR, "No se pudo adjuntar", config.rootCauseMessage().message(exception));
        }
    }

    private void openHouseExpenseDocument(HouseExpense expense) {
        if (!hasHouseExpenseDocument(expense)) {
            config.alert().show(Alert.AlertType.INFORMATION, "Sin documento", "Este gasto no tiene documento adjunto.");
            return;
        }
        Path path = Path.of(expense.getDocumentPath());
        if (!Files.exists(path)) {
            config.alert().show(Alert.AlertType.WARNING, "Documento no encontrado", "No se encontro el archivo adjunto en:\n" + path);
            return;
        }
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException | RuntimeException exception) {
            config.alert().show(Alert.AlertType.ERROR, "No se pudo abrir", config.rootCauseMessage().message(exception));
        }
    }

    private void removeHouseExpenseDocument(HouseExpense expense, Map<Long, String> originalRows, Runnable rowsChanged) {
        if (expense == null || !hasHouseExpenseDocument(expense)) {
            return;
        }
        deleteHouseExpenseDocumentFile(expense);
        expense.setDocumentPath(null);
        expense.setDocumentName(null);
        if (expense.getId() > 0) {
            houseExpenses.update(expense);
            originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
        }
        refreshRowsChanged(rowsChanged);
    }

    private boolean hasHouseExpenseDocument(HouseExpense expense) {
        return expense != null && expense.getDocumentPath() != null && !expense.getDocumentPath().isBlank();
    }

    private void deleteHouseExpenseDocumentFile(HouseExpense expense) {
        if (!hasHouseExpenseDocument(expense)) {
            return;
        }
        try {
            Path path = Path.of(expense.getDocumentPath()).normalize();
            Path documentsRoot = Path.of("data", "documentos", "casa-gastos").normalize();
            if (path.startsWith(documentsRoot)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // If the file is locked or already gone, keep the data operation moving.
        }
    }

    private String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    private String houseExpenseSnapshot(HouseExpense expense) {
        return String.join("\u001F",
            text(expense.getLoanAlias()),
            expense.getExpenseDate() == null ? "" : expense.getExpenseDate().toString(),
            text(expense.getDescription()),
            text(expense.getProvider()),
            String.valueOf(expense.getAmount()),
            text(expense.getInvoice()),
            text(expense.getPaymentSource()),
            text(expense.getNotes()),
            text(expense.getDocumentPath()),
            text(expense.getDocumentName()),
            String.valueOf(expense.isReviewRequired()),
            String.valueOf(expense.isPendingReview())
        );
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private void refreshRowsChanged(Runnable rowsChanged) {
        if (rowsChanged != null) {
            rowsChanged.run();
        }
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        DateTimeFormatter[] formats = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            SHORT_DATE_FORMAT,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yy")
        };
        RuntimeException last = null;
        for (DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (RuntimeException exception) {
                last = exception;
            }
        }
        throw last == null ? new IllegalArgumentException("Fecha no valida: " + value) : last;
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    private int compareDates(LocalDate left, LocalDate right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private StringConverter<Double> twoDecimalConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : String.format(java.util.Locale.US, "%.2f", value);
            }

            @Override
            public Double fromString(String value) {
                if (value == null || value.isBlank()) {
                    return 0.0;
                }
                return Money.parse(value);
            }
        };
    }

    private StringConverter<String> stringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : value;
            }

            @Override
            public String fromString(String value) {
                return value == null ? "" : value;
            }
        };
    }

    private <S, T> javafx.util.Callback<TableColumn<S, T>, TableCell<S, T>> commitOnFocusLostCellFactory(StringConverter<T> converter) {
        return column -> new TableCell<>() {
            private TextField textField;

            @Override
            public void startEdit() {
                if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                    return;
                }
                super.startEdit();
                if (textField == null) {
                    textField = new TextField();
                    textField.setOnAction(event -> commitCurrentEdit());
                    textField.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ESCAPE) {
                            cancelEdit();
                        } else if (event.getCode() == KeyCode.TAB) {
                            commitCurrentEdit();
                            moveToAdjacentEditableCell(this, event.isShiftDown());
                            event.consume();
                        }
                    });
                    textField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                        if (wasFocused && !isFocused && isEditing()) {
                            commitCurrentEdit();
                        }
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
                    if (textField != null) {
                        textField.setText(converter.toString(item));
                    }
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

    private void moveToAdjacentEditableCell(TableCell<?, ?> cell, boolean reverse) {
        TableView<?> table = cell.getTableView();
        if (table == null) {
            return;
        }
        int rowIndex = cell.getIndex();
        if (rowIndex < 0 || rowIndex >= table.getItems().size()) {
            return;
        }
        int columnIndex = table.getVisibleLeafColumns().indexOf(cell.getTableColumn());
        int step = reverse ? -1 : 1;
        int nextColumnIndex = columnIndex + step;
        while (nextColumnIndex >= 0 && nextColumnIndex < table.getVisibleLeafColumns().size()) {
            TableColumn<?, ?> nextColumn = table.getVisibleLeafColumns().get(nextColumnIndex);
            if (nextColumn.isEditable()) {
                editCell(table, rowIndex, nextColumn);
                return;
            }
            nextColumnIndex += step;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void editCell(TableView<?> table, int rowIndex, TableColumn<?, ?> column) {
        Platform.runLater(() -> {
            table.getSelectionModel().clearAndSelect(rowIndex, (TableColumn) column);
            table.scrollTo(rowIndex);
            table.requestFocus();
            ((TableView) table).edit(rowIndex, (TableColumn) column);
        });
    }

    public boolean confirmNavigation(TableView<HouseExpense> table, Map<Long, String> originalRows, Runnable refresh) {
        if (!hasUnsavedHouseExpenseChanges(table, originalRows)) {
            return true;
        }
        ButtonType save = new ButtonType("Guardar");
        ButtonType discard = new ButtonType("Salir sin guardar");
        Alert alert = new Alert(Alert.AlertType.WARNING,
            "Hay cambios en Casa - Gastos que todavia no has guardado. ¿Quieres guardarlos antes de salir?",
            save,
            discard,
            ButtonType.CANCEL
        );
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("Cambios sin guardar");
        alert.getDialogPane().setMinWidth(620);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            return false;
        }
        if (result.get() == save) {
            try {
                saveHouseExpenseChanges(table, originalRows);
            } catch (RuntimeException exception) {
                config.alert().show(Alert.AlertType.ERROR, "No se pudieron guardar los gastos", config.rootCauseMessage().message(exception));
                return false;
            }
        } else {
            refresh.run();
        }
        return true;
    }

    private boolean hasUnsavedHouseExpenseChanges(TableView<HouseExpense> table, Map<Long, String> originalRows) {
        for (HouseExpense expense : table.getItems()) {
            if (expense.getId() == 0) {
                return true;
            }
            if (!houseExpenseSnapshot(expense).equals(originalRows.get(expense.getId()))) {
                return true;
            }
        }
        return false;
    }

    public record Content(Node actions, TableView<HouseExpense> table) {
    }

    public record Config(
        Supplier<Window> owner,
        Supplier<List<String>> paymentSourceOptions,
        AlertAction alert,
        RootCauseMessage rootCauseMessage
    ) {
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }

    @FunctionalInterface
    public interface RootCauseMessage {
        String message(Throwable exception);
    }
}
