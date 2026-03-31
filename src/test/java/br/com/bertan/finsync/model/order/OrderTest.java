package br.com.bertan.finsync.model.order;

import br.com.bertan.finsync.exception.InvalidOrderStateException;
import br.com.bertan.finsync.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    private OrderItem itemA;
    private OrderItem itemB;

    @BeforeEach
    void setUp() {
        // 2 × R$ 10.00 = R$ 20.00
        itemA = OrderItem.of(null, "Product A", 2, new Money(new BigDecimal("10.00"), "BRL"));
        // 3 × R$ 5.00 = R$ 15.00
        itemB = OrderItem.of(null, "Product B", 3, new Money(new BigDecimal("5.00"), "BRL"));
    }

    @Nested
    class Place {

        @Test
        void shouldCreateOrderWithPendingStatus() {
            Order order = Order.place("ERP-001", List.of(itemA));

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        void shouldCalculateTotalFromAllItems() {
            Order order = Order.place("ERP-001", List.of(itemA, itemB));

            // (2 × 10.00) + (3 × 5.00) = 35.00
            assertThat(order.getAmount().getValue()).isEqualByComparingTo("35.00");
            assertThat(order.getAmount().getCurrency()).isEqualTo("BRL");
        }

        @Test
        void shouldCalculateTotalFromSingleItem() {
            Order order = Order.place("ERP-001", List.of(itemA));

            assertThat(order.getAmount().getValue()).isEqualByComparingTo("20.00");
        }

        @Test
        void shouldStoreExternalReference() {
            Order order = Order.place("ERP-001", List.of(itemA));

            assertThat(order.getExternalReference()).isEqualTo("ERP-001");
        }

        @Test
        void shouldAttachItemsToOrder() {
            Order order = Order.place("ERP-001", List.of(itemA, itemB));

            assertThat(order.getItems()).hasSize(2);
            assertThat(order.getItems()).containsExactly(itemA, itemB);
        }

        @Test
        void shouldLinkEachItemBackToOrder() {
            Order order = Order.place("ERP-001", List.of(itemA, itemB));

            assertThat(itemA.getOrder()).isSameAs(order);
            assertThat(itemB.getOrder()).isSameAs(order);
        }

        @Test
        void withNullExternalReference_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Order.place(null, List.of(itemA)))
                    .withMessage("External reference cannot be blank");
        }

        @Test
        void withBlankExternalReference_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Order.place("  ", List.of(itemA)))
                    .withMessage("External reference cannot be blank");
        }

        @Test
        void withNullItemsList_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Order.place("ERP-001", null))
                    .withMessage("Order must have at least one item");
        }

        @Test
        void withEmptyItemsList_shouldThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Order.place("ERP-001", Collections.emptyList()))
                    .withMessage("Order must have at least one item");
        }
    }

    @Nested
    class Pay {

        @Test
        void fromPending_shouldTransitionToPaid() {
            Order order = Order.place("ERP-001", List.of(itemA));

            order.pay();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        void whenAlreadyPaid_shouldThrow() {
            Order order = Order.place("ERP-001", List.of(itemA));
            order.pay();

            assertThatExceptionOfType(InvalidOrderStateException.class)
                    .isThrownBy(order::pay)
                    .withMessage("Order has already been paid");
        }

        @Test
        void whenCancelled_shouldThrow() {
            Order order = Order.place("ERP-001", List.of(itemA));
            order.cancel();

            assertThatExceptionOfType(InvalidOrderStateException.class)
                    .isThrownBy(order::pay)
                    .withMessage("Cannot pay a cancelled order");
        }
    }

    @Nested
    class Cancel {

        @Test
        void fromPending_shouldTransitionToCancelled() {
            Order order = Order.place("ERP-001", List.of(itemA));

            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        void whenAlreadyCancelled_shouldThrow() {
            Order order = Order.place("ERP-001", List.of(itemA));
            order.cancel();

            assertThatExceptionOfType(InvalidOrderStateException.class)
                    .isThrownBy(order::cancel)
                    .withMessage("Order has already been cancelled");
        }

        @Test
        void whenPaid_shouldThrow() {
            Order order = Order.place("ERP-001", List.of(itemA));
            order.pay();

            assertThatExceptionOfType(InvalidOrderStateException.class)
                    .isThrownBy(order::cancel)
                    .withMessage("Cannot cancel a paid order");
        }
    }
}