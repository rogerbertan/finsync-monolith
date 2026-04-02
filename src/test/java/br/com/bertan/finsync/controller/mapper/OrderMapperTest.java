package br.com.bertan.finsync.controller.mapper;

import br.com.bertan.finsync.controller.dto.OrderResponseDTO;
import br.com.bertan.finsync.controller.dto.OrderResponseDTO.OrderItemResponseDTO;
import br.com.bertan.finsync.model.Money;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderItem;
import br.com.bertan.finsync.model.order.OrderStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private static final Money PRICE = new Money(new BigDecimal("30.00"), "BRL");
    private static final String ORDER_REF = "ORD-001";

    private final OrderMapper mapper = new OrderMapper();

    private Order singleItemOrder() {
        return Order.place(ORDER_REF, List.of(OrderItem.of(null, "Product A", 1, PRICE)));
    }

    @Nested
    class ToResponse {

        @Test
        void shouldMapOrderFields() {
            Order order = singleItemOrder();

            OrderResponseDTO response = mapper.toResponse(order);

            assertThat(response.externalReference()).isEqualTo(ORDER_REF);
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(response.currency()).isEqualTo("BRL");
        }

        @Test
        void shouldMapOrderItem() {
            Order order = singleItemOrder();

            OrderResponseDTO response = mapper.toResponse(order);

            assertThat(response.items()).hasSize(1);
            OrderItemResponseDTO item = response.items().getFirst();
            assertThat(item.description()).isEqualTo("Product A");
            assertThat(item.quantity()).isEqualTo(1);
            assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(item.currency()).isEqualTo("BRL");
        }

        @Test
        void withMultipleItems_shouldMapAllItems() {
            Order order = Order.place(ORDER_REF, List.of(
                    OrderItem.of(null, "Product A", 2, PRICE),
                    OrderItem.of(null, "Product B", 3, new Money(new BigDecimal("10.00"), "BRL"))
            ));

            OrderResponseDTO response = mapper.toResponse(order);

            assertThat(response.items()).hasSize(2);
            assertThat(response.items()).extracting(OrderItemResponseDTO::description)
                    .containsExactly("Product A", "Product B");
        }

        @Test
        void withMultipleItems_shouldSumTotalCorrectly() {
            Order order = Order.place(ORDER_REF, List.of(
                    OrderItem.of(null, "Product A", 2, PRICE),
                    OrderItem.of(null, "Product B", 1, new Money(new BigDecimal("10.00"), "BRL"))
            ));

            OrderResponseDTO response = mapper.toResponse(order);

            assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("70.00"));
        }
    }
}