package com.silveira.accounting.ui.nyl;

import com.silveira.accounting.controllers.nyl.NylController;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.utils.Money;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

public class NylTableView {
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

    private final NylController controller;
    private final AlertAction alert;

    public NylTableView(NylController controller, AlertAction alert) {
        this.controller = controller;
        this.alert = alert;
    }

    public TableView<NylRecord> build() {
        TableView<NylRecord> table = new TableView<>();
        table.setEditable(true);
        table.setRowFactory(view -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(NylRecord item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("month-row-"));
                if (!empty && item != null) {
                    getStyleClass().add("month-row-" + item.getMonth());
                }
            }
        });
        TableColumn<NylRecord, Integer> year = new TableColumn<>("Año");
        year.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getYear()).asObject());
        year.setCellFactory(commitOnFocusLostCellFactory(new IntegerStringConverter()));
        year.setOnEditCommit(event -> {
            event.getRowValue().setYear(event.getNewValue());
            controller.recordCorrection(event.getRowValue().getFingerprint(), "year", String.valueOf(event.getOldValue()), String.valueOf(event.getNewValue()), "edicion en tabla");
        });
        TableColumn<NylRecord, Integer> month = new TableColumn<>("Mes");
        month.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getMonth()).asObject());
        month.setCellFactory(commitOnFocusLostCellFactory(new IntegerStringConverter()));
        month.setOnEditCommit(event -> {
            event.getRowValue().setMonth(event.getNewValue());
            controller.recordCorrection(event.getRowValue().getFingerprint(), "month", String.valueOf(event.getOldValue()), String.valueOf(event.getNewValue()), "edicion en tabla");
        });
        TableColumn<NylRecord, String> concept = new TableColumn<>("Concepto");
        concept.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConcept()));
        concept.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        concept.setOnEditCommit(event -> {
            event.getRowValue().setConcept(event.getNewValue());
            controller.recordCorrection(event.getRowValue().getFingerprint(), "concept", event.getOldValue(), event.getNewValue(), "edicion en tabla");
        });
        concept.setPrefWidth(360);
        TableColumn<NylRecord, String> section = new TableColumn<>("Apartado");
        section.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSection()));
        section.setCellFactory(ComboBoxTableCell.forTableColumn(SECTION_OPTIONS));
        section.setOnEditCommit(event -> {
            event.getRowValue().setSection(event.getNewValue());
            event.getRowValue().setAmount(normalizedAmount(event.getRowValue(), event.getRowValue().getAmount()));
            controller.recordCorrection(event.getRowValue().getFingerprint(), "section", event.getOldValue(), event.getNewValue(), "edicion en tabla");
            table.refresh();
        });
        TableColumn<NylRecord, String> type = new TableColumn<>("Tipo");
        type.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecordType()));
        type.setCellFactory(ComboBoxTableCell.forTableColumn("credito", "deduccion"));
        type.setOnEditCommit(event -> {
            event.getRowValue().setRecordType(event.getNewValue());
            event.getRowValue().setAmount(normalizedAmount(event.getRowValue(), event.getRowValue().getAmount()));
            controller.recordCorrection(event.getRowValue().getFingerprint(), "record_type", event.getOldValue(), event.getNewValue(), "edicion en tabla");
            table.refresh();
        });
        TableColumn<NylRecord, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> {
            event.getRowValue().setAmount(normalizedAmount(event.getRowValue(), event.getNewValue()));
            controller.recordCorrection(event.getRowValue().getFingerprint(), "amount", String.valueOf(event.getOldValue()), String.valueOf(event.getNewValue()), "edicion en tabla");
            table.refresh();
        });
        TableColumn<NylRecord, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    record.setPendingReview(!isReviewed);
                    record.setReviewRequired(!isReviewed);
                    if (isReviewed && (record.getReviewNotes() == null || record.getReviewNotes().isBlank() || record.getReviewNotes().startsWith("OCR:"))) {
                        record.setReviewNotes("Revisado");
                    }
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
        TableColumn<NylRecord, String> status = new TableColumn<>("Estado");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImportStatus()));
        TableColumn<NylRecord, String> review = new TableColumn<>("Revisión");
        review.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<NylRecord, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setReviewNotes(event.getNewValue());
            controller.recordCorrection(event.getRowValue().getFingerprint(), "review_notes", event.getOldValue(), event.getNewValue(), "edicion en tabla");
        });
        notes.setPrefWidth(260);
        TableColumn<NylRecord, Void> actions = new TableColumn<>("Acciones");
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button save = new Button("Guardar");
            private final Button edit = new Button("Editar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, save, edit, delete);
            {
                save.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    record.setAmount(normalizedAmount(record, record.getAmount()));
                    if (record.getId() > 0) {
                        controller.updateRecord(record);
                    }
                    getTableView().refresh();
                });
                edit.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(record);
                    getTableView().scrollTo(record);
                    getTableView().requestFocus();
                    getTableView().edit(getIndex(), year);
                });
                delete.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    if (record.getId() > 0) {
                        controller.delete(record.getId());
                    }
                    getTableView().getItems().remove(record);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actions.setPrefWidth(230);
        table.getColumns().setAll(year, month, section, concept, type, amount, reviewed, status, review, notes, actions);
        return table;
    }

    private double normalizedAmount(NylRecord record, double amount) {
        boolean deduction = "deduccion".equalsIgnoreCase(record.getRecordType())
            || "Deducciones".equalsIgnoreCase(record.getSection());
        return deduction ? -Math.abs(amount) : Math.abs(amount);
    }

    private StringConverter<Double> twoDecimalConverter() {
        return new StringConverter<>() {
            @Override public String toString(Double value) { return value == null ? "" : String.format(java.util.Locale.US, "%.2f", value); }
            @Override public Double fromString(String value) { return value == null || value.isBlank() ? 0.0 : Money.parse(value); }
        };
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
                    alert.show(Alert.AlertType.ERROR, "Valor no valido", "Revisa el valor introducido antes de guardar.");
                }
            }
        };
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }
}
