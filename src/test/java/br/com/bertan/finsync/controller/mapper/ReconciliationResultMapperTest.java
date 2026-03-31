package br.com.bertan.finsync.controller.mapper;

import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse;
import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse.OrderSummary;
import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse.PaymentSummary;
import br.com.bertan.finsync.model.Money;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderItem;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.model.payment.Payment;
import br.com.bertan.finsync.model.payment.PaymentMethod;
import br.com.bertan.finsync.model.payment.PaymentStatus;
import br.com.bertan.finsync.model.reconciliation.ReconciliationResult;
import br.com.bertan.finsync.model.reconciliation.ReconciliationStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationResultMapperTest {

    private static final Money AMOUNT = new Money(new BigDecimal("150.00"), "BRL");
    private static final String ORDER_REF = "ORD-001";

    private final ReconciliationResultMapper mapper = new ReconciliationResultMapper();

    private Order pendingOrder() {
        return Order.place(ORDER_REF, List.of(OrderItem.of(null, "Product", 1, AMOUNT)));
    }

    private Payment receivedPayment() {
        return Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT, PaymentMethod.PIX);
    }

    @Nested
    class ToResponse {

        @Test
        void withMatchedResult_shouldMapStatusAndIds() {
            Order order = pendingOrder();
            Payment payment = receivedPayment();
            ReconciliationResult result = ReconciliationResult.matched(order, payment);

            ReconciliationResultResponse response = mapper.toResponse(result);

            assertThat(response.status()).isEqualTo(ReconciliationStatus.MATCHED);
            assertThat(response.divergenceReason()).isNull();
        }

        @Test
        void withMatchedResult_shouldMapOrderSummary() {
            Order order = pendingOrder();
            Payment payment = receivedPayment();
            ReconciliationResult result = ReconciliationResult.matched(order, payment);

            ReconciliationResultResponse response = mapper.toResponse(result);

            OrderSummary orderSummary = response.order();
            assertThat(orderSummary).isNotNull();
            assertThat(orderSummary.externalReference()).isEqualTo(ORDER_REF);
            assertThat(orderSummary.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(orderSummary.currency()).isEqualTo("BRL");
            assertThat(orderSummary.status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        void withMatchedResult_shouldMapPaymentSummary() {
            Order order = pendingOrder();
            Payment payment = receivedPayment();
            ReconciliationResult result = ReconciliationResult.matched(order, payment);

            ReconciliationResultResponse response = mapper.toResponse(result);

            PaymentSummary paymentSummary = response.payment();
            assertThat(paymentSummary).isNotNull();
            assertThat(paymentSummary.gatewayPaymentId()).isEqualTo("gw-001");
            assertThat(paymentSummary.orderExternalReference()).isEqualTo(ORDER_REF);
            assertThat(paymentSummary.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
            assertThat(paymentSummary.currency()).isEqualTo("BRL");
            assertThat(paymentSummary.method()).isEqualTo(PaymentMethod.PIX);
            assertThat(paymentSummary.status()).isEqualTo(PaymentStatus.RECEIVED);
        }

        @Test
        void withDivergentResult_shouldMapDivergenceReason() {
            Order order = pendingOrder();
            Money differentAmount = new Money(new BigDecimal("100.00"), "BRL");
            Payment payment = Payment.receive("gw-002", "idem-002", ORDER_REF, differentAmount, PaymentMethod.BOLETO);
            ReconciliationResult result = ReconciliationResult.divergent(order, payment, "Expected 150.00 BRL but received 100.00 BRL");

            ReconciliationResultResponse response = mapper.toResponse(result);

            assertThat(response.status()).isEqualTo(ReconciliationStatus.DIVERGED);
            assertThat(response.divergenceReason()).isEqualTo("Expected 150.00 BRL but received 100.00 BRL");
            assertThat(response.order()).isNotNull();
            assertThat(response.payment()).isNotNull();
        }

        @Test
        void withUnmatchedResult_shouldMapNullOrderSummary() {
            Payment payment = Payment.receive("gw-999", "idem-999", "UNKNOWN-REF", AMOUNT, PaymentMethod.CARD);
            ReconciliationResult result = ReconciliationResult.suspiciousPayment(payment);

            ReconciliationResultResponse response = mapper.toResponse(result);

            assertThat(response.status()).isEqualTo(ReconciliationStatus.UNMATCHED);
            assertThat(response.order()).isNull();
            assertThat(response.payment()).isNotNull();
            assertThat(response.payment().gatewayPaymentId()).isEqualTo("gw-999");
            assertThat(response.payment().method()).isEqualTo(PaymentMethod.CARD);
        }

        @Test
        void withDifferentPaymentMethods_shouldMapMethodCorrectly() {
            Order order = pendingOrder();
            Payment boletoPayment = Payment.receive("gw-003", "idem-003", ORDER_REF, AMOUNT, PaymentMethod.BOLETO);
            ReconciliationResult result = ReconciliationResult.matched(order, boletoPayment);

            ReconciliationResultResponse response = mapper.toResponse(result);

            assertThat(response.payment().method()).isEqualTo(PaymentMethod.BOLETO);
        }
    }
}