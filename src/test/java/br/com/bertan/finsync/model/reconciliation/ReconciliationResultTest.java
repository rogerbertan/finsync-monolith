package br.com.bertan.finsync.model.reconciliation;

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

class ReconciliationResultTest {

    private static final String ORDER_REF = "ORD-001";
    private static final Money AMOUNT = new Money(new BigDecimal("100.00"), "BRL");

    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = Order.place(ORDER_REF, List.of(
                OrderItem.of(null, "Product", 1, AMOUNT)
        ));
        payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT, PaymentMethod.PIX);
    }

    @Nested
    class Matched {

        @Test
        void shouldCreateWithMatchedStatus() {
            ReconciliationResult result = ReconciliationResult.matched(order, payment);

            assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        }

        @Test
        void shouldStoreOrderAndPayment() {
            ReconciliationResult result = ReconciliationResult.matched(order, payment);

            assertThat(result.getOrder()).isEqualTo(order);
            assertThat(result.getPayment()).isEqualTo(payment);
        }

        @Test
        void shouldNotSetDivergenceReason() {
            ReconciliationResult result = ReconciliationResult.matched(order, payment);

            assertThat(result.getDivergenceReason()).isNull();
        }

        @Test
        void withNullOrder_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ReconciliationResult.matched(null, payment))
                    .withMessage("Order cannot be null for a MATCHED result");
        }

        @Test
        void withNullPayment_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ReconciliationResult.matched(order, null))
                    .withMessage("Payment cannot be null for a MATCHED result");
        }
    }

    @Nested
    class Divergent {

        private static final String REASON = "Expected 100.00 BRL but received 90.00 BRL";

        @Test
        void shouldCreateWithDivergedStatus() {
            ReconciliationResult result = ReconciliationResult.divergent(order, payment, REASON);

            assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.DIVERGED);
        }

        @Test
        void shouldStoreDivergenceReason() {
            ReconciliationResult result = ReconciliationResult.divergent(order, payment, REASON);

            assertThat(result.getDivergenceReason()).isEqualTo(REASON);
        }

        @Test
        void shouldStoreOrderAndPayment() {
            ReconciliationResult result = ReconciliationResult.divergent(order, payment, REASON);

            assertThat(result.getOrder()).isEqualTo(order);
            assertThat(result.getPayment()).isEqualTo(payment);
        }

        @Test
        void withNullOrder_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ReconciliationResult.divergent(null, payment, REASON))
                    .withMessage("Order cannot be null for a DIVERGED result");
        }

        @Test
        void withNullPayment_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ReconciliationResult.divergent(order, null, REASON))
                    .withMessage("Payment cannot be null for a DIVERGED result");
        }

        @Test
        void withNullReason_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ReconciliationResult.divergent(order, payment, null))
                    .withMessage("Divergence reason cannot be blank");
        }

        @Test
        void withBlankReason_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ReconciliationResult.divergent(order, payment, "  "))
                    .withMessage("Divergence reason cannot be blank");
        }
    }

    @Nested
    class SuspiciousPayment {

        @Test
        void shouldCreateWithUnmatchedStatus() {
            ReconciliationResult result = ReconciliationResult.suspiciousPayment(payment);

            assertThat(result.getStatus()).isEqualTo(ReconciliationStatus.UNMATCHED);
        }

        @Test
        void shouldStorePayment() {
            ReconciliationResult result = ReconciliationResult.suspiciousPayment(payment);

            assertThat(result.getPayment()).isEqualTo(payment);
        }

        @Test
        void shouldHaveNullOrder() {
            ReconciliationResult result = ReconciliationResult.suspiciousPayment(payment);

            assertThat(result.getOrder()).isNull();
        }

        @Test
        void withNullPayment_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> ReconciliationResult.suspiciousPayment(null))
                    .withMessage("Payment cannot be null for an UNMATCHED result");
        }
    }
}