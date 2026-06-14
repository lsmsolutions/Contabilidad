package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;

public class CardStatementTableView {
    public TableView<CreditCardStatement> build(
        BiConsumer<CreditCardStatement, Boolean> updateReview,
        Consumer<CreditCardStatement> deleteStatement,
        Function<String, LocalDate> parseDate
    ) {
        TableView<CreditCardStatement> table = new TableView<>();
        table.setEditable(true);

        TableColumn<CreditCardStatement, Integer> year = new TableColumn<>("A\u00f1o");
        year.setCellValueFactory(data -> new SimpleIntegerProperty(
            data.getValue().getStatementEndDate() == null ? 0 : data.getValue().getStatementEndDate().getYear()
        ).asObject());
        TableColumn<CreditCardStatement, Integer> month = new TableColumn<>("Mes");
        month.setCellValueFactory(data -> new SimpleIntegerProperty(
            data.getValue().getStatementEndDate() == null ? 0 : data.getValue().getStatementEndDate().getMonthValue()
        ).asObject());

        TableColumn<CreditCardStatement, String> startDate = dateColumn(
            "Inicio ciclo", CreditCardStatement::getStatementStartDate, CreditCardStatement::setStatementStartDate, parseDate
        );
        TableColumn<CreditCardStatement, String> endDate = dateColumn(
            "Cierre ciclo", CreditCardStatement::getStatementEndDate, CreditCardStatement::setStatementEndDate, parseDate
        );
        TableColumn<CreditCardStatement, Double> newBalance = moneyColumn(
            "Deuda cierre", CreditCardStatement::getNewBalance, CreditCardStatement::setNewBalance
        );
        TableColumn<CreditCardStatement, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    CreditCardStatement statement = getTableView().getItems().get(getIndex());
                    updateReview.accept(statement, checkBox.isSelected());
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

        TableColumn<CreditCardStatement, Double> minimum = moneyColumn(
            "Pago m\u00ednimo", CreditCardStatement::getMinimumPaymentDue, CreditCardStatement::setMinimumPaymentDue
        );
        TableColumn<CreditCardStatement, String> dueDate = dateColumn(
            "Fecha l\u00edmite de pago", CreditCardStatement::getPaymentDueDate, CreditCardStatement::setPaymentDueDate, parseDate
        );
        TableColumn<CreditCardStatement, Double> previous = moneyColumn("Saldo anterior", CreditCardStatement::getPreviousBalance, CreditCardStatement::setPreviousBalance);
        TableColumn<CreditCardStatement, Double> payments = moneyColumn("Pagos", CreditCardStatement::getPayments, CreditCardStatement::setPayments);
        TableColumn<CreditCardStatement, Double> credits = moneyColumn("Cr\u00e9ditos", CreditCardStatement::getOtherCredits, CreditCardStatement::setOtherCredits);
        TableColumn<CreditCardStatement, Double> purchases = moneyColumn("Compras", CreditCardStatement::getTransactions, CreditCardStatement::setTransactions);
        TableColumn<CreditCardStatement, Double> cash = moneyColumn("Cash advances", CreditCardStatement::getCashAdvances, CreditCardStatement::setCashAdvances);
        TableColumn<CreditCardStatement, Double> fees = moneyColumn("Fees", CreditCardStatement::getFeesCharged, CreditCardStatement::setFeesCharged);
        TableColumn<CreditCardStatement, Double> interest = moneyColumn("Intereses", CreditCardStatement::getInterestCharged, CreditCardStatement::setInterestCharged);
        TableColumn<CreditCardStatement, Double> limit = moneyColumn("Limite banco", CreditCardStatement::getCreditLimit, CreditCardStatement::setCreditLimit);
        TableColumn<CreditCardStatement, Double> available = moneyColumn("Cr\u00e9dito disponible", CreditCardStatement::getAvailableCredit, CreditCardStatement::setAvailableCredit);
        TableColumn<CreditCardStatement, String> status = new TableColumn<>("Revisi\u00f3n");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<CreditCardStatement, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(TextFieldTableCell.forTableColumn());
        notes.setOnEditCommit(event -> event.getRowValue().setReviewNotes(event.getNewValue()));
        notes.setPrefWidth(260);
        TableColumn<CreditCardStatement, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    CreditCardStatement statement = getTableView().getItems().get(getIndex());
                    deleteStatement.accept(statement);
                    getTableView().getItems().remove(statement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });

        table.getColumns().setAll(
            year, month, startDate, endDate, newBalance, reviewed, minimum, dueDate, previous, payments,
            credits, purchases, cash, fees, interest, limit, available, status, notes, delete
        );
        return table;
    }

    private TableColumn<CreditCardStatement, Double> moneyColumn(
        String title,
        ToDoubleFunction<CreditCardStatement> getter,
        BiConsumer<CreditCardStatement, Double> setter
    ) {
        TableColumn<CreditCardStatement, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleDoubleProperty(getter.applyAsDouble(data.getValue())).asObject());
        column.setCellFactory(TextFieldTableCell.forTableColumn(twoDecimalConverter()));
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), event.getNewValue()));
        column.setPrefWidth(120);
        return column;
    }

    private TableColumn<CreditCardStatement, String> dateColumn(
        String title,
        Function<CreditCardStatement, LocalDate> getter,
        BiConsumer<CreditCardStatement, LocalDate> setter,
        Function<String, LocalDate> parseDate
    ) {
        TableColumn<CreditCardStatement, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(
            getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue()).toString()
        ));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), parseDate.apply(event.getNewValue())));
        column.setPrefWidth(120);
        return column;
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
}
