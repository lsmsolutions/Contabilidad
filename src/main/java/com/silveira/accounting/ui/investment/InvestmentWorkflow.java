package com.silveira.accounting.ui.investment;

import com.silveira.accounting.controllers.investment.InvestmentController;
import com.silveira.accounting.models.investment.InvestmentAccount;
import com.silveira.accounting.models.investment.InvestmentPosition;
import com.silveira.accounting.models.investment.InvestmentStatement;
import com.silveira.accounting.models.investment.InvestmentTransaction;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class InvestmentWorkflow {
    private final InvestmentController controller;
    private final Config config;

    public InvestmentWorkflow(InvestmentController controller, Config config) {
        this.controller = controller;
        this.config = config;
    }

    public List<InvestmentAccount> accounts() {
        return controller.accounts();
    }

    public void showInvestments() {
        config.setPage().accept(new InvestmentHubView().build(
            controller.accounts(),
            this::addInvestmentAccount,
            account -> importInvestmentPdf(account.getAlias()),
            this::editInvestmentAccount,
            this::deleteInvestmentAccount,
            alias -> showInvestmentDetail(alias, null)
        ));
    }

    public void showInvestmentDetail(String alias) {
        showInvestmentDetail(alias, null);
    }

    private void addInvestmentAccount() {
        InvestmentAccount account = new InvestmentAccount();
        account.setProviderName("Charles Schwab");
        account.setAccountType("Brokerage");
        new InvestmentAccountEditDialogView().show(account).ifPresent(updated -> {
            if (updated.getAlias().isBlank()) {
                config.alert().show(Alert.AlertType.WARNING, "Alias requerido", "Introduce un alias para la inversion.");
                return;
            }
            controller.saveAccount(updated);
            config.rebuildSidebar().run();
            showInvestments();
        });
    }

    private void editInvestmentAccount(InvestmentAccount account) {
        String originalAlias = account.getAlias();
        new InvestmentAccountEditDialogView().show(account).ifPresent(updated -> {
            if (updated.getAlias().isBlank()) {
                config.alert().show(Alert.AlertType.WARNING, "Alias requerido", "Introduce un alias para la inversion.");
                return;
            }
            controller.updateAccount(originalAlias, updated);
            config.rebuildSidebar().run();
            showInvestments();
        });
    }

    private void deleteInvestmentAccount(InvestmentAccount account) {
        if (!config.confirm().confirm(
            "Eliminar inversion",
            "Se eliminaran la cuenta, sus periodos, posiciones y movimientos.\n\nEsta accion no se puede deshacer.",
            "Eliminar"
        )) {
            return;
        }
        controller.deleteAccount(account.getAlias());
        config.rebuildSidebar().run();
        showInvestments();
    }

    private void showInvestmentDetail(String alias, Long selectedStatementId) {
        Optional<InvestmentAccount> account = controller.account(alias);
        if (account.isEmpty()) {
            showInvestments();
            return;
        }
        List<InvestmentStatement> statements = controller.statements(alias);
        InvestmentStatement selected = statements.stream()
            .filter(value -> selectedStatementId == null || value.getId() == selectedStatementId)
            .findFirst()
            .orElse(statements.isEmpty() ? null : statements.get(0));
        long statementId = selected == null ? 0 : selected.getId();
        config.setPage().accept(new InvestmentDetailView().build(
            account.get(),
            statements,
            selected,
            selected == null ? List.of() : controller.allocations(statementId),
            selected == null ? List.of() : controller.positions(statementId),
            selected == null ? List.of() : controller.transactions(statementId),
            this::showInvestments,
            () -> importInvestmentPdf(alias),
            id -> showInvestmentDetail(alias, id),
            (statement, positions) -> savePositions(alias, statement, positions),
            (statement, transactions) -> saveTransactions(alias, statement, transactions),
            statement -> editInvestmentStatement(alias, statement),
            statement -> {
                if (config.confirm().confirm(
                    "Eliminar periodo de inversion",
                    "Se eliminara el periodo " + statement.getPeriodEnd() + " y todo su detalle.",
                    "Eliminar periodo"
                )) {
                    controller.deleteStatement(statement.getId());
                    showInvestmentDetail(alias, null);
                }
            },
            statement -> config.reviewMarkLabel().apply(
                "investment",
                alias,
                statement.getPeriodEnd().getYear(),
                statement.getPeriodEnd().getMonthValue()
            )
        ));
    }

    private void editInvestmentStatement(String alias, InvestmentStatement statement) {
        new InvestmentStatementEditDialogView().show(statement).ifPresent(updated -> {
            try {
                controller.updateStatement(updated);
                showInvestmentDetail(alias, updated.getId());
            } catch (RuntimeException exception) {
                config.alert().show(
                    Alert.AlertType.ERROR,
                    "No se pudo actualizar el periodo",
                    config.rootCauseMessage().apply(exception)
                );
            }
        });
    }

    private void saveTransactions(String alias, InvestmentStatement statement, List<InvestmentTransaction> transactions) {
        try {
            controller.saveTransactions(statement, transactions);
            config.alert().show(Alert.AlertType.INFORMATION, "Movimientos guardados", "Los movimientos de inversion quedaron guardados.");
            showInvestmentDetail(alias, statement.getId());
        } catch (RuntimeException exception) {
            config.alert().show(
                Alert.AlertType.ERROR,
                "No se pudieron guardar los movimientos",
                config.rootCauseMessage().apply(exception)
            );
        }
    }

    private void savePositions(String alias, InvestmentStatement statement, List<InvestmentPosition> positions) {
        try {
            controller.savePositions(statement, positions);
            config.alert().show(Alert.AlertType.INFORMATION, "Posiciones guardadas", "Las posiciones y sus totales quedaron guardados.");
            showInvestmentDetail(alias, statement.getId());
        } catch (RuntimeException exception) {
            config.alert().show(
                Alert.AlertType.ERROR,
                "No se pudieron guardar las posiciones",
                config.rootCauseMessage().apply(exception)
            );
        }
    }

    private void importInvestmentPdf(String alias) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import investment statement");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File file = chooser.showOpenDialog(config.owner().get());
        if (file == null) {
            return;
        }
        try {
            InvestmentStatement imported = controller.importPdf(file.toPath(), alias);
            showImportedInvestment(imported);
        } catch (RuntimeException exception) {
            handleInvestmentImportFailure(alias, file, exception);
        }
    }

    private void handleInvestmentImportFailure(String alias, File file, RuntimeException exception) {
        ButtonType ai = new ButtonType("Intentar con IA", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(
            Alert.AlertType.ERROR,
            config.rootCauseMessage().apply(exception)
                + "\n\nPuedes intentar leer este PDF con IA. Revisa el resultado contra el PDF original.",
            ai,
            close
        );
        alert.setTitle("No se pudo importar la inversion");
        alert.setHeaderText("No se pudo importar la inversion");
        alert.initOwner(config.owner().get());
        Optional<ButtonType> selected = alert.showAndWait();
        if (selected.isPresent() && selected.get() == ai) {
            importInvestmentPdfWithAi(alias, file);
        } else {
            showInvestmentDetail(alias, null);
        }
    }

    private void importInvestmentPdfWithAi(String alias, File file) {
        try {
            InvestmentStatement imported = controller.importPdfWithAi(file.toPath(), alias);
            showImportedInvestment(imported);
            config.alert().show(
                Alert.AlertType.INFORMATION,
                "Investment leido con IA",
                "The statement, positions and transactions were imported. Revisa el resultado contra el PDF original."
            );
        } catch (RuntimeException exception) {
            config.alert().show(
                Alert.AlertType.ERROR,
                "No se pudo leer con IA",
                config.rootCauseMessage().apply(exception)
            );
            showInvestmentDetail(alias, null);
        }
    }

    private void showImportedInvestment(InvestmentStatement imported) {
        config.rebuildSidebar().run();
        showInvestmentDetail(imported.getAccountAlias(), imported.getId());
        config.alert().show(
            Alert.AlertType.INFORMATION,
            "Investment imported",
            "The statement, positions and transactions were imported."
        );
    }

    public record Config(
        Consumer<Parent> setPage,
        Runnable rebuildSidebar,
        Supplier<Window> owner,
        AlertAction alert,
        Function<Throwable, String> rootCauseMessage,
        ConfirmAction confirm,
        ReviewMarkLabelFactory reviewMarkLabel
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

    @FunctionalInterface
    public interface ReviewMarkLabelFactory {
        Node apply(String source, String accountAlias, int year, int month);
    }
}
