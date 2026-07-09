package com.silveira.accounting.ui;

import com.silveira.accounting.models.CreditCardTransaction;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Callback;

import java.io.File;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.Optional;

public class AppViewActions {
    private Delegate delegate;

    public void connect(Delegate delegate) {
        this.delegate = delegate;
    }

    public Integer selectedYearValue() {
        return requireDelegate().selectedYearValue().get();
    }

    public Integer selectedMonthValue() {
        return requireDelegate().selectedMonthValue().get();
    }

    public void setSelectedYearValue(Integer value) {
        requireDelegate().setSelectedYearValue().accept(value);
    }

    public void setSelectedMonthValue(Integer value) {
        requireDelegate().setSelectedMonthValue().accept(value);
    }

    public void setPage(Parent page) {
        requireDelegate().setPage().accept(page);
    }

    public Parent page(String title, Node... nodes) {
        return requireDelegate().page().build(title, nodes);
    }

    public void setDarkHubPage(String title, Node... nodes) {
        requireDelegate().darkHub().show(title, nodes);
    }

    public void rebuildSidebar() {
        requireDelegate().rebuildSidebar().run();
    }

    public Window owner() {
        return requireDelegate().owner().get();
    }

    public String selectedBankAccountAlias() {
        return requireDelegate().selectedBankAccountAlias().get();
    }

    public void setSelectedBankAccountAlias(String alias) {
        requireDelegate().setSelectedBankAccountAlias().accept(alias);
    }

    public void alert(Alert.AlertType type, String title, String message) {
        requireDelegate().alert().show(type, title, message);
    }

    public String rootCauseMessage(Throwable throwable) {
        return requireDelegate().rootCauseMessage().apply(throwable);
    }

    public void showProcessing(String title, String message) {
        requireDelegate().showProcessing().accept(title, message);
    }

    public void showProcessing(String title, String message, Runnable cancelAction) {
        requireDelegate().showProcessingWithCancel().show(title, message, cancelAction);
    }

    public boolean confirm(String title, String message, String confirmText) {
        return requireDelegate().confirm().confirm(title, message, confirmText);
    }

    public Optional<String> promptText(String title, String message) {
        return requireDelegate().promptText().apply(title, message);
    }

    public void showMortgages() {
        requireDelegate().showMortgages().run();
    }

    public void showNylHub() {
        requireDelegate().showNylHub().run();
    }

    public Button backButton(String text, Runnable action) {
        return requireDelegate().backButton().create(text, action);
    }

    public File choosePdf() {
        return requireDelegate().choosePdf().choose();
    }

    public File chooseExcel(String initialFileName) {
        return requireDelegate().chooseExcel().choose(initialFileName);
    }

    public void addReviewMark(VBox card, String source, String accountAlias, int year, int month) {
        requireDelegate().addReviewMark().add(card, source, accountAlias, year, month);
    }

    public String monthName(int month) {
        return requireDelegate().monthName().apply(month);
    }

    public void selectedPeriodChanged(Integer year, Integer month) {
        requireDelegate().selectedPeriodChanged().accept(year, month);
    }

    public javafx.scene.control.Label helperNote(String text) {
        return requireDelegate().helperNote().apply(text);
    }

    public Node reviewMarkLabel(String source, String accountAlias, int year, int month) {
        return requireDelegate().reviewMarkLabel().apply(source, accountAlias, year, month);
    }

    public LocalDate parseDate(String value) {
        return requireDelegate().parseDate().apply(value);
    }

    public String safeFileName(String value) {
        return requireDelegate().safeFileName().apply(value);
    }

    public void importingChanged(Boolean importing) {
        requireDelegate().importingChanged().accept(importing);
    }

    public Callback<TableColumn<CreditCardTransaction, String>, TableCell<CreditCardTransaction, String>> stringCellFactory() {
        return requireDelegate().stringCellFactory().get();
    }

    public TableCell<CreditCardTransaction, String> stringCellFactory(TableColumn<CreditCardTransaction, String> column) {
        return stringCellFactory().call(column);
    }

    public Callback<TableColumn<CreditCardTransaction, Double>, TableCell<CreditCardTransaction, Double>> moneyCellFactory() {
        return requireDelegate().moneyCellFactory().get();
    }

    public TableCell<CreditCardTransaction, Double> moneyCellFactory(TableColumn<CreditCardTransaction, Double> column) {
        return moneyCellFactory().call(column);
    }

    public void showReview(String title, TableView<?> table, Runnable confirm, Node warningNode) {
        requireDelegate().reviewPresenter().show(title, table, confirm, warningNode);
    }

    public VBox monthlyActionCard(String title, String line1, String line2, String line3, String line4, Runnable action) {
        return requireDelegate().monthlyActionCard().create(title, line1, line2, line3, line4, action);
    }

    public void addMonthlyCardLine(VBox card, String text, String styleClass) {
        requireDelegate().addMonthlyCardLine().add(card, text, styleClass);
    }

    public void addMonthlyCardDivider(VBox card) {
        requireDelegate().addMonthlyCardDivider().accept(card);
    }

    public Button monthlyExportButton(Runnable action) {
        return requireDelegate().monthlyExportButton().create(action);
    }

    public VBox miniTotal(String title, String value, String styleClass) {
        return requireDelegate().miniTotal().create(title, value, styleClass);
    }

    public ScrollPane horizontalStatementScroll(Node node) {
        return requireDelegate().horizontalStatementScroll().apply(node);
    }

    private Delegate requireDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("AppView actions have not been connected.");
        }
        return delegate;
    }

    public record Delegate(
        Supplier<Integer> selectedYearValue,
        Supplier<Integer> selectedMonthValue,
        Consumer<Integer> setSelectedYearValue,
        Consumer<Integer> setSelectedMonthValue,
        Consumer<Parent> setPage,
        PageFactory page,
        PagePresenter darkHub,
        Runnable rebuildSidebar,
        Supplier<Window> owner,
        Supplier<String> selectedBankAccountAlias,
        Consumer<String> setSelectedBankAccountAlias,
        AlertAction alert,
        Function<Throwable, String> rootCauseMessage,
        BiConsumer<String, String> showProcessing,
        ProcessingWithCancelAction showProcessingWithCancel,
        ConfirmAction confirm,
        BiFunction<String, String, Optional<String>> promptText,
        Runnable showMortgages,
        Runnable showNylHub,
        BackButtonFactory backButton,
        ChooseFileAction choosePdf,
        ChooseExcelAction chooseExcel,
        AddReviewMarkAction addReviewMark,
        IntFunction<String> monthName,
        BiConsumer<Integer, Integer> selectedPeriodChanged,
        Function<String, javafx.scene.control.Label> helperNote,
        ReviewMarkLabelFactory reviewMarkLabel,
        Function<String, LocalDate> parseDate,
        Function<String, String> safeFileName,
        Consumer<Boolean> importingChanged,
        Supplier<Callback<TableColumn<CreditCardTransaction, String>, TableCell<CreditCardTransaction, String>>> stringCellFactory,
        Supplier<Callback<TableColumn<CreditCardTransaction, Double>, TableCell<CreditCardTransaction, Double>>> moneyCellFactory,
        ReviewPresenter reviewPresenter,
        MonthlyActionCardFactory monthlyActionCard,
        AddMonthlyCardLineAction addMonthlyCardLine,
        Consumer<VBox> addMonthlyCardDivider,
        MonthlyExportButtonFactory monthlyExportButton,
        MiniTotalFactory miniTotal,
        Function<Node, ScrollPane> horizontalStatementScroll
    ) {
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }

    @FunctionalInterface
    public interface ProcessingWithCancelAction {
        void show(String title, String message, Runnable cancelAction);
    }

    @FunctionalInterface
    public interface PageFactory {
        Parent build(String title, Node... nodes);
    }

    @FunctionalInterface
    public interface PagePresenter {
        void show(String title, Node... nodes);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }

    @FunctionalInterface
    public interface BackButtonFactory {
        Button create(String text, Runnable action);
    }

    @FunctionalInterface
    public interface ChooseFileAction {
        File choose();
    }

    @FunctionalInterface
    public interface ChooseExcelAction {
        File choose(String initialFileName);
    }

    @FunctionalInterface
    public interface AddReviewMarkAction {
        void add(VBox card, String source, String accountAlias, int year, int month);
    }

    @FunctionalInterface
    public interface ReviewMarkLabelFactory {
        Node apply(String source, String accountAlias, int year, int month);
    }

    @FunctionalInterface
    public interface ReviewPresenter {
        void show(String title, TableView<?> table, Runnable confirm, Node warningNode);
    }

    @FunctionalInterface
    public interface MonthlyActionCardFactory {
        VBox create(String title, String line1, String line2, String line3, String line4, Runnable action);
    }

    @FunctionalInterface
    public interface AddMonthlyCardLineAction {
        void add(VBox card, String text, String styleClass);
    }

    @FunctionalInterface
    public interface MonthlyExportButtonFactory {
        Button create(Runnable action);
    }

    @FunctionalInterface
    public interface MiniTotalFactory {
        VBox create(String title, String value, String styleClass);
    }
}
