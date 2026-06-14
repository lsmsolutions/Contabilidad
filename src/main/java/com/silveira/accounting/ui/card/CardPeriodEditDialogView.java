package com.silveira.accounting.ui.card;

import com.silveira.accounting.models.CreditCardStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class CardPeriodEditDialogView {
    public Optional<FormData> show(List<CreditCardStatement> statements, Function<CreditCardStatement, String> title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar datos de tarjeta");
        ComboBox<CreditCardStatement> selector = new ComboBox<>(FXCollections.observableArrayList(statements));
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(CreditCardStatement statement) {
                return statement == null ? "" : title.apply(statement);
            }

            @Override
            public CreditCardStatement fromString(String value) {
                return selector.getValue();
            }
        });
        selector.setValue(statements.get(0));

        DatePicker start = new DatePicker();
        DatePicker end = new DatePicker();
        DatePicker due = new DatePicker();
        DatePicker nextClosing = new DatePicker();
        TextField previous = moneyField();
        TextField payments = moneyField();
        TextField credits = moneyField();
        TextField purchases = moneyField();
        TextField transfers = moneyField();
        TextField cash = moneyField();
        TextField fees = moneyField();
        TextField interest = moneyField();
        TextField newBalance = moneyField();
        TextField minimum = moneyField();
        TextField limit = moneyField();
        TextField available = moneyField();
        TextField cashLimit = moneyField();
        TextField cashAvailable = moneyField();
        TextField rewardsBalance = moneyField();
        TextField rewardsPrevious = moneyField();
        TextField rewardsEarned = moneyField();
        TextField rewardsRedeemed = moneyField();
        CheckBox reviewed = new CheckBox("Revisado");
        TextArea notes = new TextArea();
        notes.setPrefRowCount(2);

        Runnable loadSelected = () -> {
            CreditCardStatement selected = selector.getValue();
            start.setValue(selected == null ? null : selected.getStatementStartDate());
            end.setValue(selected == null ? null : selected.getStatementEndDate());
            due.setValue(selected == null ? null : selected.getPaymentDueDate());
            nextClosing.setValue(selected == null ? null : selected.getNextClosingDate());
            setMoney(previous, selected == null ? 0 : selected.getPreviousBalance());
            setMoney(payments, selected == null ? 0 : selected.getPayments());
            setMoney(credits, selected == null ? 0 : selected.getOtherCredits());
            setMoney(purchases, selected == null ? 0 : selected.getTransactions());
            setMoney(transfers, selected == null ? 0 : selected.getBalanceTransfers());
            setMoney(cash, selected == null ? 0 : selected.getCashAdvances());
            setMoney(fees, selected == null ? 0 : selected.getFeesCharged());
            setMoney(interest, selected == null ? 0 : selected.getInterestCharged());
            setMoney(newBalance, selected == null ? 0 : selected.getNewBalance());
            setMoney(minimum, selected == null ? 0 : selected.getMinimumPaymentDue());
            setMoney(limit, selected == null ? 0 : selected.getCreditLimit());
            setMoney(available, selected == null ? 0 : selected.getAvailableCredit());
            setMoney(cashLimit, selected == null ? 0 : selected.getCashAdvanceLimit());
            setMoney(cashAvailable, selected == null ? 0 : selected.getAvailableCashAdvanceCredit());
            setMoney(rewardsBalance, selected == null ? 0 : selected.getRewardsBalance());
            setMoney(rewardsPrevious, selected == null ? 0 : selected.getRewardsPreviousBalance());
            setMoney(rewardsEarned, selected == null ? 0 : selected.getRewardsEarned());
            setMoney(rewardsRedeemed, selected == null ? 0 : selected.getRewardsRedeemed());
            reviewed.setSelected(selected != null && !selected.isPendingReview());
            notes.setText(selected == null ? "" : text(selected.getReviewNotes()));
        };
        loadSelected.run();
        selector.setOnAction(event -> loadSelected.run());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        int row = 0;
        if (statements.size() > 1) {
            form.addRow(row++, new Label("Resumen"), selector);
        }
        form.addRow(row++, new Label("Statement Start Date"), start);
        form.addRow(row++, new Label("Statement End Date"), end);
        form.addRow(row++, new Label("Payment Due Date"), due);
        form.addRow(row++, new Label("Next Statement Closing Date"), nextClosing);
        form.addRow(row++, new Label("Previous Balance"), previous);
        form.addRow(row++, new Label("Payments and Credits"), payments);
        form.addRow(row++, new Label("Other Credits"), credits);
        form.addRow(row++, new Label("Purchases"), purchases);
        form.addRow(row++, new Label("Balance Transfers"), transfers);
        form.addRow(row++, new Label("Cash Advances"), cash);
        form.addRow(row++, new Label("Fees Charged"), fees);
        form.addRow(row++, new Label("Interest Charged"), interest);
        form.addRow(row++, new Label("New Balance"), newBalance);
        form.addRow(row++, new Label("Minimum Payment Due"), minimum);
        form.addRow(row++, new Label("Credit Line"), limit);
        form.addRow(row++, new Label("Credit Line Available"), available);
        form.addRow(row++, new Label("Cash Advance Credit Line"), cashLimit);
        form.addRow(row++, new Label("Cash Advance Credit Line Available"), cashAvailable);
        form.addRow(row++, new Label("Rewards Balance"), rewardsBalance);
        form.addRow(row++, new Label("Previous Rewards"), rewardsPrevious);
        form.addRow(row++, new Label("Earned This Period"), rewardsEarned);
        form.addRow(row++, new Label("Redeemed This Period"), rewardsRedeemed);
        form.addRow(row++, new Label("Review"), reviewed);
        form.addRow(row, new Label("Notes"), notes);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(560);
        scroll.setPrefViewportHeight(620);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        return dialog.showAndWait()
            .filter(ButtonType.OK::equals)
            .map(result -> new FormData(
                selector.getValue(), start.getValue(), end.getValue(), due.getValue(), nextClosing.getValue(),
                previous.getText(), payments.getText(), credits.getText(), purchases.getText(), transfers.getText(),
                cash.getText(), fees.getText(), interest.getText(), newBalance.getText(), minimum.getText(),
                limit.getText(), available.getText(), cashLimit.getText(), cashAvailable.getText(),
                rewardsBalance.getText(), rewardsPrevious.getText(), rewardsEarned.getText(), rewardsRedeemed.getText(),
                reviewed.isSelected(), notes.getText()
            ));
    }

    private TextField moneyField() {
        return new TextField();
    }

    private void setMoney(TextField field, double value) {
        field.setText(String.format(Locale.US, "%.2f", value));
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    public record FormData(
        CreditCardStatement statement,
        LocalDate start,
        LocalDate end,
        LocalDate due,
        LocalDate nextClosing,
        String previous,
        String payments,
        String credits,
        String purchases,
        String transfers,
        String cash,
        String fees,
        String interest,
        String newBalance,
        String minimum,
        String limit,
        String available,
        String cashLimit,
        String cashAvailable,
        String rewardsBalance,
        String rewardsPrevious,
        String rewardsEarned,
        String rewardsRedeemed,
        boolean reviewed,
        String notes
    ) {
    }
}
