package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.controllers.bank.BankAccountDetailController;
import com.silveira.accounting.controllers.bank.BankImportController;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BankAccountDetailScreenView {
    private final BankApplicationService bank;
    private final BankAccountDetailController detail;
    private final BankImportController imports;

    public BankAccountDetailScreenView(
        BankApplicationService bank,
        BankAccountDetailController detail,
        BankImportController imports
    ) {
        this.bank = bank;
        this.detail = detail;
        this.imports = imports;
    }

    public VBox build(Config config) {
        BankPeriodSummary[] selectedPeriod = new BankPeriodSummary[1];
        BankPeriodActionControls periodActions = new BankPeriodActionControls();
        Label selectedPeriodLabel = periodActions.selectedPeriod();

        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        Runnable refreshTotals = () -> totals.getChildren().setAll(
            new BankTotalsView().buildAccumulated(detail.totals(config.selectedYear(), null, config.accountAlias()), openingBalance(config))
        );
        refreshTotals.run();

        VBox monthlyCards = new VBox(10);
        Runnable[] refreshPeriodCards = new Runnable[1];
        Runnable[] restoreSelectedPeriodTable = new Runnable[1];
        TableView<BankTransaction> table = new BankTransactionTableView(bank).build(() -> {
            refreshTotals.run();
            if (refreshPeriodCards[0] != null) {
                refreshPeriodCards[0].run();
            }
        });

        Consumer<BankPeriodSummary> selectPeriod = period -> {
            selectedPeriod[0] = period;
            selectedPeriodLabel.setText("Periodo seleccionado: " + BankPeriodTextFormatter.title(period.statementPeriod()));
        };
        Consumer<BankPeriodSummary> openPeriod = period -> {
            selectPeriod.accept(period);
            config.openPeriod().accept(period);
        };

        refreshPeriodCards[0] = () -> {
            BankStatementPeriod previousPeriod = selectedPeriod[0] == null ? null : selectedPeriod[0].statementPeriod();
            selectedPeriod[0] = null;
            monthlyCards.getChildren().setAll(monthlyCards(table, totals, config, openPeriod));
            if (previousPeriod != null) {
                detail.periodSummaries(config.accountAlias()).stream()
                    .filter(period -> period.samePeriodAs(previousPeriod))
                    .findFirst()
                    .ifPresent(selectPeriod);
            }
            if (selectedPeriod[0] == null) {
                resetSelectedPeriodLabel(selectedPeriodLabel);
            }
        };

        Runnable refresh = () -> {
            table.setItems(FXCollections.observableArrayList(detail.findRows(config.selectedYear(), config.selectedMonth(), null, null, config.accountAlias())));
            refreshTotals.run();
            selectedPeriod[0] = null;
            resetSelectedPeriodLabel(selectedPeriodLabel);
            monthlyCards.getChildren().setAll(monthlyCards(table, totals, config, openPeriod));
        };

        periodActions.addRecord().setOnAction(event -> {
            if (selectedPeriod[0] == null) {
                config.info().accept("Selecciona un periodo", "Haz clic en una card de periodo antes de añadir un registro.");
                return;
            }
            addManualTransaction(table, totals, monthlyCards, selectedPeriod[0].statementPeriod(), config, selectPeriod);
        });
        periodActions.save().setOnAction(event -> {
            saveVisibleRows(table);
            if (selectedPeriod[0] != null) {
                refreshSelectedPeriod(table, totals, selectedPeriod[0].statementPeriod(), config, selectPeriod);
            } else {
                refreshTotals.run();
            }
            monthlyCards.getChildren().setAll(monthlyCards(table, totals, config, openPeriod));
            table.refresh();
            config.info().accept("Banco guardado", "Cambios visibles guardados.");
        });

        periodActions.pending().setOnAction(event -> {
            if (selectedPeriod[0] == null) {
                List<BankPeriodSummary> periodsWithPending = detail.pendingPeriods(config.accountAlias());
                if (periodsWithPending.isEmpty()) {
                    config.info().accept("Sin pendientes", "No hay movimientos pendientes por revisar en esta cuenta.");
                    return;
                }
                if (periodsWithPending.size() > 1) {
                    config.info().accept("Selecciona un periodo", "Hay pendientes en varios periodos. Haz clic en una card para ver solo sus pendientes.");
                    return;
                }
                selectedPeriod[0] = periodsWithPending.get(0);
            }
            List<BankTransaction> pendingRows = selectedPeriod[0].transactions().stream()
                .filter(BankTransaction::isPendingReview)
                .toList();
            table.setItems(FXCollections.observableArrayList(pendingRows));
            totals.getChildren().setAll(new BankTotalsView().build(detail.totalsFromRows(pendingRows), selectedPeriod[0].statementPeriod().openingBalance()));
            selectedPeriodLabel.setText("Pendientes del periodo: " + BankPeriodTextFormatter.title(selectedPeriod[0].statementPeriod()));
        });

        periodActions.showAll().setOnAction(event -> {
            if (selectedPeriod[0] == null) {
                List<BankPeriodSummary> periodsWithPending = detail.pendingPeriods(config.accountAlias());
                if (periodsWithPending.size() == 1) {
                    selectedPeriod[0] = periodsWithPending.get(0);
                } else {
                    config.info().accept("Selecciona un periodo", "Haz clic en una card de periodo para ver sus movimientos.");
                    return;
                }
            }
            if (restoreSelectedPeriodTable[0] != null) {
                restoreSelectedPeriodTable[0].run();
            }
        });

        refresh.run();

        BankAccountDetailControls controls = new BankAccountDetailControls(config.selectedYear(), config.selectedMonth());
        ComboBox<Integer> year = controls.year();
        ComboBox<Integer> month = controls.month();
        TextField provider = controls.provider();
        ComboBox<String> type = controls.type();
        controls.filter().setOnAction(event -> {
            config.selectionChanged().accept(year.getValue(), month.getValue());
            table.setItems(FXCollections.observableArrayList(detail.findRows(year.getValue(), month.getValue(), provider.getText(), type.getValue(), config.accountAlias())));
            refreshTotals.run();
            selectedPeriod[0] = null;
            resetSelectedPeriodLabel(selectedPeriodLabel);
            monthlyCards.getChildren().setAll(monthlyCards(table, totals, config.withSelection(year.getValue(), month.getValue()), openPeriod));
        });
        controls.importPdf().setOnAction(event -> config.importPdf().accept(refresh));
        controls.manualPeriod().setOnAction(event -> config.manualPeriod().accept(refresh));
        controls.deletePeriod().setOnAction(event -> config.deletePeriod().accept(selectedPeriod[0], refresh));

        restoreSelectedPeriodTable[0] = () -> {
            if (selectedPeriod[0] == null) {
                return;
            }
            table.setItems(FXCollections.observableArrayList(selectedPeriod[0].transactions()));
            totals.getChildren().setAll(new BankTotalsView().build(selectedPeriod[0].totals(), selectedPeriod[0].statementPeriod().openingBalance()));
            selectedPeriodLabel.setText("Periodo seleccionado: " + BankPeriodTextFormatter.title(selectedPeriod[0].statementPeriod()));
        };

        Label reviewNote = new Label("Haz clic en una card para abrir el detalle completo de ese periodo.");
        reviewNote.getStyleClass().add("section-subtitle");
        reviewNote.setWrapText(true);

        return new BankAccountDetailPageView().build(
            config.title(),
            config.backButton(),
            controls.actions(),
            reviewNote,
            totals,
            monthlyCards
        );
    }

    private VBox monthlyCards(TableView<BankTransaction> table, HBox totals, Config config, Consumer<BankPeriodSummary> selectPeriod) {
        int year = config.selectedYear() == null ? LocalDate.now().getYear() : config.selectedYear();
        return new BankPeriodCardsView(bank).build(
            table,
            totals,
            year,
            config.accountAlias(),
            openingBalance(config),
            detail.periodSummaries(config.accountAlias()),
            config.reviewMarkFactory(),
            selectPeriod,
            config.editPeriod(),
            config.downloadPeriod(),
            (period) -> config.deletePeriod().accept(period, () -> config.reload().run())
        );
    }

    private void addManualTransaction(
        TableView<BankTransaction> table,
        HBox totals,
        VBox monthlyCards,
        BankStatementPeriod period,
        Config config,
        Consumer<BankPeriodSummary> selectPeriod
    ) {
        LocalDate fallbackDate = LocalDate.of(
            config.selectedYear() == null ? LocalDate.now().getYear() : config.selectedYear(),
            config.selectedMonth() == null ? LocalDate.now().getMonthValue() : config.selectedMonth(),
            1
        );
        BankTransaction transaction = imports.createManualTransaction(config.accountAlias(), fallbackDate, period);
        refreshSelectedPeriod(table, totals, period, config, selectPeriod);
        monthlyCards.getChildren().setAll(monthlyCards(table, totals, config, selectPeriod));
        BankTransaction visibleTransaction = table.getItems().stream()
            .filter(row -> row.getId() == transaction.getId())
            .findFirst()
            .orElse(transaction);
        int rowIndex = table.getItems().indexOf(visibleTransaction);
        if (rowIndex >= 0) {
            table.scrollTo(visibleTransaction);
            table.getSelectionModel().select(visibleTransaction);
            table.edit(rowIndex, table.getColumns().get(0));
        }
    }

    private void saveVisibleRows(TableView<BankTransaction> table) {
        for (BankTransaction transaction : table.getItems()) {
            bank.transactions().normalizeSign(transaction);
            if (transaction.getId() > 0) {
                bank.transactions().update(transaction);
            } else {
                transaction.setId(bank.transactions().save(transaction));
            }
        }
    }

    private void refreshSelectedPeriod(
        TableView<BankTransaction> table,
        HBox totals,
        BankStatementPeriod period,
        Config config,
        Consumer<BankPeriodSummary> selectPeriod
    ) {
        BankPeriodSummary refreshedPeriod = detail.periodSummaries(config.accountAlias()).stream()
            .filter(summary -> Objects.equals(summary.statementPeriod().sourcePdf(), period.sourcePdf()))
            .findFirst()
            .orElse(null);
        if (refreshedPeriod != null) {
            selectPeriod.accept(refreshedPeriod);
            table.setItems(FXCollections.observableArrayList(refreshedPeriod.transactions()));
            totals.getChildren().setAll(new BankTotalsView().build(refreshedPeriod.totals(), refreshedPeriod.statementPeriod().openingBalance()));
        }
    }

    private double openingBalance(Config config) {
        return detail.firstOpeningBalance(config.selectedYear(), config.accountAlias());
    }

    private void resetSelectedPeriodLabel(Label selectedPeriodLabel) {
        selectedPeriodLabel.setText("Selecciona una card de periodo para añadir registros dentro de ese extracto.");
    }

    public record Config(
        String accountAlias,
        String title,
        Integer selectedYear,
        Integer selectedMonth,
        Node backButton,
        BiConsumer<String, String> info,
        BiConsumer<Integer, Integer> selectionChanged,
        Consumer<Runnable> importPdf,
        Consumer<Runnable> manualPeriod,
        DeletePeriodAction deletePeriod,
        Function<BankStatementPeriod, Label> reviewMarkFactory,
        Consumer<BankPeriodSummary> editPeriod,
        Consumer<BankStatementPeriod> downloadPeriod,
        Consumer<BankPeriodSummary> openPeriod,
        Runnable reload
    ) {
        Config withSelection(Integer year, Integer month) {
            return new Config(
                accountAlias,
                title,
                year,
                month,
                backButton,
                info,
                selectionChanged,
                importPdf,
                manualPeriod,
                deletePeriod,
                reviewMarkFactory,
                editPeriod,
                downloadPeriod,
                openPeriod,
                reload
            );
        }
    }

    @FunctionalInterface
    public interface DeletePeriodAction {
        void accept(BankPeriodSummary period, Runnable refresh);
    }
}
