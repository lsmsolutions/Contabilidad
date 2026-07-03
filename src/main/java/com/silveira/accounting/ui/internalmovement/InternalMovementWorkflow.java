package com.silveira.accounting.ui.internalmovement;

import com.silveira.accounting.controllers.internalmovement.InternalMovementController;
import com.silveira.accounting.models.InternalMovementRecord;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class InternalMovementWorkflow {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InternalMovementController controller;
    private final Config config;

    public InternalMovementWorkflow(InternalMovementController controller, Config config) {
        this.controller = controller;
        this.config = config;
    }

    public void showInternalMovements() {
        TableView<InternalMovementRecord> table = internalMovementTable();
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(
            controller.findManual(selectedYear(), selectedMonth())
        ));
        refresh.run();
        ComboBox<Integer> year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(config.selectedYearValue().get());
        ComboBox<Integer> month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(config.selectedMonthValue().get());
        Button filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");
        filter.setOnAction(event -> {
            config.setSelectedYearValue().accept(year.getValue());
            config.setSelectedMonthValue().accept(month.getValue());
            refresh.run();
        });
        Button add = new Button("Anadir movimiento");
        add.setOnAction(event -> table.getItems().add(controller.createManual()));
        Button save = new Button("Guardar visibles");
        save.setOnAction(event -> {
            controller.saveVisible(table.getItems());
            refresh.run();
        });
        VBox actions = actionHeader(
            new HBox(10, new Label("Ano"), year, new Label("Mes"), month, filter),
            new HBox(10, add, save)
        );
        config.setPage().accept(page("Movimientos internos", actions, table));
    }

    private TableView<InternalMovementRecord> internalMovementTable() {
        TableView<InternalMovementRecord> table = new TableView<>();
        table.setEditable(true);
        TableColumn<InternalMovementRecord, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate() == null ? "" : data.getValue().getDate().toString()));
        date.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        date.setOnEditCommit(event -> event.getRowValue().setDate(parseDateOrNull(event.getNewValue())));
        TableColumn<InternalMovementRecord, String> from = new TableColumn<>("Desde");
        from.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFrom()));
        from.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        from.setOnEditCommit(event -> event.getRowValue().setFrom(event.getNewValue()));
        TableColumn<InternalMovementRecord, String> to = new TableColumn<>("Hacia");
        to.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTo()));
        to.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        to.setOnEditCommit(event -> event.getRowValue().setTo(event.getNewValue()));
        TableColumn<InternalMovementRecord, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> event.getRowValue().setAmount(event.getNewValue()));
        TableColumn<InternalMovementRecord, String> description = new TableColumn<>("Descripcion");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));
        TableColumn<InternalMovementRecord, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isReviewed()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> getTableView().getItems().get(getIndex()).setReviewed(checkBox.isSelected()));
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
        TableColumn<InternalMovementRecord, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    InternalMovementRecord movement = getTableView().getItems().get(getIndex());
                    controller.delete(movement);
                    getTableView().getItems().remove(movement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
        table.getColumns().setAll(date, from, to, amount, description, reviewed, delete);
        return table;
    }

    private VBox page(String title, javafx.scene.Node... nodes) {
        Label heading = new Label(title);
        heading.getStyleClass().add("heading");
        VBox box = new VBox(18);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("page");
        return box;
    }

    private VBox actionHeader(javafx.scene.Node... rows) {
        VBox header = new VBox(10);
        header.getStyleClass().add("action-header");
        for (javafx.scene.Node row : rows) {
            if (row instanceof HBox hBox) {
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getStyleClass().add("action-row");
            }
            header.getChildren().add(row);
        }
        return header;
    }

    private Integer selectedYear() {
        return config.selectedYearValue().get();
    }

    private Integer selectedMonth() {
        return config.selectedMonthValue().get();
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

    private <S, T> javafx.util.Callback<TableColumn<S, T>, TableCell<S, T>> commitOnFocusLostCellFactory(
        StringConverter<T> converter
    ) {
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
                    config.alert().show(
                        Alert.AlertType.ERROR,
                        "Valor no valido",
                        "Revisa el valor introducido antes de guardar."
                    );
                }
            }
        };
    }

    public record Config(
        Supplier<Integer> selectedYearValue,
        Supplier<Integer> selectedMonthValue,
        Consumer<Integer> setSelectedYearValue,
        Consumer<Integer> setSelectedMonthValue,
        Consumer<Parent> setPage,
        AlertAction alert
    ) {
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }
}
