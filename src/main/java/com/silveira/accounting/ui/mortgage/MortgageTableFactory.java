package com.silveira.accounting.ui.mortgage;

import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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

public class MortgageTableFactory {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MortgageApplicationService mortgage;
    private final Config config;

    public MortgageTableFactory(MortgageApplicationService mortgage, Config config) {
        this.mortgage = mortgage;
        this.config = config;
    }

    public TableView<MortgageStatement> statementTable() {
        TableView<MortgageStatement> table = new TableView<>();
        table.setEditable(true);
        TableColumn<MortgageStatement, Integer> year = new TableColumn<>("Año");
        year.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStatementDate() == null ? 0 : data.getValue().getStatementDate().getYear()).asObject());
        TableColumn<MortgageStatement, Integer> month = new TableColumn<>("Mes");
        month.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStatementDate() == null ? 0 : data.getValue().getStatementDate().getMonthValue()).asObject());
        TableColumn<MortgageStatement, String> statementDate = mortgageDateColumn("Statement Date\n(Fecha del estado)", MortgageStatement::getStatementDate, MortgageStatement::setStatementDate);
        TableColumn<MortgageStatement, String> dueDate = mortgageDateColumn("Payment Due Date\n(Fecha límite de pago)", MortgageStatement::getPaymentDueDate, MortgageStatement::setPaymentDueDate);
        TableColumn<MortgageStatement, Double> totalDue = mortgageMoneyColumn("Total Due\n(Deuda a pagar\neste mes)", s -> s.getTotalDue() > 0 ? s.getTotalDue() : s.getPaymentAmountDue(), MortgageStatement::setTotalDue);
        TableColumn<MortgageStatement, Boolean> reviewed = mortgageStatementReviewedColumn();
        TableColumn<MortgageStatement, Double> principal = mortgageMoneyColumn("Principal\n(Pago principal\nmensual)", MortgageStatement::getPrincipalDue, MortgageStatement::setPrincipalDue);
        TableColumn<MortgageStatement, Double> interest = mortgageMoneyColumn("Interest\n(Intereses)", MortgageStatement::getInterestDue, MortgageStatement::setInterestDue);
        TableColumn<MortgageStatement, Double> escrow = mortgageMoneyColumn("Escrow\n(Reserva impuestos/seguros)", MortgageStatement::getEscrowDue, MortgageStatement::setEscrowDue);
        TableColumn<MortgageStatement, Double> fees = mortgageMoneyColumn("Fees\n(Cargos)", MortgageStatement::getFees, MortgageStatement::setFees);
        TableColumn<MortgageStatement, Double> debt = mortgageMoneyColumn("Outstanding Principal\n(Deuda principal\npendiente)", MortgageStatement::getOutstandingPrincipalBalance, MortgageStatement::setOutstandingPrincipalBalance);
        TableColumn<MortgageStatement, Double> original = mortgageMoneyColumn("Original Principal\n(Principal original)", MortgageStatement::getOriginalPrincipalBalance, MortgageStatement::setOriginalPrincipalBalance);
        TableColumn<MortgageStatement, Double> rate = mortgageMoneyColumn("Interest Rate\n(Tasa de interés)", MortgageStatement::getInterestRate, MortgageStatement::setInterestRate);
        TableColumn<MortgageStatement, String> maturity = mortgageDateColumn("Maturity Date\n(Fecha finalizacion)", MortgageStatement::getMaturityDate, MortgageStatement::setMaturityDate);
        TableColumn<MortgageStatement, String> servicer = new TableColumn<>("Servicer\n(Entidad)");
        servicer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getServicerName()));
        servicer.setCellFactory(TextFieldTableCell.forTableColumn());
        servicer.setOnEditCommit(event -> event.getRowValue().setServicerName(event.getNewValue()));
        TableColumn<MortgageStatement, String> status = new TableColumn<>("Revisión");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<MortgageStatement, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(TextFieldTableCell.forTableColumn());
        notes.setOnEditCommit(event -> event.getRowValue().setReviewNotes(event.getNewValue()));
        notes.setPrefWidth(240);
        TableColumn<MortgageStatement, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    MortgageStatement statement = getTableView().getItems().get(getIndex());
                    if (statement.getId() > 0) {
                        mortgage.statements().delete(statement.getId());
                    }
                    getTableView().getItems().remove(statement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
        table.getColumns().setAll(year, month, statementDate, dueDate, totalDue, reviewed, principal, interest, escrow, fees, debt, original, rate, maturity, servicer, status, notes, delete);
        return table;
    }

    public TableView<MortgageTransaction> transactionTable() {
        TableView<MortgageTransaction> table = new TableView<>();
        table.setEditable(true);
        TableColumn<MortgageTransaction, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransactionDate() == null ? "" : data.getValue().getTransactionDate().toString()));
        date.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        date.setOnEditCommit(event -> {
            event.getRowValue().setTransactionDate(parseDateOrNull(event.getNewValue()));
            config.updateTransactionIfSaved().update(event.getRowValue());
        });
        TableColumn<MortgageTransaction, String> description = new TableColumn<>("Descripción");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            config.updateTransactionIfSaved().update(event.getRowValue());
        });
        description.setPrefWidth(320);
        TableColumn<MortgageTransaction, Double> total = mortgageTxMoneyColumn("Total\n(Total)", MortgageTransaction::getTotal, MortgageTransaction::setTotal);
        TableColumn<MortgageTransaction, Boolean> reviewed = mortgageTransactionReviewedColumn();
        TableColumn<MortgageTransaction, Double> principal = mortgageTxMoneyColumn("Principal\n(Pago principal\nmensual)", MortgageTransaction::getPrincipal, MortgageTransaction::setPrincipal);
        TableColumn<MortgageTransaction, Double> interest = mortgageTxMoneyColumn("Interest\n(Intereses)", MortgageTransaction::getInterest, MortgageTransaction::setInterest);
        TableColumn<MortgageTransaction, Double> escrow = mortgageTxMoneyColumn("Escrow\n(Reserva)", MortgageTransaction::getEscrow, MortgageTransaction::setEscrow);
        TableColumn<MortgageTransaction, Double> fees = mortgageTxMoneyColumn("Fees\n(Cargos)", MortgageTransaction::getFees, MortgageTransaction::setFees);
        TableColumn<MortgageTransaction, Double> unapplied = mortgageTxMoneyColumn("Unapplied\n(No aplicado)", MortgageTransaction::getUnapplied, MortgageTransaction::setUnapplied);
        TableColumn<MortgageTransaction, Double> other = mortgageTxMoneyColumn("Other\n(Otros)", MortgageTransaction::getOther, MortgageTransaction::setOther);
        TableColumn<MortgageTransaction, String> status = new TableColumn<>("Revisión");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<MortgageTransaction, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setReviewNotes(event.getNewValue());
            config.updateTransactionIfSaved().update(event.getRowValue());
        });
        notes.setPrefWidth(240);
        TableColumn<MortgageTransaction, Void> actions = new TableColumn<>("Acciones");
        actions.setPrefWidth(160);
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button edit = new Button("Editar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, edit, delete);
            {
                edit.setOnAction(event -> {
                    MortgageTransaction movement = getTableView().getItems().get(getIndex());
                    config.editTransaction().edit(movement, () -> getTableView().refresh());
                });
                delete.getStyleClass().add("danger-button");
                delete.setOnAction(event -> {
                    MortgageTransaction movement = getTableView().getItems().get(getIndex());
                    if (movement.getId() > 0) {
                        mortgage.transactions().delete(movement.getId());
                    }
                    getTableView().getItems().remove(movement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        table.getColumns().setAll(date, description, total, reviewed, principal, interest, escrow, fees, unapplied, other, status, notes, actions);
        return table;
    }

    private TableColumn<MortgageStatement, Boolean> mortgageStatementReviewedColumn() {
        TableColumn<MortgageStatement, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    MortgageStatement statement = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    config.updateStatementReview().update(statement, isReviewed);
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

    private TableColumn<MortgageStatement, Double> mortgageMoneyColumn(String title, java.util.function.ToDoubleFunction<MortgageStatement> getter, java.util.function.BiConsumer<MortgageStatement, Double> setter) {
        TableColumn<MortgageStatement, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleDoubleProperty(getter.applyAsDouble(data.getValue())).asObject());
        column.setCellFactory(TextFieldTableCell.forTableColumn(twoDecimalConverter()));
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), event.getNewValue()));
        column.setPrefWidth(130);
        return column;
    }

    private TableColumn<MortgageStatement, String> mortgageDateColumn(String title, java.util.function.Function<MortgageStatement, LocalDate> getter, java.util.function.BiConsumer<MortgageStatement, LocalDate> setter) {
        TableColumn<MortgageStatement, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue()).toString()));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), parseDateOrNull(event.getNewValue())));
        column.setPrefWidth(120);
        return column;
    }

    private TableColumn<MortgageTransaction, Boolean> mortgageTransactionReviewedColumn() {
        TableColumn<MortgageTransaction, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    MortgageTransaction row = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    config.updateTransactionReview().update(row, isReviewed);
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

    private TableColumn<MortgageTransaction, Double> mortgageTxMoneyColumn(String title, java.util.function.ToDoubleFunction<MortgageTransaction> getter, java.util.function.BiConsumer<MortgageTransaction, Double> setter) {
        TableColumn<MortgageTransaction, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleDoubleProperty(getter.applyAsDouble(data.getValue())).asObject());
        column.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        column.setOnEditCommit(event -> {
            setter.accept(event.getRowValue(), event.getNewValue());
            config.updateTransactionIfSaved().update(event.getRowValue());
        });
        column.setPrefWidth(110);
        return column;
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
                    config.invalidValue().show();
                }
            }
        };
    }

    public record Config(
        UpdateStatementReviewAction updateStatementReview,
        UpdateTransactionReviewAction updateTransactionReview,
        UpdateTransactionAction updateTransactionIfSaved,
        EditTransactionAction editTransaction,
        InvalidValueAction invalidValue
    ) {
    }

    @FunctionalInterface
    public interface UpdateStatementReviewAction {
        void update(MortgageStatement statement, boolean reviewed);
    }

    @FunctionalInterface
    public interface UpdateTransactionReviewAction {
        void update(MortgageTransaction transaction, boolean reviewed);
    }

    @FunctionalInterface
    public interface UpdateTransactionAction {
        void update(MortgageTransaction transaction);
    }

    @FunctionalInterface
    public interface EditTransactionAction {
        void edit(MortgageTransaction transaction, Runnable refresh);
    }

    @FunctionalInterface
    public interface InvalidValueAction {
        void show();
    }
}
