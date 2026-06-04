package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.utils.Money;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class BankDashboardPanelView {
    public VBox build(
        List<BankAccount> accounts,
        Function<String, List<BankPeriodSummary>> periodSummaries
    ) {
        Label title = new Label("Banco");
        title.getStyleClass().add("dashboard-panel-title");
        HBox accountCards = new HBox(12);
        accountCards.getStyleClass().add("dashboard-bank-accounts");
        for (BankAccount account : accounts) {
            accountCards.getChildren().add(accountCard(account, periodSummaries));
        }
        if (accountCards.getChildren().isEmpty()) {
            Label empty = new Label("No hay cuentas bancarias registradas.");
            empty.getStyleClass().add("dashboard-empty");
            accountCards.getChildren().add(empty);
        }
        VBox panel = new VBox(10, title, accountCards);
        panel.getStyleClass().add("dashboard-panel");
        return panel;
    }

    private VBox accountCard(
        BankAccount account,
        Function<String, List<BankPeriodSummary>> periodSummaries
    ) {
        Optional<BankPeriodSummary> latest = periodSummaries.apply(account.getAlias()).stream()
            .max(Comparator.comparing(summary -> summary.statementPeriod().periodEnd()));
        Label name = new Label(account.getAlias());
        name.getStyleClass().add("dashboard-bank-name");
        Label period = new Label(latest
            .map(summary -> BankPeriodTextFormatter.title(summary.statementPeriod()))
            .orElse("Sin periodo"));
        period.getStyleClass().add("dashboard-bank-period");
        HBox bars = latest
            .map(summary -> bars(summary.totals().income(), Math.abs(summary.totals().expenses())))
            .orElseGet(() -> bars(0, 0));
        VBox box = new VBox(8, name, period, bars);
        box.getStyleClass().add("dashboard-bank-card");
        Label expectedPdf = new Label(expectedPdfLabel(account.getAlias()));
        expectedPdf.getStyleClass().add("dashboard-bank-pdf");
        return new VBox(5, box, expectedPdf);
    }

    private String expectedPdfLabel(String alias) {
        return switch (alias == null ? "" : alias.trim()) {
            case "cta_15705" -> "PDF: fin/inicio";
            case "cta_55385" -> "PDF: 14-17 cada mes";
            default -> "PDF: -";
        };
    }

    private HBox bars(double deposits, double withdrawals) {
        double max = Math.max(Math.max(deposits, withdrawals), 1.0);
        HBox bars = new HBox(12,
            verticalBar("Depositos", deposits, max, "dashboard-bank-deposit-bar"),
            verticalBar("Salidas", withdrawals, max, "dashboard-bank-withdrawal-bar")
        );
        bars.setAlignment(Pos.BOTTOM_LEFT);
        return bars;
    }

    private VBox verticalBar(String title, double amount, double max, String styleClass) {
        Region bar = new Region();
        bar.getStyleClass().add(styleClass);
        double height = 18 + (72 * (amount / max));
        bar.setMinSize(26, height);
        bar.setPrefSize(26, height);
        bar.setMaxSize(26, height);
        Label value = new Label(Money.format(amount));
        value.getStyleClass().add("dashboard-bank-bar-value");
        Label label = new Label(title);
        label.getStyleClass().add("dashboard-bank-bar-label");
        VBox box = new VBox(5, value, bar, label);
        box.setAlignment(Pos.BOTTOM_CENTER);
        return box;
    }
}
