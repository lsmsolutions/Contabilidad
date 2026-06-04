package com.silveira.accounting.services;

import com.silveira.accounting.models.CreditCardAnalysis;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.FinancialAlert;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class CreditCardAnalysisService {
    public CreditCardAnalysis analyze(CreditCardStatement statement) {
        double calculated = statement.getPreviousBalance()
            - statement.getPayments()
            - statement.getOtherCredits()
            + statement.getTransactions()
            + statement.getBalanceTransfers()
            + statement.getCashAdvances()
            + statement.getFeesCharged()
            + statement.getInterestCharged();
        double diff = calculated - statement.getNewBalance();
        double utilization = statement.getCreditLimit() == 0 ? 0 : statement.getNewBalance() / statement.getCreditLimit() * 100;
        double interestRatio = statement.getTransactions() == 0 ? 0 : statement.getInterestCharged() / statement.getTransactions() * 100;
        long days = statement.getPaymentDueDate() == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), statement.getPaymentDueDate());
        String status = days < 0 ? "vencido" : "pendiente";
        String risk = utilization > 80 ? "peligroso" : utilization > 50 ? "elevado" : utilization > 30 ? "moderado" : "saludable";
        double lateFee = 40.0;
        List<FinancialAlert> alerts = alerts(statement, days, utilization, calculated, diff);
        return new CreditCardAnalysis(calculated, diff, utilization, interestRatio, days, status, risk, lateFee, alerts);
    }

    private List<FinancialAlert> alerts(CreditCardStatement statement, long days, double utilization, double calculated, double diff) {
        List<FinancialAlert> alerts = new ArrayList<>();
        if (days < 0) alerts.add(alert("critica", "Pago vencido", "Pago vencido. Puede aplicar penalización y cargos adicionales."));
        else if (days == 1) alerts.add(alert("critica", "Pago vence mañana", "Alerta crítica: el pago vence mañana."));
        else if (days <= 3) alerts.add(alert("alta", "Pago vence en pocos días", "Atención: el pago vence en pocos días."));
        else if (days <= 7) alerts.add(alert("media", "Pago vence pronto", "Tu pago vence pronto."));
        if (utilization > 80) alerts.add(alert("critica", "Uso de crédito peligroso", "Uso de crédito peligroso."));
        else if (utilization > 50) alerts.add(alert("alta", "Uso de crédito elevado", "Uso de crédito elevado."));
        else if (utilization > 30) alerts.add(alert("media", "Uso de crédito alto", "Uso de crédito por encima del nivel recomendado."));
        if (statement.getInterestCharged() > 0) alerts.add(alert("alta", "Intereses cargados", "Esta tarjeta está generando intereses. Conviene revisar estrategia de pago."));
        if (statement.getMinimumPaymentDue() > 0) alerts.add(alert("media", "Pago mínimo", "Si solo pagas el mínimo, la deuda puede tardar años en liquidarse."));
        if (Math.abs(diff) > 0.05) alerts.add(alert("alta", "Saldo no cuadra", "El saldo calculado no coincide con la deuda al cierre del mes."));
        return alerts;
    }

    private FinancialAlert alert(String severity, String title, String message) {
        return new FinancialAlert(0, 0, severity, title, message);
    }
}
