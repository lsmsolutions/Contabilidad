package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class VehicleLeaseHubView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public VBox build(List<VehicleLeaseAccount> accounts, Runnable importAction, Consumer<String> openAccount) {
        Label heading = new Label("Vehicle Leases");
        heading.getStyleClass().add("heading");
        Button importPdf = new Button("Import PDF");
        importPdf.getStyleClass().add("primary");
        importPdf.setOnAction(event -> importAction.run());
        HBox actions = new HBox(10, importPdf);

        FlowPane cards = new FlowPane(12, 12);
        cards.getStyleClass().add("monthly-card-row");
        for (VehicleLeaseAccount account : accounts) {
            Label title = new Label(vehicleName(account));
            title.getStyleClass().add("monthly-card-title");
            GridPane details = new GridPane();
            details.getStyleClass().add("monthly-card-grid");
            addLine(details, 0, "Provider", text(account.getProviderName()));
            addLine(details, 1, "Account", ending(account.getAccountNumber()));
            addLine(details, 2, "VIN", text(account.getVin()));
            addLine(details, 3, "Maturity", account.getMaturityDate() == null ? "" : account.getMaturityDate().format(DATE));
            VBox card = new VBox(0, title, details);
            card.getStyleClass().add("monthly-card");
            card.setOnMouseClicked(event -> openAccount.accept(account.getAlias()));
            cards.getChildren().add(card);
        }
        if (accounts.isEmpty()) {
            Label empty = new Label("No vehicle leases imported yet.");
            empty.getStyleClass().add("section-subtitle");
            cards.getChildren().add(empty);
        }

        VBox page = new VBox(18, heading, actions, cards);
        page.setPadding(new Insets(28));
        page.getStyleClass().add("page");
        return page;
    }

    private void addLine(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(valueText);
        value.getStyleClass().add("monthly-card-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private String vehicleName(VehicleLeaseAccount account) {
        return (account.getVehicleYear() > 0 ? account.getVehicleYear() + " " : "")
            + text(account.getMake()) + " " + text(account.getModel()) + " " + text(account.getTrim());
    }

    private String ending(String value) {
        String text = text(value);
        return text.length() <= 4 ? text : "Ending " + text.substring(text.length() - 4);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
