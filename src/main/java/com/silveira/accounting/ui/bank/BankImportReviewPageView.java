package com.silveira.accounting.ui.bank;

import com.silveira.accounting.application.bank.BankApplicationService;
import com.silveira.accounting.application.bank.dto.BankImportReview;
import com.silveira.accounting.models.bank.BankTransaction;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class BankImportReviewPageView {
    private final BankApplicationService bank;

    public BankImportReviewPageView(BankApplicationService bank) {
        this.bank = bank;
    }

    public Page build(List<BankTransaction> parsed, String selectedAccountAlias) {
        BankImportReview importReview = bank.imports().prepareReview(parsed, selectedAccountAlias);
        List<BankTransaction> newTransactions = importReview.newTransactions();

        TableView<BankTransaction> table = new BankTransactionTableView(bank).build();
        table.setItems(FXCollections.observableArrayList(newTransactions));

        VBox warnings = warningBox(reviewWarnings(newTransactions.size(), importReview.existingCount()));

        Button saveProgress = new Button("Guardar progreso");
        saveProgress.getStyleClass().add("primary");

        return new Page(table, new VBox(10, warnings, saveProgress), saveProgress);
    }

    private List<String> reviewWarnings(int newCount, int existingCount) {
        if (newCount == 0 && existingCount > 0) {
            return List.of(
                "Este periodo ya esta registrado en la app.",
                existingCount + " transacciones ya existian y se ocultaron porque coinciden con movimientos guardados."
            );
        }
        if (existingCount == 0) {
            return List.of(newCount + " transacciones nuevas detectadas. Marca como revisadas las verificadas contra el PDF.");
        }
        return List.of(newCount + " transacciones nuevas detectadas.", existingCount + " transacciones ya existian y se ocultaron.");
    }

    private VBox warningBox(List<String> warnings) {
        VBox box = new VBox(4);
        if (warnings.isEmpty()) {
            Label ok = new Label("Validacion inicial sin alertas. Revise igualmente antes de guardar.");
            ok.getStyleClass().add("review-ok");
            box.getChildren().add(ok);
            return box;
        }
        box.getStyleClass().add("review-warning");
        Label title = new Label("Alertas de revision");
        title.getStyleClass().add("review-warning-title");
        box.getChildren().add(title);
        for (String warning : warnings) {
            box.getChildren().add(new Label(warning));
        }
        return box;
    }

    public record Page(TableView<BankTransaction> table, Node warningNode, Button saveProgress) {}
}
