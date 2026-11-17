package com.silveira.accounting.ui.business;

import com.silveira.accounting.application.business.BusinessApplicationService;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.models.business.Business;
import com.silveira.accounting.models.business.BusinessExpense;
import com.silveira.accounting.utils.Money;
import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class BusinessWorkflow {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final BusinessApplicationService application;
    private final Config config;

    public BusinessWorkflow(BusinessApplicationService application, Config config) {
        this.application = application;
        this.config = config;
    }

    public List<Business> businesses() { return application.businesses(); }

    public void showBusinesses() {
        Button create = new Button("Create Business");
        create.getStyleClass().add("primary");
        create.setOnAction(event -> createBusiness());
        FlowPane cards = new FlowPane(14, 14);
        for (Business business : businesses()) {
            Button card = new Button(business.name());
            card.getStyleClass().addAll("hub-card", "business-card");
            card.setPrefSize(260, 82);
            card.setOnAction(event -> showExpenses(business));
            Button edit = new Button("Edit");
            edit.setOnAction(event -> editBusiness(business));
            Button delete = new Button("Delete");
            delete.getStyleClass().add("danger");
            delete.setOnAction(event -> deleteBusiness(business));
            HBox cardActions = new HBox(8, edit, delete);
            cardActions.setAlignment(Pos.CENTER);
            VBox businessCard = new VBox(8, card, cardActions);
            businessCard.setAlignment(Pos.CENTER);
            cards.getChildren().add(businessCard);
        }
        config.setPage().accept(page("Businesses", create, cards));
    }

    public void createBusiness() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Business");
        dialog.setHeaderText("New business");
        dialog.setContentText("Business name:");
        dialog.initOwner(config.owner().get());
        dialog.showAndWait().map(String::trim).filter(name -> !name.isBlank()).ifPresent(name -> {
            try {
                application.createBusiness(name);
                config.rebuildSidebar().run();
                showBusinesses();
            } catch (RuntimeException exception) {
                config.alert().show(Alert.AlertType.ERROR, "Could not create business", config.rootCauseMessage().apply(exception));
            }
        });
    }

    private void editBusiness(Business business) {
        TextInputDialog dialog = new TextInputDialog(business.name());
        dialog.setTitle("Edit Business");
        dialog.setHeaderText("Rename business");
        dialog.setContentText("Business name:");
        dialog.initOwner(config.owner().get());
        dialog.showAndWait().map(String::trim).filter(name -> !name.isBlank()).ifPresent(name -> {
            try {
                application.renameBusiness(business.id(), name);
                config.rebuildSidebar().run();
                showBusinesses();
            } catch (RuntimeException exception) {
                config.alert().show(Alert.AlertType.ERROR, "Could not edit business", config.rootCauseMessage().apply(exception));
            }
        });
    }

    private void deleteBusiness(Business business) {
        Alert confirmation = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Delete " + business.name() + " and all of its expenses?\n\nThis action cannot be undone.",
            ButtonType.YES,
            ButtonType.NO
        );
        confirmation.setTitle("Delete Business");
        confirmation.setHeaderText("Delete business");
        confirmation.initOwner(config.owner().get());
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            application.deleteBusiness(business.id());
            config.rebuildSidebar().run();
            showBusinesses();
        } catch (RuntimeException exception) {
            config.alert().show(Alert.AlertType.ERROR, "Could not delete business", config.rootCauseMessage().apply(exception));
        }
    }

    public void showExpenses(Business business) { showExpenses(business, null, null); }

    private void showExpenses(Business business, Integer selectedYear, Integer selectedMonth) {
        List<BusinessExpense> all = application.expenses(business.id(), null, null);
        int year = selectedYear != null ? selectedYear : all.stream().map(BusinessExpense::getExpenseDate).filter(java.util.Objects::nonNull).mapToInt(LocalDate::getYear).max().orElse(LocalDate.now().getYear());
        Runnable[] refreshSummaries = {() -> {}};
        TableView<BusinessExpense> table = expenseTable(business, () -> refreshSummaries[0].run());
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(application.expenses(business.id(), selectedMonth == null ? null : year, selectedMonth)));
        refresh.run();

        Label total = new Label(Money.format(total(table.getItems())));
        total.getStyleClass().add("mini-total-value");
        table.getItems().addListener((javafx.collections.ListChangeListener<BusinessExpense>) change -> total.setText(Money.format(total(table.getItems()))));
        Button add = new Button("Add Expense"); add.getStyleClass().add("primary");
        add.setOnAction(event -> {
            LocalDate date = selectedMonth == null ? LocalDate.now() : LocalDate.of(year, selectedMonth, 1);
            BusinessExpense expense = new BusinessExpense(0, business.id(), date, "Manual expense", "", 0, "", "", false, "", null, null);
            table.getItems().add(0, expense); table.getSelectionModel().select(expense); table.refresh();
        });
        Button save = new Button("Save Changes");
        save.setOnAction(event -> { table.getItems().forEach(this::save); table.refresh(); refreshSummaries[0].run(); });
        HBox actions = new HBox(12, add, save, totalBox(total)); actions.setAlignment(Pos.CENTER_LEFT);

        FlowPane months = new FlowPane(10, 10);
        refreshSummaries[0] = () -> {
            total.setText(Money.format(total(table.getItems())));
            populateMonthCards(months, business);
        };
        refreshSummaries[0].run();
        Button back = new Button("Back to Businesses"); back.setOnAction(event -> showBusinesses());
        config.setPage().accept(page(business.name() + " - Expenses", back, months, actions, table));
    }

    private Button monthButton(String title, String amount, Runnable action) {
        Button button = new Button(title + "\n" + amount); button.getStyleClass().addAll("monthly-card", "business-month-card");
        button.setPrefSize(190, 78); button.setOnAction(event -> action.run()); return button;
    }

    private void populateMonthCards(FlowPane months, Business business) {
        List<BusinessExpense> expenses = application.expenses(business.id(), null, null);
        months.getChildren().clear();
        months.getChildren().add(monthButton("General", Money.format(total(expenses)), () -> showExpenses(business, null, null)));
        expenses.stream().map(BusinessExpense::getExpenseDate).filter(java.util.Objects::nonNull)
            .map(date -> date.getYear() * 100 + date.getMonthValue()).distinct().sorted().forEach(period -> {
                int year = period / 100, month = period % 100;
                double amount = expenses.stream()
                    .filter(expense -> expense.getExpenseDate() != null && expense.getExpenseDate().getYear() == year && expense.getExpenseDate().getMonthValue() == month)
                    .mapToDouble(BusinessExpense::getAmount).sum();
                String title = java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + year;
                months.getChildren().add(monthButton(title, Money.format(amount), () -> showExpenses(business, year, month)));
            });
    }

    private HBox totalBox(Label total) { Label label = new Label("Total Expenses"); label.getStyleClass().add("mini-total-title"); HBox box = new HBox(14, label, total); box.getStyleClass().addAll("mini-total", "neutral-total"); return box; }
    private double total(List<BusinessExpense> expenses) { return expenses.stream().mapToDouble(BusinessExpense::getAmount).sum(); }
    private void save(BusinessExpense expense) { if (expense.getId() == 0) expense.setId(application.saveExpense(expense)); else application.updateExpense(expense); }

    private TableView<BusinessExpense> expenseTable(Business business, Runnable dataChanged) {
        TableView<BusinessExpense> table = new TableView<>(); table.setEditable(true); table.setMinWidth(1320); table.setPrefHeight(520);
        TableColumn<BusinessExpense,String> date = textColumn("Date\nMM/DD/YYYY", 120, e -> e.getExpenseDate() == null ? "" : DATE.format(e.getExpenseDate()), (e,v) -> e.setExpenseDate(parseDate(v)), dataChanged);
        TableColumn<BusinessExpense,String> description = textColumn("Description", 220, BusinessExpense::getDescription, BusinessExpense::setDescription, dataChanged);
        TableColumn<BusinessExpense,String> provider = textColumn("Provider", 160, BusinessExpense::getProvider, BusinessExpense::setProvider, dataChanged);
        TableColumn<BusinessExpense,String> amount = textColumn("Amount", 100, e -> Money.format(e.getAmount()), (e,v) -> e.setAmount(Money.parse(v)), dataChanged);
        TableColumn<BusinessExpense,String> invoice = textColumn("Invoice", 110, BusinessExpense::getInvoice, BusinessExpense::setInvoice, dataChanged);
        TableColumn<BusinessExpense,String> paid = new TableColumn<>("Paid With"); paid.setPrefWidth(150);
        paid.setCellValueFactory(d -> new SimpleStringProperty(value(d.getValue().getPaymentSource())));
        paid.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(paymentOptions())));
        paid.setOnEditCommit(e -> { e.getRowValue().setPaymentSource(e.getNewValue()); save(e.getRowValue()); dataChanged.run(); });
        TableColumn<BusinessExpense,Boolean> reviewed = new TableColumn<>("Reviewed"); reviewed.setPrefWidth(85);
        reviewed.setUserData("skip-tab");
        reviewed.setCellValueFactory(d -> { SimpleBooleanProperty p = new SimpleBooleanProperty(d.getValue().isReviewed()); p.addListener((o,a,b) -> { d.getValue().setReviewed(b); save(d.getValue()); dataChanged.run(); }); return p; });
        reviewed.setCellFactory(CheckBoxTableCell.forTableColumn(reviewed));
        TableColumn<BusinessExpense,String> notes = textColumn("Notes", 160, BusinessExpense::getNotes, BusinessExpense::setNotes, dataChanged);
        TableColumn<BusinessExpense,Void> document = new TableColumn<>("Document"); document.setPrefWidth(145);
        document.setEditable(false);
        document.setCellFactory(c -> new TableCell<>() { final Button button = new Button(); { button.setOnAction(e -> { documentAction(getTableView().getItems().get(getIndex()), button); dataChanged.run(); }); } @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : button); if (!empty) button.setText(hasDocument(getTableView().getItems().get(getIndex())) ? "Open" : "Attach"); } });
        TableColumn<BusinessExpense,Void> actions = new TableColumn<>("Actions"); actions.setPrefWidth(190);
        actions.setEditable(false);
        actions.setCellFactory(c -> new TableCell<>() { final Button saveButton = new Button("Save"); final Button edit = new Button("Edit"); final Button delete = new Button("Delete"); final HBox box = new HBox(5, saveButton, edit, delete); { saveButton.setOnAction(e -> { save(current()); getTableView().refresh(); dataChanged.run(); }); edit.setOnAction(e -> { getTableView().getSelectionModel().select(getIndex()); getTableView().edit(getIndex(), date); }); delete.setOnAction(e -> { BusinessExpense x=current(); Alert a=new Alert(Alert.AlertType.CONFIRMATION,"Delete this expense?",ButtonType.YES,ButtonType.NO); a.initOwner(config.owner().get()); if(a.showAndWait().orElse(ButtonType.NO)==ButtonType.YES){ if(x.getId()>0)application.deleteExpense(x.getId()); getTableView().getItems().remove(x); dataChanged.run(); }}); } private BusinessExpense current(){return getTableView().getItems().get(getIndex());} @Override protected void updateItem(Void item,boolean empty){super.updateItem(item,empty);setGraphic(empty?null:box);} });
        table.getColumns().addAll(date, description, provider, amount, invoice, paid, reviewed, notes, document, actions);
        return table;
    }

    private TableColumn<BusinessExpense,String> textColumn(String title, double width, Function<BusinessExpense,String> get, java.util.function.BiConsumer<BusinessExpense,String> set, Runnable dataChanged) {
        TableColumn<BusinessExpense,String> column = new TableColumn<>(title); column.setPrefWidth(width);
        column.setCellValueFactory(d -> new SimpleStringProperty(value(get.apply(d.getValue()))));
        column.setCellFactory(commitOnFocusLostCellFactory());
        column.setOnEditCommit(e -> { set.accept(e.getRowValue(), e.getNewValue()); save(e.getRowValue()); dataChanged.run(); }); return column;
    }

    private javafx.util.Callback<TableColumn<BusinessExpense, String>, TableCell<BusinessExpense, String>> commitOnFocusLostCellFactory() {
        return column -> new TableCell<>() {
            private TextField field;

            @Override public void startEdit() {
                if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) return;
                super.startEdit();
                if (field == null) {
                    field = new TextField();
                    field.setOnAction(event -> commitCurrentEdit());
                    field.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ESCAPE) {
                            cancelEdit();
                        } else if (event.getCode() == KeyCode.TAB) {
                            commitCurrentEdit();
                            moveToAdjacentEditableCell(this, event.isShiftDown());
                            event.consume();
                        }
                    });
                    field.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                        if (wasFocused && !isFocused && isEditing()) commitCurrentEdit();
                    });
                }
                field.setText(value(getItem())); setText(null); setGraphic(field); field.selectAll(); field.requestFocus();
            }

            @Override public void cancelEdit() { super.cancelEdit(); setText(value(getItem())); setGraphic(null); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); setGraphic(null); }
                else if (isEditing()) { field.setText(value(item)); setText(null); setGraphic(field); }
                else { setText(value(item)); setGraphic(null); }
            }
            private void commitCurrentEdit() {
                try { commitEdit(field.getText()); }
                catch (RuntimeException exception) {
                    cancelEdit();
                    config.alert().show(Alert.AlertType.ERROR, "Invalid value", exception.getMessage() == null ? "Check the value and try again." : exception.getMessage());
                }
            }
        };
    }

    private void moveToAdjacentEditableCell(TableCell<?, ?> cell, boolean reverse) {
        TableView<?> table = cell.getTableView();
        if (table == null || table.getItems().isEmpty()) return;
        List<? extends TableColumn<?, ?>> columns = table.getVisibleLeafColumns().stream()
            .filter(TableColumn::isEditable)
            .filter(column -> !"skip-tab".equals(column.getUserData()))
            .toList();
        int column = columns.indexOf(cell.getTableColumn());
        int row = cell.getIndex();
        int next = column + (reverse ? -1 : 1);
        if (next >= columns.size()) { next = 0; row++; }
        else if (next < 0) { next = columns.size() - 1; row--; }
        if (row < 0 || row >= table.getItems().size()) return;
        editCell(table, row, columns.get(next));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void editCell(TableView<?> table, int row, TableColumn<?, ?> column) {
        Platform.runLater(() -> {
            table.getSelectionModel().clearAndSelect(row, (TableColumn) column);
            table.scrollTo(row);
            ((TableView) table).edit(row, (TableColumn) column);
        });
    }

    private List<String> paymentOptions() { List<String> values = new ArrayList<>(); values.add(""); values.add("Cash"); for(BankAccount a:config.bankAccounts().get())values.add("Account: "+a.getAlias()); for(CreditCardAccount a:config.cardAccounts().get())values.add("Card: "+a.getAlias()); return values; }
    private void documentAction(BusinessExpense expense, Button button) { try { if(hasDocument(expense)){Desktop.getDesktop().open(new File(expense.getDocumentPath()));return;} FileChooser chooser=new FileChooser(); chooser.setTitle("Attach Document"); File file=chooser.showOpenDialog(config.owner().get()); if(file!=null){expense.setDocumentPath(file.getAbsolutePath());expense.setDocumentName(file.getName());save(expense);button.setText("Open");} } catch(Exception e){config.alert().show(Alert.AlertType.ERROR,"Document error",config.rootCauseMessage().apply(e));} }
    private boolean hasDocument(BusinessExpense e){return e.getDocumentPath()!=null&&!e.getDocumentPath().isBlank()&&new File(e.getDocumentPath()).exists();}
    private LocalDate parseDate(String text){try{return LocalDate.parse(text.trim(),DATE);}catch(Exception e){throw new IllegalArgumentException("Use MM/DD/YYYY");}}
    private String value(String value){return value==null?"":value;}
    private VBox page(String title, javafx.scene.Node... nodes){Label heading=new Label(title);heading.getStyleClass().add("heading");VBox box=new VBox(18);box.setPadding(new Insets(28));box.getStyleClass().addAll("page","horizontal-page");box.getChildren().add(heading);box.getChildren().addAll(nodes);VBox.setVgrow(nodes[nodes.length-1], Priority.ALWAYS);return box;}

    public record Config(Supplier<Window> owner, Supplier<List<BankAccount>> bankAccounts, Supplier<List<CreditCardAccount>> cardAccounts, Consumer<Parent> setPage, Runnable rebuildSidebar, AlertAction alert, Function<Throwable,String> rootCauseMessage) {}
    @FunctionalInterface public interface AlertAction { void show(Alert.AlertType type, String title, String message); }
}
