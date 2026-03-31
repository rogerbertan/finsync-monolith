package br.com.bertan.finsync.model.payment;

import br.com.bertan.finsync.exception.InvalidPaymentStateException;
import br.com.bertan.finsync.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    private static final String GATEWAY_ID = "gw-abc-123";
    private static final String IDEM_KEY = "idem-xyz-999";
    private static final String ORDER_REF = "ORD-001";
    private static final Money AMOUNT = new Money(new BigDecimal("150.00"), "BRL");
    private static final PaymentMethod METHOD = PaymentMethod.PIX;

    @Nested
    class Receive {

        @Test
        void shouldCreatePaymentWithReceivedStatus() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);
        }

        @Test
        void shouldStoreGatewayPaymentId() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getGatewayPaymentId()).isEqualTo(GATEWAY_ID);
        }

        @Test
        void shouldStoreIdempotencyKey() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getIdempotencyKey()).isEqualTo(IDEM_KEY);
        }

        @Test
        void shouldStoreAmount() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getAmount()).isEqualTo(AMOUNT);
        }

        @Test
        void shouldStorePaymentMethod() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getMethod()).isEqualTo(METHOD);
        }

        @Test
        void shouldNotSetReceivedAtBeforePersistence() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getReceivedAt()).isNull();
        }

        @Test
        void shouldNotSetProcessedAtOnCreation() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getProcessedAt()).isNull();
        }

        @Test
        void withNullGatewayPaymentId_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive(null, IDEM_KEY, ORDER_REF, AMOUNT, METHOD))
                    .withMessage("Gateway payment ID cannot be blank");
        }

        @Test
        void withBlankGatewayPaymentId_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive("  ", IDEM_KEY, ORDER_REF, AMOUNT, METHOD))
                    .withMessage("Gateway payment ID cannot be blank");
        }

        @Test
        void shouldStoreOrderExternalReference() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.getOrderExternalReference()).isEqualTo(ORDER_REF);
        }

        @Test
        void withNullIdempotencyKey_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive(GATEWAY_ID, null, ORDER_REF, AMOUNT, METHOD))
                    .withMessage("Idempotency key cannot be blank");
        }

        @Test
        void withBlankIdempotencyKey_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive(GATEWAY_ID, "", ORDER_REF, AMOUNT, METHOD))
                    .withMessage("Idempotency key cannot be blank");
        }

        @Test
        void withNullOrderExternalReference_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive(GATEWAY_ID, IDEM_KEY, null, AMOUNT, METHOD))
                    .withMessage("Order external reference cannot be blank");
        }

        @Test
        void withBlankOrderExternalReference_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive(GATEWAY_ID, IDEM_KEY, "  ", AMOUNT, METHOD))
                    .withMessage("Order external reference cannot be blank");
        }

        @Test
        void withNullAmount_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, null, METHOD))
                    .withMessage("Amount cannot be null");
        }

        @Test
        void withNullPaymentMethod_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, null))
                    .withMessage("Payment method cannot be null");
        }
    }

    @Nested
    class MarkProcessed {

        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);
        }

        @Test
        void fromReceived_shouldTransitionToProcessed() {
            payment.markProcessed();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSED);
        }

        @Test
        void fromReceived_shouldSetProcessedAt() {
            payment.markProcessed();

            assertThat(payment.getProcessedAt())
                    .isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        void whenAlreadyProcessed_shouldThrow() {
            payment.markProcessed();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(payment::markProcessed)
                    .withMessage("Payment has already been processed");
        }

        @Test
        void whenFailed_shouldThrow() {
            payment.markFailed();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(payment::markProcessed)
                    .withMessage("Cannot process a failed payment");
        }
    }

    @Nested
    class MarkFailed {

        private Payment payment;

        @BeforeEach
        void setUp() {
            payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);
        }

        @Test
        void fromReceived_shouldTransitionToFailed() {
            payment.markFailed();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        void fromReceived_shouldNotSetProcessedAt() {
            payment.markFailed();

            assertThat(payment.getProcessedAt()).isNull();
        }

        @Test
        void whenAlreadyFailed_shouldThrow() {
            payment.markFailed();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(payment::markFailed)
                    .withMessage("Payment has already been marked as failed");
        }

        @Test
        void whenProcessed_shouldThrow() {
            payment.markProcessed();

            assertThatExceptionOfType(InvalidPaymentStateException.class)
                    .isThrownBy(payment::markFailed)
                    .withMessage("Cannot fail a payment that has already been processed");
        }
    }

    @Nested
    class IsEligibleForReconciliation {

        @Test
        void whenReceived_shouldBeEligible() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);

            assertThat(payment.isEligibleForReconciliation()).isTrue();
        }

        @Test
        void whenProcessed_shouldNotBeEligible() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);
            payment.markProcessed();

            assertThat(payment.isEligibleForReconciliation()).isFalse();
        }

        @Test
        void whenFailed_shouldNotBeEligible() {
            Payment payment = Payment.receive(GATEWAY_ID, IDEM_KEY, ORDER_REF, AMOUNT, METHOD);
            payment.markFailed();

            assertThat(payment.isEligibleForReconciliation()).isFalse();
        }
    }
}