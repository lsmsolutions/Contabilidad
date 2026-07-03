package com.silveira.accounting.ui.investment;

import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.utils.Money;
import java.time.LocalDate;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class InvestmentStatementEditDialogView {
    public Optional<InvestmentStatement> show(InvestmentStatement statement) {
        Dialog<InvestmentStatement> dialog = new Dialog<>();
        dialog.setTitle("Editar periodo de inversion");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        TextField periodStart = field(date(statement.getPeriodStart()));
        TextField periodEnd = field(date(statement.getPeriodEnd()));
        TextField beginningValue = field(Money.format(statement.getBeginningValue()));
        TextField transferOfSecurities = field(Money.format(statement.getTransferOfSecurities()));
        TextField dividendsReinvested = field(Money.format(statement.getDividendsReinvested()));
        TextField cashActivity = field(Money.format(statement.getCashActivity()));
        TextField changeInMarketValue = field(Money.format(statement.getChangeInMarketValue()));
        TextField endingValue = field(Money.format(statement.getEndingValue()));
        TextField costBasisTotal = field(Money.format(statement.getCostBasisTotal()));
        TextField unrealizedGainLoss = field(Money.format(statement.getUnrealizedGainLoss()));

        addRow(grid, 0, "Period start (yyyy-mm-dd)", periodStart);
        addRow(grid, 1, "Period end (yyyy-mm-dd)", periodEnd);
        addRow(grid, 2, "Beginning Value", beginningValue);
        addRow(grid, 3, "Transfer of Securities(In/Out)", transferOfSecurities);
        addRow(grid, 4, "Dividends Reinvested", dividendsReinvested);
        addRow(grid, 5, "Cash Activity", cashActivity);
        addRow(grid, 6, "Change in Market Value", changeInMarketValue);
        addRow(grid, 7, "Ending Value", endingValue);
        addRow(grid, 8, "Cost Basis", costBasisTotal);
        addRow(grid, 9, "Unrealized Gain/(Loss)", unrealizedGainLoss);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(scroll);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            InvestmentStatement updated = copy(statement);
            updated.setPeriodStart(LocalDate.parse(periodStart.getText().trim()));
            updated.setPeriodEnd(LocalDate.parse(periodEnd.getText().trim()));
            updated.setBeginningValue(Money.parse(beginningValue.getText()));
            updated.setTransferOfSecurities(Money.parse(transferOfSecurities.getText()));
            updated.setDividendsReinvested(Money.parse(dividendsReinvested.getText()));
            updated.setCashActivity(Money.parse(cashActivity.getText()));
            updated.setChangeInMarketValue(Money.parse(changeInMarketValue.getText()));
            updated.setEndingValue(Money.parse(endingValue.getText()));
            updated.setCostBasisTotal(Money.parse(costBasisTotal.getText()));
            updated.setUnrealizedGainLoss(Money.parse(unrealizedGainLoss.getText()));
            return updated;
        });
        return dialog.showAndWait();
    }

    private void addRow(GridPane grid, int row, String label, TextField field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }

    private TextField field(String value) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPrefWidth(260);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private String date(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private InvestmentStatement copy(InvestmentStatement source) {
        InvestmentStatement copy = new InvestmentStatement();
        copy.setId(source.getId());
        copy.setAccountAlias(source.getAccountAlias());
        copy.setPeriodStart(source.getPeriodStart());
        copy.setPeriodEnd(source.getPeriodEnd());
        copy.setBeginningValue(source.getBeginningValue());
        copy.setEndingValue(source.getEndingValue());
        copy.setTransferOfSecurities(source.getTransferOfSecurities());
        copy.setDividendsReinvested(source.getDividendsReinvested());
        copy.setCashActivity(source.getCashActivity());
        copy.setChangeInMarketValue(source.getChangeInMarketValue());
        copy.setDeposits(source.getDeposits());
        copy.setWithdrawals(source.getWithdrawals());
        copy.setDividendsInterest(source.getDividendsInterest());
        copy.setMarketChange(source.getMarketChange());
        copy.setExpenses(source.getExpenses());
        copy.setCostBasisTotal(source.getCostBasisTotal());
        copy.setUnrealizedGainLoss(source.getUnrealizedGainLoss());
        copy.setSourcePdfPath(source.getSourcePdfPath());
        return copy;
    }
}
