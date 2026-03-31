package br.com.bertan.finsync.service;

import br.com.bertan.finsync.model.Money;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderItem;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.model.payment.Payment;
import br.com.bertan.finsync.model.payment.PaymentMethod;
import br.com.bertan.finsync.model.payment.PaymentStatus;
import br.com.bertan.finsync.model.reconciliation.ReconciliationResult;
import br.com.bertan.finsync.model.reconciliation.ReconciliationService;
import br.com.bertan.finsync.model.reconciliation.ReconciliationStatus;
import br.com.bertan.finsync.repository.OrderRepository;
import br.com.bertan.finsync.repository.PaymentRepository;
import br.com.bertan.finsync.repository.ReconciliationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationApplicationServiceTest {

    private static final String ORDER_REF = "ORD-001";
    private static final Money AMOUNT = new Money(new BigDecimal("100.00"), "BRL");

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ReconciliationResultRepository reconciliationResultRepository;

    private ReconciliationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationApplicationService(
                orderRepository,
                paymentRepository,
                reconciliationResultRepository,
                new ReconciliationService()
        );
    }

    @Nested
    class RunReconciliation {

        @Test
        void withMatchingOrderAndPayment_shouldSaveMatchedResult() {
            Order order = Order.place(ORDER_REF, List.of(OrderItem.of(null, "Product", 1, AMOUNT)));
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT, PaymentMethod.PIX);

            when(orderRepository.findAllByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
            when(paymentRepository.findByOrderExternalReferenceAndStatus(ORDER_REF, PaymentStatus.RECEIVED))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.findReceivedWithoutMatchingOrder()).thenReturn(List.of());

            service.runReconciliation();

            ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
            verify(reconciliationResultRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        }

        @Test
        void withMatchingOrderAndPayment_shouldMarkOrderPaidAndPaymentProcessed() {
            Order order = Order.place(ORDER_REF, List.of(OrderItem.of(null, "Product", 1, AMOUNT)));
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, AMOUNT, PaymentMethod.PIX);

            when(orderRepository.findAllByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
            when(paymentRepository.findByOrderExternalReferenceAndStatus(ORDER_REF, PaymentStatus.RECEIVED))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.findReceivedWithoutMatchingOrder()).thenReturn(List.of());

            service.runReconciliation();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSED);
        }

        @Test
        void withDivergentAmounts_shouldSaveDivergentResult_andNotPayOrder() {
            Money differentAmount = new Money(new BigDecimal("90.00"), "BRL");
            Order order = Order.place(ORDER_REF, List.of(OrderItem.of(null, "Product", 1, AMOUNT)));
            Payment payment = Payment.receive("gw-001", "idem-001", ORDER_REF, differentAmount, PaymentMethod.PIX);

            when(orderRepository.findAllByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
            when(paymentRepository.findByOrderExternalReferenceAndStatus(ORDER_REF, PaymentStatus.RECEIVED))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.findReceivedWithoutMatchingOrder()).thenReturn(List.of());

            service.runReconciliation();

            ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
            verify(reconciliationResultRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReconciliationStatus.DIVERGED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        void withOrphanPayment_shouldSaveUnmatchedResult() {
            Payment orphan = Payment.receive("gw-999", "idem-999", "UNKNOWN-REF", AMOUNT, PaymentMethod.BOLETO);

            when(orderRepository.findAllByStatus(OrderStatus.PENDING)).thenReturn(List.of());
            when(paymentRepository.findReceivedWithoutMatchingOrder()).thenReturn(List.of(orphan));

            service.runReconciliation();

            ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
            verify(reconciliationResultRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(ReconciliationStatus.UNMATCHED);
            assertThat(captor.getValue().getOrder()).isNull();
        }

        @Test
        void withPendingOrderButNoMatchingPayment_shouldNotSaveAnyResult() {
            Order order = Order.place(ORDER_REF, List.of(OrderItem.of(null, "Product", 1, AMOUNT)));

            when(orderRepository.findAllByStatus(OrderStatus.PENDING)).thenReturn(List.of(order));
            when(paymentRepository.findByOrderExternalReferenceAndStatus(ORDER_REF, PaymentStatus.RECEIVED))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findReceivedWithoutMatchingOrder()).thenReturn(List.of());

            service.runReconciliation();

            verify(reconciliationResultRepository, never()).save(any());
        }
    }
}