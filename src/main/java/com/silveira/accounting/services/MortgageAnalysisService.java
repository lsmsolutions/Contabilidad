package com.silveira.accounting.services;

import com.silveira.accounting.models.MortgageAlert;
import com.silveira.accounting.models.MortgageAnalysis;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.utils.Money;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class MortgageAnalysisService {
    public MortgageAnalysis analyze(MortgageStatement statement) {
        double total = statement.getTotalDue() > 0 ? statement.getTotalDue() : statement.getPaymentAmountDue();
        int days = statement.getPaymentDueDate() == null ? 0 : (int) ChronoUnit.DAYS.between(LocalDate.now(), statement.getPaymentDueDate());
        String status = statement.getPaymentDueDate() == null ? "pendiente" : days < 0 ? "vencido" : "pendiente";
        double principalPercent = percent(statement.getPrincipalDue(), total);
        double interestPercent = percent(statement.getInterestDue(), total);
        double escrowPercent = percent(statement.getEscrowDue(), total);
        double debtReduction = statement.getOriginalPrincipalBalance() == 0 ? 0 :
            (statement.getOriginalPrincipalBalance() - statement.getOutstandingPrincipalBalance()) / statement.getOriginalPrincipalBalance() * 100;
        double interestPrincipal = statement.getPrincipalDue() == 0 ? 0 : statement.getInterestDue() / statement.getPrincipalDue();
        List<MortgageAlert> alerts = alerts(statement, days, principalPercent, interestPrincipal);
        return new MortgageAnalysis(days, status, principalPercent, interestPercent, escrowPercent, debtReduction, interestPrincipal, alerts);
    }

    private List<MortgageAlert> alerts(MortgageStatement s, int days, double principalPercent, double interestPrincipal) {
        List<MortgageAlert> alerts = new ArrayList<>();
        if (s.getPaymentDueDate() != null) {
            if (days < 0) alerts.add(alert("alta", "Pago vencido", "Pago vencido. Puede aplicar penalizacion y cargos adicionales."));
            else if (days == 1) alerts.add(alert("alta", "Vence manana", "Alerta critica: el pago vence manana."));
            else if (days <= 3) alerts.add(alert("media", "Vence pronto", "Atención: el pago vence en pocos días."));
            else if (days <= 7) alerts.add(alert("media", "Vence pronto", "Tu pago vence pronto."));
        }
        if (s.getLateFeeAmount() > 0 || s.getLateFeeDate() != null) alerts.add(alert("media", "Late fee posible", "El statement indica posible late fee."));
        if (s.getPastDueAmount() > 0) alerts.add(alert("alta", "Importes vencidos", "Hay importes vencidos por " + Money.format(s.getPastDueAmount()) + "."));
        if (s.getFees() + s.getOtherFeesAndCharges() > 0) alerts.add(alert("media", "Cargos adicionales", "Hay fees o cargos adicionales en el statement."));
        if (s.getUnappliedFunds() > 0) alerts.add(alert("media", "Fondos no aplicados", "Hay fondos no aplicados por " + Money.format(s.getUnappliedFunds()) + "."));
        if (s.getInterestDue() > s.getPrincipalDue() && s.getInterestDue() > 0) alerts.add(alert("media", "Interés mayor que principal", "Este mes los intereses superan la parte que reduce deuda."));
        if (principalPercent > 0 && principalPercent < 25) alerts.add(alert("media", "Baja reduccion de deuda", "Solo el " + String.format("%.1f%%", principalPercent) + " del pago reduce principal."));
        if (interestPrincipal > 1) alerts.add(alert("media", "Coste financiero alto", "La relación intereses/principal es elevada."));
        return alerts;
    }

    private MortgageAlert alert(String severity, String title, String message) {
        return new MortgageAlert(0, 0, severity, title, message);
    }

    private double percent(double value, double total) {
        return total == 0 ? 0 : value / total * 100;
    }
}
