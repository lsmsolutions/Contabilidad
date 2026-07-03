package com.silveira.accounting.ui.vehiclelease;

import com.silveira.accounting.controllers.vehiclelease.VehicleLeaseController;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class VehicleLeaseWorkflow {
    private final VehicleLeaseController controller;
    private final Config config;

    public VehicleLeaseWorkflow(VehicleLeaseController controller, Config config) {
        this.controller = controller;
        this.config = config;
    }

    public List<VehicleLeaseAccount> accounts() {
        return controller.accounts();
    }

    public void showVehicleLeases() {
        config.setPage().accept(new VehicleLeaseHubView().build(
            controller.accounts(),
            this::addVehicleLeaseAccount,
            account -> importVehicleLeasePdf(account.getAlias()),
            this::editVehicleLeaseAccount,
            this::deleteVehicleLeaseAccount,
            this::showVehicleLeaseDetail
        ));
    }

    public void showVehicleLeaseDetail(String alias) {
        Optional<VehicleLeaseAccount> account = controller.account(alias);
        if (account.isEmpty()) {
            showVehicleLeases();
            return;
        }
        List<VehicleLeaseStatement> statements = controller.statements(alias);
        config.setPage().accept(new VehicleLeaseDetailView().build(
            account.get(),
            statements,
            this::showVehicleLeases,
            () -> importVehicleLeasePdf(alias),
            controller::isFieldReviewed,
            controller::isStatementReviewed,
            (statement, field, reviewed) -> {
                controller.setFieldReviewed(statement, field, reviewed);
            },
            (statement, reviewed) -> {
                controller.setAllReviewed(statement, reviewed);
            },
            statement -> editVehicleLeaseStatement(alias, statement),
            this::saveVehicleLeaseStatement,
            statement -> deleteVehicleLeaseStatement(alias, statement)
        ));
    }

    private void addVehicleLeaseAccount() {
        VehicleLeaseAccount account = new VehicleLeaseAccount();
        new VehicleLeaseAccountEditDialogView().show(account).ifPresent(updated -> {
            if (updated.getAlias().isBlank()) {
                config.alert().show(Alert.AlertType.WARNING, "Alias requerido", "Introduce un alias para el vehiculo.");
                return;
            }
            controller.saveAccount(updated);
            config.rebuildSidebar().run();
            showVehicleLeases();
        });
    }

    private void editVehicleLeaseAccount(VehicleLeaseAccount account) {
        String originalAlias = account.getAlias();
        new VehicleLeaseAccountEditDialogView().show(account).ifPresent(updated -> {
            if (updated.getAlias().isBlank()) {
                config.alert().show(Alert.AlertType.WARNING, "Alias requerido", "Introduce un alias para el vehiculo.");
                return;
            }
            controller.updateAccount(originalAlias, updated);
            config.rebuildSidebar().run();
            showVehicleLeases();
        });
    }

    private void deleteVehicleLeaseAccount(VehicleLeaseAccount account) {
        if (!config.confirm().confirm(
            "Eliminar vehiculo",
            "Se eliminaran el vehiculo y sus statements.\n\nEsta accion no se puede deshacer.",
            "Eliminar"
        )) {
            return;
        }
        controller.deleteAccount(account.getAlias());
        config.rebuildSidebar().run();
        showVehicleLeases();
    }

    private void editVehicleLeaseStatement(String alias, VehicleLeaseStatement statement) {
        try {
            new VehicleLeaseStatementEditDialogView().show(statement).ifPresent(updated -> {
                controller.update(updated);
                showVehicleLeaseDetail(alias);
            });
        } catch (RuntimeException exception) {
            config.alert().show(
                Alert.AlertType.ERROR,
                "No se pudo editar el statement",
                config.rootCauseMessage().apply(exception)
            );
        }
    }

    private void saveVehicleLeaseStatement(VehicleLeaseStatement statement) {
        try {
            controller.update(statement);
            config.alert().show(Alert.AlertType.INFORMATION, "Guardado", "El periodo de leasing quedo guardado.");
        } catch (RuntimeException exception) {
            config.alert().show(
                Alert.AlertType.ERROR,
                "No se pudo guardar el periodo",
                config.rootCauseMessage().apply(exception)
            );
        }
    }

    private void deleteVehicleLeaseStatement(String alias, VehicleLeaseStatement statement) {
        boolean proceed = config.confirm().confirm(
            "Eliminar periodo de leasing",
            "Se eliminara el statement del " + (statement.getStatementDate() == null ? "periodo seleccionado" : statement.getStatementDate())
                + ".\n\nEsta accion no se puede deshacer.",
            "Eliminar periodo"
        );
        if (proceed) {
            controller.delete(statement);
            showVehicleLeaseDetail(alias);
        }
    }

    private void importVehicleLeasePdf(String currentAlias) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import vehicle lease statement");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File file = chooser.showOpenDialog(config.owner().get());
        if (file == null) {
            return;
        }
        try {
            VehicleLeaseStatement imported = currentAlias == null
                ? controller.importPdf(file.toPath())
                : controller.importPdf(file.toPath(), currentAlias);
            config.rebuildSidebar().run();
            showVehicleLeaseDetail(imported.getAccountAlias());
            config.alert().show(
                Alert.AlertType.INFORMATION,
                "Vehicle lease imported",
                "The statement was imported and is ready for review."
            );
        } catch (RuntimeException exception) {
            handleVehicleLeaseImportFailure(currentAlias, file, exception);
        }
    }

    private void handleVehicleLeaseImportFailure(String currentAlias, File file, RuntimeException exception) {
        ButtonType ai = new ButtonType("Intentar con IA", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
            Alert.AlertType.ERROR,
            config.rootCauseMessage().apply(exception)
                + "\n\nPuedes intentar leer este PDF con IA. El resultado quedara pendiente de revision.",
            ai,
            close
        );
        alert.setTitle("No se pudo importar el leasing");
        alert.setHeaderText("No se pudo importar el leasing");
        alert.initOwner(config.owner().get());
        Optional<ButtonType> selected = alert.showAndWait();
        if (selected.isPresent() && selected.get() == ai) {
            importVehicleLeasePdfWithAi(currentAlias, file);
            return;
        }
        if (currentAlias != null) {
            showVehicleLeaseDetail(currentAlias);
        }
    }

    private void importVehicleLeasePdfWithAi(String currentAlias, File file) {
        config.alert().show(
            Alert.AlertType.INFORMATION,
            "Lectura con IA",
            "La IA intentara leer el PDF. Puedes revisar el resultado antes de marcarlo como revisado."
        );
        Task<VehicleLeaseStatement> task = new Task<>() {
            @Override
            protected VehicleLeaseStatement call() {
                return currentAlias == null
                ? controller.importPdfWithAi(file.toPath())
                : controller.importPdfWithAi(file.toPath(), currentAlias);
            }
        };
        task.setOnSucceeded(event -> {
            VehicleLeaseStatement imported = task.getValue();
            config.rebuildSidebar().run();
            showVehicleLeaseDetail(imported.getAccountAlias());
            config.alert().show(
                Alert.AlertType.INFORMATION,
                "Leasing leido con IA",
                "El statement fue extraido con IA y quedo pendiente de revision."
            );
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            config.alert().show(
                Alert.AlertType.ERROR,
                "No se pudo leer con IA",
                config.rootCauseMessage().apply(exception)
            );
            if (currentAlias != null) {
                showVehicleLeaseDetail(currentAlias);
            }
        });
        Thread worker = new Thread(task, "vehicle-lease-ai-import");
        worker.setDaemon(true);
        worker.start();
    }

    public record Config(
        Consumer<Parent> setPage,
        Runnable rebuildSidebar,
        Supplier<Window> owner,
        AlertAction alert,
        Function<Throwable, String> rootCauseMessage,
        ConfirmAction confirm
    ) {
    }

    @FunctionalInterface
    public interface AlertAction {
        void show(Alert.AlertType type, String title, String message);
    }

    @FunctionalInterface
    public interface ConfirmAction {
        boolean confirm(String title, String message, String confirmText);
    }
}
