package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.models.bank.BankTransaction;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

public class BankTransactionTableView {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BankApplicationService bank;

    public BankTransactionTableView(BankApplicationService bank) {
        this.bank = bank;
    }

    public TableView<BankTransaction> build() {
        return build(null);
    }

    public TableView<BankTransaction> build(Runnable rowsChanged) {
        TableView<BankTransaction> table = new TableView<>();
        table.setEditable(true);

        TableColumn<BankTransaction, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate().toString()));
        date.setCellFactory(TextFieldTableCell.forTableColumn());
        date.setOnEditCommit(event -> {
            LocalDate parsed = parseDateOrNull(event.getNewValue());
            if (parsed != null) {
                event.getRowValue().setDate(parsed);
                event.getRowValue().setMonth(parsed.getMonthValue());
                event.getRowValue().setYear(parsed.getYear());
                updateBankRowIfSaved(event.getRowValue());
                refreshAfterBankRowChange(rowsChanged);
            }
        });

        TableColumn<BankTransaction, String> description = new TableColumn<>("Descripci\u00f3n");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            updateBankRowIfSaved(event.getRowValue());
            refreshAfterBankRowChange(rowsChanged);
        });
        description.setPrefWidth(360);

        TableColumn<BankTransaction, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> {
            event.getRowValue().setAmount(event.getNewValue().doubleValue());
            bank.transactions().normalizeSign(event.getRowValue());
            updateBankRowIfSaved(event.getRowValue());
            table.refresh();
            refreshAfterBankRowChange(rowsChanged);
        });

        TableColumn<BankTransaction, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    int rowIndex = getIndex();
                    BankTransaction transaction = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    transaction.setPendingReview(!isReviewed);
                    transaction.setReviewRequired(!isReviewed);
                    if (isReviewed && (transaction.getReviewNotes() == null || transaction.getReviewNotes().isBlank() || transaction.getReviewNotes().startsWith("Revisar"))) {
                        transaction.setReviewNotes("Revisado");
                    }
                    updateBankRowIfSaved(transaction);
                    refreshAfterBankRowChange(rowsChanged);
                    Platform.runLater(() -> {
                        getTableView().getSelectionModel().select(rowIndex);
                        getTableView().scrollTo(rowIndex);
                        getTableView().requestFocus();
                        getTableView().refresh();
                    });
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

        TableColumn<BankTransaction, String> type = new TableColumn<>("Tipo");
        type.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMovementType()));
        type.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        type.setOnEditCommit(event -> {
            event.getRowValue().setMovementType(event.getNewValue());
            bank.transactions().normalizeSign(event.getRowValue());
            updateBankRowIfSaved(event.getRowValue());
            table.refresh();
            refreshAfterBankRowChange(rowsChanged);
        });

        TableColumn<BankTransaction, String> provider = new TableColumn<>("Proveedor");
        provider.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProvider()));
        provider.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        provider.setOnEditCommit(event -> {
            event.getRowValue().setProvider(event.getNewValue());
            if (event.getRowValue().getId() > 0) {
                bank.transactions().updateProvider(event.getRowValue().getId(), event.getNewValue(), event.getRowValue().getMovementType());
            }
            refreshAfterBankRowChange(rowsChanged);
        });

        TableColumn<BankTransaction, String> reference = new TableColumn<>("Referencia");
        reference.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReference()));

        TableColumn<BankTransaction, String> review = new TableColumn<>("Revisi\u00f3n");
        review.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));

        TableColumn<BankTransaction, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setReviewNotes(event.getNewValue());
            updateBankRowIfSaved(event.getRowValue());
            refreshAfterBankRowChange(rowsChanged);
        });
        notes.setPrefWidth(220);

        TableColumn<BankTransaction, Void> actions = new TableColumn<>("Acciones");
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button save = new Button("Guardar");
            private final Button edit = new Button("Editar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, save, edit, delete);
            {
                save.setOnAction(event -> {
                    BankTransaction transaction = getTableView().getItems().get(getIndex());
                    bank.transactions().normalizeSign(transaction);
                    updateBankRowIfSaved(transaction);
                    getTableView().refresh();
                    refreshAfterBankRowChange(rowsChanged);
                });
                edit.setOnAction(event -> {
                    BankTransaction transaction = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(transaction);
                    getTableView().scrollTo(transaction);
                    getTableView().requestFocus();
                    getTableView().edit(getIndex(), date);
                });
                delete.setOnAction(event -> {
                    BankTransaction transaction = getTableView().getItems().get(getIndex());
                    if (transaction.getId() > 0) {
                        bank.transactions().delete(transaction.getId());
                    }
                    getTableView().getItems().remove(transaction);
                    refreshAfterBankRowChange(rowsChanged);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actions.setPrefWidth(230);

        table.getColumns().setAll(date, description, amount, reviewed, type, provider, reference, review, notes, actions);
        return table;
    }

    private void refreshAfterBankRowChange(Runnable rowsChanged) {
        if (rowsChanged != null) {
            rowsChanged.run();
        }
    }

    private void updateBankRowIfSaved(BankTransaction transaction) {
        if (transaction.getId() > 0) {
            bank.transactions().update(transaction);
        }
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (RuntimeException ignored) {
            return LocalDate.parse(trimmed, SHORT_DATE_FORMAT);
        }
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
                return Double.parseDouble(value.replace("$", "").replace(",", "").trim());
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
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Valor no valido");
                    alert.setHeaderText("Valor no valido");
                    alert.setContentText("Revisa el valor introducido antes de guardar.");
                    alert.showAndWait();
                }
            }
        };
    }
}
