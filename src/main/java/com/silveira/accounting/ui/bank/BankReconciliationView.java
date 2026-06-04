package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.utils.Money;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class BankReconciliationView {
    public record AccountSummary(String alias, SourceTotals totals) {
    }

    public record NylBankSummary(
        BankAccount selectedAccount,
        SourceTotals nylTotals,
        List<BankTransaction> bankRows
    ) {
    }

    private final BankApplicationService bank;

    public BankReconciliationView(BankApplicationService bank) {
        this.bank = bank;
    }

    public VBox bankByAccount(List<AccountSummary> accountSummaries, Consumer<String> openAccount) {
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");
        for (AccountSummary summary : accountSummaries) {
            SourceTotals totals = summary.totals();
            cards.getChildren().add(monthlyActionCard(
                summary.alias(),
                "Depositos: " + Money.format(totals.income()),
                "Salidas: " + Money.format(Math.abs(totals.expenses())),
                "Neto: " + Money.format(totals.net()),
                "Pendientes: " + totals.pendingCount(),
                () -> openAccount.accept(summary.alias())
            ));
        }
        return new VBox(10, sectionLabel("Banco por cuenta"), cards);
    }

    public VBox nylBank(NylBankSummary summary) {
        double bankReceived = summary.bankRows().stream().mapToDouble(BankTransaction::getAmount).sum();
        double expectedFromNyl = summary.nylTotals().net();
        double difference = bankReceived - expectedFromNyl;
        String status = Math.abs(difference) < 0.01 ? "Conciliado" : "Diferencia por revisar";
        String accountText = summary.selectedAccount() == null
            ? "Cuenta bancaria: no seleccionada. Se recomienda usar la cuenta que recibe pagos de New York Life."
            : "Cuenta bancaria usada para NYL: " + summary.selectedAccount().getAlias() + BankAccountTextFormatter.accountSuffix(summary.selectedAccount());

        HBox totals = new HBox(12,
            miniTotal("Esperado segun NYL", Money.format(expectedFromNyl), "income-total"),
            miniTotal("Recibido en banco de NYL", Money.format(bankReceived), "neutral-total"),
            miniTotal("Diferencia", Money.format(difference), Math.abs(difference) < 0.01 ? "net-total" : "pending-total"),
            miniTotal("Estado", status, Math.abs(difference) < 0.01 ? "net-total" : "pending-total")
        );
        totals.getStyleClass().add("totals-panel");

        TableView<BankTransaction> table = new BankTransactionTableView(bank).build();
        table.setItems(FXCollections.observableArrayList(summary.bankRows()));
        table.setMinHeight(360);
        table.setPrefHeight(460);

        Label title = sectionLabel("Conciliacion New York Life vs Banco");
        Label explanation = helperNote("Compara el neto revisado de New York Life contra lo recibido en Banco con proveedor New York Life. Solo se toman registros con OK; los pendientes no cuentan.");
        Label account = helperNote(accountText);
        Label detailTitle = sectionLabel("Depositos bancarios incluidos");
        return new VBox(12, title, explanation, account, totals, detailTitle, table);
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

    private int addMonthlyCardGridLine(GridPane grid, int row, String text) {
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
        grid.add(label, 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }

    private VBox miniTotal(String title, String value, String styleClass) {
        Label label = new Label(title);
        label.getStyleClass().add("mini-total-title");
        Label amount = new Label(value);
        amount.getStyleClass().add("mini-total-value");
        VBox box = new VBox(4, label, amount);
        box.getStyleClass().addAll("mini-total", styleClass);
        return box;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label helperNote(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-subtitle");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);
        return label;
    }
}
