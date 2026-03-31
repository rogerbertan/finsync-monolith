package br.com.bertan.finsync.controller.dto;

import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.model.payment.PaymentMethod;
import br.com.bertan.finsync.model.payment.PaymentStatus;
import br.com.bertan.finsync.model.reconciliation.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReconciliationResultResponse(
        UUID id,
        ReconciliationStatus status,
        String divergenceReason,
        LocalDateTime reconciledAt,
        OrderSummary order,
        PaymentSummary payment
) {

    public record OrderSummary(
            UUID id,
            String externalReference,
            BigDecimal amount,
            String currency,
            OrderStatus status
    ) {}

    public record PaymentSummary(
            UUID id,
            String gatewayPaymentId,
            String orderExternalReference,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            PaymentStatus status
    ) {}
}