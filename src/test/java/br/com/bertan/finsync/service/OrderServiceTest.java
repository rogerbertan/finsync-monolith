package br.com.bertan.finsync.service;

import br.com.bertan.finsync.model.Money;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderItem;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Money PRICE = new Money(new BigDecimal("50.00"), "BRL");

    @Mock private OrderRepository orderRepository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository);
    }

    private Order pendingOrder() {
        return Order.place("ORD-001", List.of(OrderItem.of(null, "Product", 2, PRICE)));
    }

    @Nested
    class Place {

        @Test
        void shouldCreateAndSaveOrder() {
            List<OrderItem> items = List.of(OrderItem.of(null, "Product", 2, PRICE));
            Order saved = Order.place("ORD-001", items);
            when(orderRepository.save(any(Order.class))).thenReturn(saved);

            Order result = service.place("ORD-001", items);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            assertThat(captor.getValue().getExternalReference()).isEqualTo("ORD-001");
            assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(result).isSameAs(saved);
        }

        @Test
        void shouldCalculateTotalFromItems() {
            List<OrderItem> items = List.of(
                    OrderItem.of(null, "Item A", 2, PRICE),
                    OrderItem.of(null, "Item B", 1, PRICE)
            );
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = service.place("ORD-002", items);

            assertThat(result.getAmount().getValue()).isEqualByComparingTo(new BigDecimal("150.00"));
        }
    }

    @Nested
    class FindAll {

        private final Pageable pageable = Pageable.unpaged();

        @Test
        void withStatusFilter_shouldDelegateToFindAllByStatus() {
            Page<Order> expected = new PageImpl<>(List.of(pendingOrder()));
            when(orderRepository.findAllByStatus(OrderStatus.PENDING, pageable)).thenReturn(expected);

            Page<Order> result = service.findAll(OrderStatus.PENDING, pageable);

            assertThat(result).isSameAs(expected);
            verify(orderRepository).findAllByStatus(OrderStatus.PENDING, pageable);
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        void withoutStatusFilter_shouldDelegateToFindAll() {
            Page<Order> expected = Page.empty();
            when(orderRepository.findAll(pageable)).thenReturn(expected);

            Page<Order> result = service.findAll(null, pageable);

            assertThat(result).isSameAs(expected);
            verify(orderRepository).findAll(pageable);
            verifyNoMoreInteractions(orderRepository);
        }
    }

    @Nested
    class FindById {

        @Test
        void whenFound_shouldReturnOrder() {
            UUID id = UUID.randomUUID();
            Order order = pendingOrder();
            when(orderRepository.findById(id)).thenReturn(Optional.of(order));

            Order result = service.findById(id);

            assertThat(result).isSameAs(order);
        }

        @Test
        void whenNotFound_shouldThrowEntityNotFoundException() {
            UUID id = UUID.randomUUID();
            when(orderRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(id))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    @Nested
    class Cancel {

        @Test
        void onPendingOrder_shouldCancelAndSave() {
            UUID id = UUID.randomUUID();
            Order order = pendingOrder();
            when(orderRepository.findById(id)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            Order result = service.cancel(id);

            assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(order);
        }

        @Test
        void onPaidOrder_shouldThrowInvalidOrderStateException() {
            UUID id = UUID.randomUUID();
            Order order = pendingOrder();
            order.pay();
            when(orderRepository.findById(id)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancel(id))
                    .hasMessageContaining("Cannot cancel a paid order");
        }

        @Test
        void onAlreadyCancelledOrder_shouldThrowInvalidOrderStateException() {
            UUID id = UUID.randomUUID();
            Order order = pendingOrder();
            order.cancel();
            when(orderRepository.findById(id)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancel(id))
                    .hasMessageContaining("Order has already been cancelled");
        }
    }
}