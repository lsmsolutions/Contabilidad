package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.bank.BankStatementPeriod;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.ui.common.PeriodActionCardView;
import com.silveira.accounting.utils.Money;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BankPeriodCardsView {
    private final BankApplicationService bank;

    public BankPeriodCardsView(BankApplicationService bank) {
        this.bank = bank;
    }

    public VBox build(
        TableView<BankTransaction> table,
        HBox totalsPanel,
        int selectedYear,
        String accountAlias,
        double openingBalance,
        List<BankPeriodSummary> periods,
        Function<BankStatementPeriod, Label> reviewMarkFactory,
        Consumer<BankPeriodSummary> periodSelected,
        Consumer<BankPeriodSummary> editPeriod,
        Consumer<BankStatementPeriod> downloadPeriod,
        Consumer<BankPeriodSummary> deletePeriod
    ) {
        Label title = new Label("Resumen por periodo Banco");
        title.getStyleClass().add("section-title");
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");

        VBox general = monthlyActionCard("General", "Ver todos", "", "", "", () -> {
            table.setItems(FXCollections.observableArrayList(bank.transactions().find(selectedYear, null, null, null, accountAlias)));
            totalsPanel.getChildren().setAll(new BankTotalsView().build(bank.transactions().totals(selectedYear, null, accountAlias), openingBalance));
        });
        general.getStyleClass().add("monthly-card-general");
        cards.getChildren().add(general);

        for (BankPeriodSummary period : periods) {
            SourceTotals totals = period.totals();
            BankStatementPeriod statementPeriod = period.statementPeriod();
            double calculatedEnding = statementPeriod.openingBalance() + totals.net();
            VBox card = new PeriodActionCardView().build(
                BankPeriodTextFormatter.title(statementPeriod),
                reviewMarkFactory.apply(statementPeriod),
                () -> {
                    table.setItems(FXCollections.observableArrayList(period.transactions()));
                    totalsPanel.getChildren().setAll(new BankTotalsView().build(totals, statementPeriod.openingBalance()));
                    if (periodSelected != null) {
                        periodSelected.accept(period);
                    }
                }
            );
            addMonthlyCardLine(card, "Dep\u00f3sitos: " + Money.format(totals.income()));
            addMonthlyCardLine(card, "Salidas: " + Money.format(Math.abs(totals.expenses())));
            addMonthlyCardLine(card, "Ingreso NYL: " + Money.format(bank.transactions().nylIncome(period.transactions())));
            addMonthlyCardLine(card, "Movimiento neto: " + Money.format(totals.net()));
            addMonthlyCardLine(card, "Pendientes: " + totals.pendingCount());
            addMonthlyCardLine(card, "Saldo inicial: " + Money.format(statementPeriod.openingBalance()));
            addMonthlyCardLine(card, "Saldo final calculado: " + Money.format(calculatedEnding), "monthly-card-value-strong");
            addMonthlyCardLine(card, statementPeriod.hasStatementEndingBalance()
                ? "Saldo final PDF: " + Money.format(statementPeriod.statementEndingBalance())
                : "Saldo final PDF: pendiente", "monthly-card-value-strong");

            Button editBalances = new Button("Editar periodo");
            editBalances.setOnAction(event -> {
                event.consume();
                editPeriod.accept(period);
            });
            Button download = new Button("Descargar");
            download.setOnAction(event -> {
                event.consume();
                downloadPeriod.accept(statementPeriod);
            });
            Button delete = new Button("Eliminar periodo");
            delete.getStyleClass().add("danger-button");
            delete.setOnAction(event -> {
                event.consume();
                deletePeriod.accept(period);
            });
            HBox bankActions = new HBox(8, editBalances, download, delete);
            bankActions.getStyleClass().add("bank-monthly-actions");
            card.getChildren().add(bankActions);
            cards.getChildren().add(card);
        }

        VBox box = new VBox(10, title, cards);
        box.getStyleClass().add("monthly-section");
        return box;
    }

    private VBox monthlyActionCard(String title, String line1, String line2, String line3, String line4, Runnable action) {
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

    private void addMonthlyCardLine(VBox card, String text) {
        addMonthlyCardLine(card, text, "monthly-card-line");
    }

    private void addMonthlyCardLine(VBox card, String text, String styleClass) {
        if (text == null || text.isBlank()) {
            return;
        }
        text = text.replaceFirst("\\s+.*Dif\\.:.*$", "");
        if ("monthly-card-line".equals(styleClass) && !card.getChildren().isEmpty() && card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid) {
            addMonthlyCardGridLine(grid, nextMonthlyCardGridRow(grid), text);
            return;
        }
        if (styleClass.startsWith("monthly-card-value-") && !card.getChildren().isEmpty() && card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid) {
            addMonthlyCardGridLine(grid, nextMonthlyCardGridRow(grid), text, styleClass);
            return;
        }
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        card.getChildren().add(label);
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

    private int nextMonthlyCardGridRow(GridPane grid) {
        return grid.getChildren().stream()
            .map(GridPane::getRowIndex)
            .mapToInt(row -> row == null ? 0 : row)
            .max()
            .orElse(-1) + 1;
    }

}
