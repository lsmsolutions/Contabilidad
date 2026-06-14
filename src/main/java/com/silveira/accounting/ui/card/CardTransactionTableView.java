package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardTransaction;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class CardTransactionTableView {
    public TableView<CreditCardTransaction> build(
        BiConsumer<CreditCardTransaction, Boolean> updateReview,
        Consumer<CreditCardTransaction> deleteMovement,
        Function<String, LocalDate> parseDate,
        Callback<TableColumn<CreditCardTransaction, String>, TableCell<CreditCardTransaction, String>> stringCellFactory,
        Callback<TableColumn<CreditCardTransaction, Double>, TableCell<CreditCardTransaction, Double>> moneyCellFactory
    ) {
        TableView<CreditCardTransaction> table = new TableView<>();
        table.setEditable(true);

        TableColumn<CreditCardTransaction, String> transactionDate = dateColumn(
            "Fecha",
            CreditCardTransaction::getTransactionDate,
            (movement, date) -> {
                movement.setTransactionDate(date);
                movement.setPostDate(date);
            },
            parseDate,
            stringCellFactory
        );
        TableColumn<CreditCardTransaction, String> description = new TableColumn<>("Descripci\u00f3n");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(stringCellFactory);
        description.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));
        description.setPrefWidth(360);
        TableColumn<CreditCardTransaction, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(moneyCellFactory);
        amount.setOnEditCommit(event -> event.getRowValue().setAmount(event.getNewValue()));
        amount.setPrefWidth(110);
        TableColumn<CreditCardTransaction, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    CreditCardTransaction movement = getTableView().getItems().get(getIndex());
                    updateReview.accept(movement, checkBox.isSelected());
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
        TableColumn<CreditCardTransaction, String> type = new TableColumn<>("Tipo");
        type.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        type.setCellFactory(stringCellFactory);
        type.setOnEditCommit(event -> event.getRowValue().setType(event.getNewValue()));
        TableColumn<CreditCardTransaction, String> category = new TableColumn<>("Categor\u00eda");
        category.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        category.setCellFactory(stringCellFactory);
        category.setOnEditCommit(event -> event.getRowValue().setCategory(event.getNewValue()));
        TableColumn<CreditCardTransaction, String> status = new TableColumn<>("Revisi\u00f3n");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<CreditCardTransaction, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(stringCellFactory);
        notes.setOnEditCommit(event -> event.getRowValue().setReviewNotes(event.getNewValue()));
        notes.setPrefWidth(260);
        TableColumn<CreditCardTransaction, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    CreditCardTransaction movement = getTableView().getItems().get(getIndex());
                    deleteMovement.accept(movement);
                    getTableView().getItems().remove(movement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });

        table.getColumns().setAll(transactionDate, description, amount, reviewed, type, category, status, notes, delete);
        return table;
    }

    private TableColumn<CreditCardTransaction, String> dateColumn(
        String title,
        Function<CreditCardTransaction, LocalDate> getter,
        BiConsumer<CreditCardTransaction, LocalDate> setter,
        Function<String, LocalDate> parseDate,
        Callback<TableColumn<CreditCardTransaction, String>, TableCell<CreditCardTransaction, String>> cellFactory
    ) {
        TableColumn<CreditCardTransaction, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(
            getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue()).toString()
        ));
        column.setCellFactory(cellFactory);
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), parseDate.apply(event.getNewValue())));
        column.setPrefWidth(110);
        return column;
    }
}
