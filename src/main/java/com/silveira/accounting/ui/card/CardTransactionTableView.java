package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardTransaction;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

public class CardTransactionTableView {
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final long TOTAL_ROW_STATEMENT_ID = Long.MIN_VALUE;
    private static final long BLOCK_ROW_STATEMENT_ID = Long.MIN_VALUE + 1;
    private static final long BLOCK_TOTAL_ROW_STATEMENT_ID = Long.MIN_VALUE + 2;
    private static final String CAPITAL_ONE_BLOCK_TOTALS = "capitalOneBlockTotals";

    public TableView<CreditCardTransaction> build(
        BiConsumer<CreditCardTransaction, Boolean> updateReview,
        Consumer<CreditCardTransaction> deleteMovement,
        Function<String, LocalDate> parseDate,
        Callback<TableColumn<CreditCardTransaction, String>, TableCell<CreditCardTransaction, String>> stringCellFactory,
        Callback<TableColumn<CreditCardTransaction, Double>, TableCell<CreditCardTransaction, Double>> moneyCellFactory
    ) {
        TableView<CreditCardTransaction> table = new TableView<>();
        table.setEditable(true);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.setRowFactory(view -> {
            javafx.scene.control.TableRow<CreditCardTransaction> row = new javafx.scene.control.TableRow<>();
            row.itemProperty().addListener((observable, oldItem, newItem) -> {
                row.getStyleClass().remove("card-transaction-total-row");
                row.getStyleClass().remove("card-transaction-block-row");
                if (isTotalRow(newItem)) {
                    row.getStyleClass().add("card-transaction-total-row");
                } else if (isBlockRow(newItem)) {
                    row.getStyleClass().add("card-transaction-block-row");
                }
            });
            return row;
        });

        TableColumn<CreditCardTransaction, String> transactionDate = dateColumn(
            "Fecha\n(MM/dd/yyyy)",
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
        description.setOnEditCommit(event -> {
            if (!isSyntheticRow(event.getRowValue())) {
                event.getRowValue().setDescription(event.getNewValue());
            }
        });
        description.setPrefWidth(360);
        TableColumn<CreditCardTransaction, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> isBlockRow(data.getValue())
            ? new SimpleObjectProperty<Double>(null)
            : new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(moneyCellFactory);
        amount.setOnEditCommit(event -> {
            if (!isSyntheticRow(event.getRowValue())) {
                event.getRowValue().setAmount(event.getNewValue());
                refreshTotalRow(table);
            }
        });
        amount.setPrefWidth(110);
        TableColumn<CreditCardTransaction, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setEditable(false);
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    CreditCardTransaction movement = currentMovement(this);
                    if (movement == null || isSyntheticRow(movement)) {
                        return;
                    }
                    updateReview.accept(movement, checkBox.isSelected());
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || isSyntheticRow(currentMovement(this))) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        TableColumn<CreditCardTransaction, String> type = new TableColumn<>("Tipo");
        type.setCellValueFactory(data -> new SimpleStringProperty(
            isSyntheticRow(data.getValue()) ? "" : normalizeType(data.getValue().getType())
        ));
        type.setCellFactory(stringCellFactory);
        type.setOnEditCommit(event -> {
            if (!isSyntheticRow(event.getRowValue())) {
                event.getRowValue().setType(normalizeType(event.getNewValue()));
            }
        });
        TableColumn<CreditCardTransaction, String> category = new TableColumn<>("Categor\u00eda");
        category.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        category.setCellFactory(stringCellFactory);
        category.setOnEditCommit(event -> {
            if (!isSyntheticRow(event.getRowValue())) {
                event.getRowValue().setCategory(event.getNewValue());
            }
        });
        TableColumn<CreditCardTransaction, String> status = new TableColumn<>("Revisi\u00f3n");
        status.setEditable(false);
        status.setCellValueFactory(data -> new SimpleStringProperty(isSyntheticRow(data.getValue()) ? "" : data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<CreditCardTransaction, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(stringCellFactory);
        notes.setOnEditCommit(event -> {
            if (!isSyntheticRow(event.getRowValue())) {
                event.getRowValue().setReviewNotes(event.getNewValue());
            }
        });
        notes.setPrefWidth(170);
        TableColumn<CreditCardTransaction, Void> delete = new TableColumn<>("Eliminar");
        delete.setEditable(false);
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    CreditCardTransaction movement = currentMovement(this);
                    if (movement == null || isSyntheticRow(movement)) {
                        return;
                    }
                    deleteMovement.accept(movement);
                    getTableView().getItems().remove(movement);
                    refreshTotalRow(getTableView());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || isSyntheticRow(currentMovement(this)) ? null : button);
            }
        });
        delete.setPrefWidth(95);

        table.getColumns().setAll(transactionDate, description, amount, reviewed, type, category, status, notes, delete);
        return table;
    }

    public static ObservableList<CreditCardTransaction> withTotalRow(List<CreditCardTransaction> movements) {
        return FXCollections.observableArrayList(rowsWithGroupedTotals(movements));
    }

    public static ObservableList<CreditCardTransaction> withCapitalOneBlockTotals(List<CreditCardTransaction> movements) {
        return FXCollections.observableArrayList(rowsWithCapitalOneBlockTotals(movements));
    }

    public static List<CreditCardTransaction> withoutTotalRow(List<CreditCardTransaction> movements) {
        return movements.stream().filter(movement -> !isSyntheticRow(movement)).toList();
    }

    public static void setCapitalOneBlockTotals(TableView<CreditCardTransaction> table, boolean enabled) {
        table.getProperties().put(CAPITAL_ONE_BLOCK_TOTALS, enabled);
    }

    public static void addBeforeTotal(TableView<CreditCardTransaction> table, CreditCardTransaction movement) {
        int totalIndex = table.getItems().size();
        if (totalIndex > 0 && isTotalRow(table.getItems().get(totalIndex - 1))) {
            totalIndex--;
        }
        table.getItems().add(totalIndex, movement);
        refreshTotalRow(table);
        focusFirstEditableCell(table, movement);
    }

    public static boolean isTotalRow(CreditCardTransaction movement) {
        return movement != null
            && (movement.getStatementId() == TOTAL_ROW_STATEMENT_ID || movement.getStatementId() == BLOCK_TOTAL_ROW_STATEMENT_ID);
    }

    private static boolean isBlockRow(CreditCardTransaction movement) {
        return movement != null && movement.getStatementId() == BLOCK_ROW_STATEMENT_ID;
    }

    private static boolean isSyntheticRow(CreditCardTransaction movement) {
        return isTotalRow(movement) || isBlockRow(movement);
    }

    private static void refreshTotalRow(TableView<CreditCardTransaction> table) {
        List<CreditCardTransaction> movements = withoutTotalRow(table.getItems());
        table.getItems().setAll(usesCapitalOneBlockTotals(table) ? rowsWithCapitalOneBlockTotals(movements) : rowsWithTotalRow(movements));
        table.refresh();
    }

    private static List<CreditCardTransaction> rowsWithTotalRow(List<CreditCardTransaction> movements) {
        List<CreditCardTransaction> rows = new java.util.ArrayList<>(movements);
        rows.add(totalRow(rows, "Total", TOTAL_ROW_STATEMENT_ID));
        return rows;
    }

    private static List<CreditCardTransaction> rowsWithGroupedTotals(List<CreditCardTransaction> movements) {
        List<CreditCardTransaction> paymentRows = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .filter(CardTransactionTableView::isPaymentCreditOrAdjustment)
            .toList();
        List<CreditCardTransaction> feeRows = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .filter(movement -> !isPaymentCreditOrAdjustment(movement))
            .filter(CardTransactionTableView::isFeeOrInterest)
            .toList();
        List<CreditCardTransaction> purchaseRows = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .filter(movement -> !isPaymentCreditOrAdjustment(movement))
            .filter(movement -> !isFeeOrInterest(movement))
            .filter(movement -> !isCashAdvance(movement))
            .toList();
        List<CreditCardTransaction> cashAdvanceRows = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .filter(movement -> !isPaymentCreditOrAdjustment(movement))
            .filter(movement -> !isFeeOrInterest(movement))
            .filter(CardTransactionTableView::isCashAdvance)
            .toList();

        List<CreditCardTransaction> rows = new java.util.ArrayList<>();
        addBlock(rows, "Payments, Credits and Adjustments", paymentRows);
        addBlock(rows, "Standard Purchases", purchaseRows);
        addBlock(rows, "Cash Advances", cashAdvanceRows);
        addBlock(rows, "Fees and Interest", feeRows);
        return rows.isEmpty() ? rowsWithTotalRow(movements) : rows;
    }

    private static List<CreditCardTransaction> rowsWithCapitalOneBlockTotals(List<CreditCardTransaction> movements) {
        List<CreditCardTransaction> rewardRows = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .filter(CardTransactionTableView::isCapitalOneReward)
            .toList();
        List<CreditCardTransaction> paymentRows = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .filter(movement -> !isCapitalOneReward(movement))
            .filter(CardTransactionTableView::isCapitalOnePaymentOrCredit)
            .toList();
        List<CreditCardTransaction> transactionRows = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .filter(movement -> !isCapitalOneReward(movement))
            .filter(movement -> !isCapitalOnePaymentOrCredit(movement))
            .toList();
        List<CreditCardTransaction> rows = new java.util.ArrayList<>();
        addBlock(rows, "Rewards", rewardRows);
        addBlock(rows, "Payments, Credits and Adjustments", paymentRows);
        addBlock(rows, "Standard Purchases", transactionRows);
        return rows;
    }

    private static void addBlock(List<CreditCardTransaction> rows, String title, List<CreditCardTransaction> movements) {
        if (movements.isEmpty()) {
            return;
        }
        rows.add(blockRow(title));
        rows.addAll(movements);
        rows.add(totalRow(movements, "Total " + title, BLOCK_TOTAL_ROW_STATEMENT_ID));
    }

    private static boolean isCapitalOnePaymentOrCredit(CreditCardTransaction movement) {
        String type = normalizeType(movement.getType()).toLowerCase(Locale.ROOT);
        String description = text(movement.getDescription()).toLowerCase(Locale.ROOT);
        return type.equals("pago")
            || type.equals("credito")
            || description.contains("payment")
            || description.contains("pymt")
            || description.contains("credit")
            || description.contains("cash back reward");
    }

    private static boolean isCapitalOneReward(CreditCardTransaction movement) {
        String description = text(movement.getDescription()).toLowerCase(Locale.ROOT);
        return description.contains("cash back reward") || description.contains("cashback reward");
    }

    private static boolean isPaymentCreditOrAdjustment(CreditCardTransaction movement) {
        String type = normalizeType(movement.getType()).toLowerCase(Locale.ROOT);
        String description = text(movement.getDescription()).toLowerCase(Locale.ROOT);
        return type.equals("pago")
            || type.equals("credito")
            || type.equals("payment")
            || type.equals("credit")
            || movement.getAmount() < 0
            || description.contains("payment")
            || description.contains("pymt")
            || description.contains("credit")
            || description.contains("cash back")
            || description.contains("cashback")
            || description.contains("reward")
            || description.contains("thankyou points");
    }

    private static boolean isFeeOrInterest(CreditCardTransaction movement) {
        String type = normalizeType(movement.getType()).toLowerCase(Locale.ROOT);
        String description = text(movement.getDescription()).toLowerCase(Locale.ROOT);
        return type.equals("fee")
            || type.equals("interes")
            || type.equals("interest")
            || description.contains("fee")
            || description.contains("interest");
    }

    private static boolean isCashAdvance(CreditCardTransaction movement) {
        String type = normalizeType(movement.getType()).toLowerCase(Locale.ROOT);
        String description = text(movement.getDescription()).toLowerCase(Locale.ROOT);
        return type.equals("cash advance") || description.contains("cash advance");
    }

    private static String normalizeType(String value) {
        String normalized = text(value).trim();
        return normalized.equalsIgnoreCase("compra") ? "purchase" : normalized;
    }

    private static CreditCardTransaction blockRow(String title) {
        CreditCardTransaction row = new CreditCardTransaction(0, BLOCK_ROW_STATEMENT_ID, null, null, title, 0, "", "");
        row.setPendingReview(false);
        row.setReviewRequired(false);
        row.setReviewNotes("");
        return row;
    }

    private static CreditCardTransaction totalRow(List<CreditCardTransaction> movements, String label, long statementId) {
        double total = movements.stream()
            .filter(movement -> !isSyntheticRow(movement))
            .mapToDouble(CreditCardTransaction::getAmount)
            .sum();
        CreditCardTransaction row = new CreditCardTransaction(0, statementId, null, null, label, total, "", "");
        row.setPendingReview(false);
        row.setReviewRequired(false);
        row.setReviewNotes("");
        return row;
    }

    private static boolean usesCapitalOneBlockTotals(TableView<CreditCardTransaction> table) {
        return Boolean.TRUE.equals(table.getProperties().get(CAPITAL_ONE_BLOCK_TOTALS));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void focusFirstEditableCell(TableView<CreditCardTransaction> table, CreditCardTransaction movement) {
        Platform.runLater(() -> {
            int rowIndex = table.getItems().indexOf(movement);
            if (rowIndex < 0) {
                return;
            }
            TableColumn<CreditCardTransaction, ?> firstEditable = table.getVisibleLeafColumns().stream()
                .filter(TableColumn::isEditable)
                .findFirst()
                .orElse(null);
            if (firstEditable == null) {
                table.getSelectionModel().select(movement);
                return;
            }
            table.getSelectionModel().clearAndSelect(rowIndex, firstEditable);
            table.scrollTo(rowIndex);
            table.requestFocus();
            ((TableView) table).edit(rowIndex, (TableColumn) firstEditable);
        });
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
            isSyntheticRow(data.getValue()) ? "" : formatDate(getter.apply(data.getValue()))
        ));
        column.setCellFactory(cellFactory);
        column.setOnEditCommit(event -> {
            if (!isSyntheticRow(event.getRowValue())) {
                setter.accept(event.getRowValue(), parseDate.apply(event.getNewValue()));
            }
        });
        column.setPrefWidth(110);
        return column;
    }

    private static CreditCardTransaction currentMovement(TableCell<CreditCardTransaction, ?> cell) {
        TableView<CreditCardTransaction> table = cell.getTableView();
        int index = cell.getIndex();
        if (table == null || index < 0 || index >= table.getItems().size()) {
            return null;
        }
        return table.getItems().get(index);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE_FORMAT);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
