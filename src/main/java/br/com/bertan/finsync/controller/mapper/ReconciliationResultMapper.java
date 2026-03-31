package br.com.bertan.finsync.controller.mapper;

import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse;
import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse.OrderSummary;
import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse.PaymentSummary;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.payment.Payment;
import br.com.bertan.finsync.model.reconciliation.ReconciliationResult;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationResultMapper {

    public ReconciliationResultResponse toResponse(ReconciliationResult result) {
        return new ReconciliationResultResponse(
                result.getId(),
                result.getStatus(),
                result.getDivergenceReason(),
                result.getReconciledAt(),
                toOrderSummary(result.getOrder()),
                toPaymentSummary(result.getPayment())
        );
    }

    private OrderSummary toOrderSummary(Order order) {
        if (order == null) return null;
        return new OrderSummary(
                order.getId(),
                order.getExternalReference(),
                order.getAmount().getValue(),
                order.getAmount().getCurrency(),
                order.getStatus()
        );
    }

    private PaymentSummary toPaymentSummary(Payment payment) {
        return new PaymentSummary(
                payment.getId(),
                payment.getGatewayPaymentId(),
                payment.getOrderExternalReference(),
                payment.getAmount().getValue(),
                payment.getAmount().getCurrency(),
                payment.getMethod(),
                payment.getStatus()
        );
    }
}