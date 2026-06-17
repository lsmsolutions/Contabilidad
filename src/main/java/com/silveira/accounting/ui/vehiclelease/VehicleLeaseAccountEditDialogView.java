package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import java.time.LocalDate;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class VehicleLeaseAccountEditDialogView {
    public Optional<VehicleLeaseAccount> show(VehicleLeaseAccount account) {
        Dialog<VehicleLeaseAccount> dialog = new Dialog<>();
        dialog.setTitle(account.getAlias() == null || account.getAlias().isBlank() ? "Add vehicle" : "Edit vehicle");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField alias = field(account.getAlias());
        TextField provider = field(account.getProviderName());
        TextField year = field(account.getVehicleYear() <= 0 ? "" : String.valueOf(account.getVehicleYear()));
        TextField make = field(account.getMake());
        TextField model = field(account.getModel());
        TextField trim = field(account.getTrim());
        TextField accountNumber = field(account.getAccountNumber());
        TextField vin = field(account.getVin());
        DatePicker maturity = new DatePicker(account.getMaturityDate());
        TextField notes = field(account.getNotes());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        addRow(grid, 0, "Alias", alias);
        addRow(grid, 1, "Provider", provider);
        addRow(grid, 2, "Year", year);
        addRow(grid, 3, "Make", make);
        addRow(grid, 4, "Model", model);
        addRow(grid, 5, "Trim", trim);
        addRow(grid, 6, "Account", accountNumber);
        addRow(grid, 7, "VIN", vin);
        grid.add(new Label("Maturity"), 0, 8);
        grid.add(maturity, 1, 8);
        addRow(grid, 9, "Notes", notes);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            VehicleLeaseAccount updated = copy(account);
            updated.setAlias(text(alias.getText()));
            updated.setProviderName(text(provider.getText()));
            updated.setVehicleYear(parseYear(year.getText()));
            updated.setMake(text(make.getText()));
            updated.setModel(text(model.getText()));
            updated.setTrim(text(trim.getText()));
            updated.setAccountNumber(text(accountNumber.getText()));
            updated.setVin(text(vin.getText()));
            updated.setMaturityDate(maturity.getValue());
            updated.setNotes(text(notes.getText()));
            return updated;
        });
        return dialog.showAndWait();
    }

    private void addRow(GridPane grid, int row, String labelText, TextField field) {
        grid.add(new Label(labelText), 0, row);
        grid.add(field, 1, row);
    }

    private TextField field(String value) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPrefWidth(320);
        GridPane.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private int parseYear(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value.trim());
    }

    private VehicleLeaseAccount copy(VehicleLeaseAccount source) {
        VehicleLeaseAccount copy = new VehicleLeaseAccount();
        copy.setId(source.getId());
        copy.setAlias(source.getAlias());
        copy.setProviderName(source.getProviderName());
        copy.setVehicleYear(source.getVehicleYear());
        copy.setMake(source.getMake());
        copy.setModel(source.getModel());
        copy.setTrim(source.getTrim());
        copy.setAccountNumber(source.getAccountNumber());
        copy.setVin(source.getVin());
        LocalDate maturity = source.getMaturityDate();
        copy.setMaturityDate(maturity);
        copy.setNotes(source.getNotes());
        return copy;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
