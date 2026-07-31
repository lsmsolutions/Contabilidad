package com.silveira.accounting.ui;

import com.silveira.accounting.utils.Money;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class AppUiSupport {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Supplier<Window> owner;
    private final Consumer<Parent> setPage;
    private final Consumer<Runnable> guardedRun;

    public AppUiSupport(Supplier<Window> owner, Consumer<Parent> setPage, Consumer<Runnable> guardedRun) {
        this.owner = owner;
        this.setPage = setPage;
        this.guardedRun = guardedRun;
    }

    public ScrollPane horizontalStatementScroll(Node statementCard) {
        ScrollPane scroll = new ScrollPane(statementCard);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setMaxWidth(Double.MAX_VALUE);
        return scroll;
    }

    public VBox dashboardSection(String title, String subtitle, String styleClass, VBox... cards) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        Label detail = new Label(subtitle);
        detail.getStyleClass().add("section-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        for (int i = 0; i < cards.length; i++) {
            grid.add(cards[i], i % 3, i / 3);
        }

        VBox section = new VBox(12, new VBox(2, heading, detail), grid);
        section.getStyleClass().addAll("dashboard-section", styleClass);
        return section;
    }

    public VBox textCard(String title, String value, String sourceClass) {
        Label label = new Label(title);
        label.getStyleClass().add("card-title");
        Label amount = new Label(value);
        amount.getStyleClass().add("card-value");
        VBox card = new VBox(8, label, amount);
        card.getStyleClass().addAll("metric-card", sourceClass);
        card.setMinWidth(250);
        return card;
    }

    public VBox monthlyActionCard(String title, String line1, String line2, String line3, String line4, Runnable action) {
        Label heading = new Label(title);
        heading.getStyleClass().add("monthly-card-title");
        GridPane lines = new GridPane();
        lines.getStyleClass().add("monthly-card-grid");
        int row = 0;
        for (String line : List.of(line1, line2, line3, line4)) {
            if (!line.isBlank()) {
                row = addMonthlyCardGridLine(lines, row, line);
            }
        }
        VBox box = new VBox(0, heading, lines);
        box.setOnMouseClicked(event -> action.run());
        return box;
    }

    public void addMonthlyCardDivider(VBox card) {
        if (card.getChildren().isEmpty() || !(card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid)) {
            return;
        }
        Separator separator = new Separator();
        separator.getStyleClass().add("monthly-card-divider");
        grid.add(separator, 0, nextGridRow(grid), 2, 1);
    }

    public void addMonthlyCardLine(VBox card, String text) {
        addMonthlyCardLine(card, text, "monthly-card-line");
    }

    public void addMonthlyCardLine(VBox card, String text, String styleClass) {
        if (text == null || text.isBlank()) {
            return;
        }
        text = text.replaceFirst("\\s+.*Dif\\.:.*$", "");
        if ("monthly-card-line".equals(styleClass) && !card.getChildren().isEmpty() && card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid) {
            addMonthlyCardGridLine(grid, nextGridRow(grid), text);
            return;
        }
        if (styleClass.startsWith("monthly-card-value-") && !card.getChildren().isEmpty() && card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid) {
            addMonthlyCardGridLine(grid, nextGridRow(grid), text, styleClass);
            return;
        }
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        card.getChildren().add(label);
    }

    public Button monthlyExportButton(Runnable action) {
        Button button = new Button("Descargar mes");
        button.setOnAction(event -> {
            event.consume();
            action.run();
        });
        return button;
    }

    public VBox miniTotal(String title, String value, String styleClass) {
        Label label = new Label(title);
        label.getStyleClass().add("mini-total-title");
        Label amount = new Label(value);
        amount.getStyleClass().add("mini-total-value");
        VBox box = new VBox(4, label, amount);
        box.getStyleClass().addAll("mini-total", styleClass);
        return box;
    }

    public Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    public String monthName(int month) {
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

    public void showProcessing(String title, String message) {
        showProcessing(title, message, null);
    }

    public void showProcessing(String title, String message, Runnable cancelAction) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(64, 64);
        Label heading = new Label(title);
        heading.getStyleClass().add("processing-title");
        Label detail = new Label(message);
        detail.getStyleClass().add("processing-detail");
        detail.setWrapText(true);
        VBox box = new VBox(16, progress, heading, detail);
        if (cancelAction != null) {
            Button cancel = new Button("Cancelar");
            cancel.setOnAction(event -> cancelAction.run());
            box.getChildren().add(cancel);
        }
        box.getStyleClass().add("processing-box");
        box.setAlignment(Pos.CENTER);
        setPage.accept(page(title, box));
    }

    public File choosePdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        return chooser.showOpenDialog(owner.get());
    }

    public File chooseExcel(String initialFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar Excel mensual");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        chooser.setInitialFileName(initialFileName);
        return chooser.showSaveDialog(owner.get());
    }

    public String text(String value) {
        return value == null ? "" : value;
    }

    public void refreshRowsChanged(Runnable rowsChanged) {
        if (rowsChanged != null) {
            rowsChanged.run();
        }
    }

    public LocalDate parseDateOrNull(String value) {
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

    public String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    public String safeFileName(String value) {
        return value == null || value.isBlank() ? "general" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public StringConverter<Double> twoDecimalConverter() {
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

    public StringConverter<String> stringConverter() {
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

    public <S, T> javafx.util.Callback<TableColumn<S, T>, TableCell<S, T>> commitOnFocusLostCellFactory(
        StringConverter<T> converter,
        AlertAction alert
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
                    alert.show(Alert.AlertType.ERROR, "Valor no valido", "Revisa el valor introducido antes de guardar.");
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

    public VBox page(String title, Node... nodes) {
        Label heading = new Label(title);
        heading.getStyleClass().add("heading");
        VBox box = new VBox(18);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("page");
        return box;
    }

    public void setDarkHubPage(String title, Node... nodes) {
        VBox hub = page(title, nodes);
        hub.getStyleClass().add("dark-hub-page");
        setPage.accept(hub);
    }

    public Button backButton(String text, Runnable action) {
        Button button = new Button("\u2190 " + text);
        button.getStyleClass().add("back-button");
        button.setOnAction(event -> guardedRun.accept(action));
        return button;
    }

    public VBox actionHeader(Node... rows) {
        VBox header = new VBox(10);
        header.getStyleClass().add("action-header");
        for (Node row : rows) {
            if (row instanceof HBox hBox) {
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getStyleClass().add("action-row");
            }
            header.getChildren().add(row);
        }
        return header;
    }

    public Label helperNote(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-subtitle");
        label.setWrapText(true);
        return label;
    }

    public void alert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(560);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    public String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    public boolean confirm(String title, String message, String confirmText) {
        ButtonType confirm = new ButtonType(confirmText);
        Alert alert = new Alert(Alert.AlertType.WARNING, message, confirm, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getDialogPane().setMinWidth(620);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    public Optional<String> promptText(String title, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(message);
        dialog.getDialogPane().setMinWidth(560);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return dialog.showAndWait();
    }

    private int addMonthlyCardGridLine(GridPane grid, int row, String text) {
        return addMonthlyCardGridLine(grid, row, text, null);
    }

    private int addMonthlyCardGridLine(GridPane grid, int row, String text, String valueStyleClass) {
        int separator = text.indexOf(": ");
        if (separator <= 0) {
            Label label = new Label(text);
            label.getStyleClass().add("monthly-card-line");
            grid.add(label, 0, row, 2, 1);
            return row + 1;
        }
        Label label = new Label(text.substring(0, separator));
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(text.substring(separator + 2));
        value.getStyleClass().add("monthly-card-value");
        if (valueStyleClass != null && !valueStyleClass.isBlank()) {
            value.getStyleClass().add(valueStyleClass);
        }
        grid.add(label, 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }

    private int nextGridRow(GridPane grid) {
        return grid.getChildren().stream()
            .map(GridPane::getRowIndex)
            .mapToInt(row -> row == null ? 0 : row)
            .max()
            .orElse(-1) + 1;
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }
}
