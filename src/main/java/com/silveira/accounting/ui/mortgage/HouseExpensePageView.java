package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class HouseExpensePageView {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MortgageApplicationService mortgage;
    private final Config config;

    public HouseExpensePageView(MortgageApplicationService mortgage, Config config) {
        this.mortgage = mortgage;
        this.config = config;
    }

    public Content build() {
        Map<Long, String> originalRows = new HashMap<>();
        TableView<HouseExpense> table = houseExpenseTable(null, originalRows);
        Runnable refresh = () -> {
            table.setItems(FXCollections.observableArrayList(mortgage.houseExpenses().findByLoan(null, null, null)));
            captureHouseExpenseRows(table, originalRows);
        };
        refresh.run();
        Button add = new Button("Anadir gasto");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> {
            HouseExpense expense = new HouseExpense(0, "", LocalDate.now(), "Gasto manual", "", 0, "", "");
            table.getItems().add(expense);
            table.getSelectionModel().select(expense);
        });
        Button save = new Button("Guardar cambios");
        save.setOnAction(event -> saveHouseExpenseChanges(table, originalRows));
        return new Content(new HBox(10, add, save), table);
    }

    private TableView<HouseExpense> houseExpenseTable(Runnable rowsChanged, Map<Long, String> originalRows) {
        TableView<HouseExpense> table = new TableView<>();
        table.getStyleClass().add("house-expenses-table");
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setEditable(true);
        TableColumn<HouseExpense, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(formatShortDate(data.getValue().getExpenseDate())));
        date.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        date.setOnEditCommit(event -> {
            event.getRowValue().setExpenseDate(parseDateOrNull(event.getNewValue()));
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        date.setPrefWidth(120);
        date.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> mortgageColumn = new TableColumn<>("Hipoteca");
        mortgageColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoanAlias()));
        mortgageColumn.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        mortgageColumn.setOnEditCommit(event -> {
            event.getRowValue().setLoanAlias(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        mortgageColumn.setPrefWidth(130);
        mortgageColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> description = new TableColumn<>("Descripción");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        description.setPrefWidth(260);
        description.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> provider = new TableColumn<>("Proveedor");
        provider.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProvider()));
        provider.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        provider.setOnEditCommit(event -> {
            event.getRowValue().setProvider(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        provider.setPrefWidth(180);
        provider.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> {
            event.getRowValue().setAmount(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        amount.setPrefWidth(110);
        amount.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<HouseExpense, String> invoice = new TableColumn<>("Factura");
        invoice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInvoice()));
        invoice.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        invoice.setOnEditCommit(event -> {
            event.getRowValue().setInvoice(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        invoice.setPrefWidth(160);
        invoice.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> paymentSource = new TableColumn<>("Pagado con");
        paymentSource.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentSource()));
        paymentSource.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(config.paymentSourceOptions().get())));
        paymentSource.setOnEditCommit(event -> {
            event.getRowValue().setPaymentSource(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        paymentSource.setPrefWidth(210);
        paymentSource.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Boolean> reviewed = houseExpenseReviewedColumn(rowsChanged);
        reviewed.setStyle("-fx-alignment: CENTER;");
        TableColumn<HouseExpense, String> notes = new TableColumn<>("Nota");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setNotes(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        notes.setPrefWidth(240);
        notes.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Void> document = houseExpenseDocumentColumn(rowsChanged, originalRows);
        TableColumn<HouseExpense, Void> actions = new TableColumn<>("Acciones");
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button save = new Button("Guardar");
            private final Button edit = new Button("Editar");
            private final Button delete = new Button("Eliminar");
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
                    if (expense.getId() > 0) {
                        deleteHouseExpenseDocumentFile(expense);
                        mortgage.houseExpenses().delete(expense.getId());
                        originalRows.remove(expense.getId());
                    }
                    getTableView().getItems().remove(expense);
                    refreshRowsChanged(rowsChanged);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actions.setPrefWidth(230);
        table.getColumns().setAll(date, mortgageColumn, description, provider, amount, invoice, paymentSource, document, reviewed, notes, actions);
        return table;
    }

    private TableColumn<HouseExpense, Void> houseExpenseDocumentColumn(Runnable rowsChanged, Map<Long, String> originalRows) {
        TableColumn<HouseExpense, Void> document = new TableColumn<>("Documento");
        document.setCellFactory(column -> new TableCell<>() {
            private final Button attach = new Button("Adjuntar");
            private final Button view = new Button("Ver");
            private final Button change = new Button("Cambiar");
            private final Button remove = new Button("Quitar");
            private final HBox buttons = new HBox(6);
            {
                attach.setOnAction(event -> attachHouseExpenseDocument(currentExpense(), originalRows, rowsChanged));
                view.setOnAction(event -> openHouseExpenseDocument(currentExpense()));
                change.setOnAction(event -> attachHouseExpenseDocument(currentExpense(), originalRows, rowsChanged));
                remove.setOnAction(event -> removeHouseExpenseDocument(currentExpense(), originalRows, rowsChanged));
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
        document.setPrefWidth(220);
        return document;
    }

    private TableColumn<HouseExpense, Boolean> houseExpenseReviewedColumn(Runnable rowsChanged) {
        TableColumn<HouseExpense, Boolean> reviewed = new TableColumn<>("Revisado");
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
            long id = mortgage.houseExpenses().save(expense);
            expense.setId(id);
        } else {
            mortgage.houseExpenses().update(expense);
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
            mortgage.houseExpenses().update(expense);
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
            mortgage.houseExpenses().update(expense);
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
