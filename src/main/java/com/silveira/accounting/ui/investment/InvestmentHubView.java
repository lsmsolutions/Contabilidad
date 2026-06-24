package com.silveira.accounting.ui.investment;

import com.silveira.accounting.models.investment.InvestmentAccount;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class InvestmentHubView {
    public VBox build(
        List<InvestmentAccount> accounts,
        Runnable addAccount,
        Consumer<InvestmentAccount> importPdf,
        Consumer<InvestmentAccount> editAccount,
        Consumer<InvestmentAccount> deleteAccount,
        Consumer<String> openAccount
    ) {
        Label heading = new Label("Inversiones");
        heading.getStyleClass().add("heading");
        Button add = new Button("Add investment");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> addAccount.run());

        FlowPane cards = new FlowPane(12, 12);
        cards.getStyleClass().add("monthly-card-row");
        for (InvestmentAccount account : accounts) {
            Label title = new Label(account.getAlias());
            title.getStyleClass().add("monthly-card-title");
            GridPane details = new GridPane();
            details.getStyleClass().add("monthly-card-grid");
            addLine(details, 0, "Provider", account.getProviderName());
            addLine(details, 1, "Type", account.getAccountType());
            addLine(details, 2, "Account", ending(account.getAccountNumber()));

            Button importButton = new Button("Import PDF");
            importButton.setOnAction(event -> {
                event.consume();
                importPdf.accept(account);
            });
            Button edit = new Button("Edit");
            edit.setOnAction(event -> {
                event.consume();
                editAccount.accept(account);
            });
            Button delete = new Button("Delete");
            delete.getStyleClass().add("danger-button");
            delete.setOnAction(event -> {
                event.consume();
                deleteAccount.accept(account);
            });
            VBox card = new VBox(0, title, details, new HBox(8, importButton, edit, delete));
            card.getStyleClass().add("monthly-card");
            card.setOnMouseClicked(event -> openAccount.accept(account.getAlias()));
            cards.getChildren().add(card);
        }
        if (accounts.isEmpty()) {
            Label empty = new Label("No investment accounts added yet.");
            empty.getStyleClass().add("section-subtitle");
            cards.getChildren().add(empty);
        }

        VBox page = new VBox(18, heading, add, cards);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        return page;
    }

    private void addLine(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(valueText == null ? "" : valueText);
        value.getStyleClass().add("monthly-card-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private String ending(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= 4 ? value : "Ending " + value.substring(value.length() - 4);
    }
}
