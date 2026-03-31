package br.com.bertan.finsync.model.reconciliation;

import br.com.bertan.finsync.exception.InvalidReconciliationStateException;
import br.com.bertan.finsync.model.Money;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderItem;
import br.com.bertan.finsync.model.payment.Payment;
import br.com.bertan.finsync.model.payment.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ReconciliationServiceTest {

    private static final String ORDER_REF = "ORD-001";
    private static final Money AMOUNT_100 = new Money(new BigDecimal("100.00"), "BRL");
    private static final Money AMOUNT_90 = new Money(new BigDecimal("90.00"), "BRL");

    private final ReconciliationService reconciliationService = new ReconciliationService();

    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        pendingOrder = Order.place(ORDER_REF, List.of(
                OrderItem.of(null, "Product", 1, AMOUNT_100)
        ));
    }

    @Nested
    class Reconcile {

        @Test
        void withMatchingAmounts_shouldReturnMatchedResult() {
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT_100, PaymentMethod.PIX);

            ReconciliationResult result = reconciliationService.reconcile(pendingOrder, payment);

            assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        }

        @Test
        void withMatchingAmounts_shouldLinkOrderAndPayment() {
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT_100, PaymentMethod.PIX);

            ReconciliationResult result = reconciliationService.reconcile(pendingOrder, payment);

            assertThat(result.getOrder()).isEqualTo(pendingOrder);
            assertThat(result.getPayment()).isEqualTo(payment);
        }

        @Test
        void withDifferentAmounts_shouldReturnDivergentResult() {
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT_90, PaymentMethod.PIX);

            ReconciliationResult result = reconciliationService.reconcile(pendingOrder, payment);

            assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.DIVERGED);
        }

        @Test
        void withDifferentAmounts_shouldRecordDivergenceReason() {
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT_90, PaymentMethod.PIX);

            ReconciliationResult result = reconciliationService.reconcile(pendingOrder, payment);

            assertThat(result.getDivergenceReason())
                    .isEqualTo("Expected 100.00 BRL but received 90.00 BRL");
        }

        @Test
        void whenPaymentNotEligible_shouldThrow() {
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT_100, PaymentMethod.PIX);
            payment.markProcessed();

            assertThatExceptionOfType(InvalidReconciliationStateException.class)
                    .isThrownBy(() -> reconciliationService.reconcile(pendingOrder, payment))
                    .withMessage("Payment is not eligible for reconciliation");
        }

        @Test
        void whenOrderNotPending_shouldThrow() {
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT_100, PaymentMethod.PIX);
            pendingOrder.cancel();

            assertThatExceptionOfType(InvalidReconciliationStateException.class)
                    .isThrownBy(() -> reconciliationService.reconcile(pendingOrder, payment))
                    .withMessage("Order must be in PENDING status to be reconciled");
        }

        @Test
        void withSameAmountButDifferentCurrency_shouldReturnDivergentResult() {
            Money usdAmount = new Money(new BigDecimal("100.00"), "USD");
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, usdAmount, PaymentMethod.PIX);

            ReconciliationResult result = reconciliationService.reconcile(pendingOrder, payment);

            assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.DIVERGED);
            assertThat(result.getDivergenceReason()).contains("BRL").contains("USD");
        }
    }
}