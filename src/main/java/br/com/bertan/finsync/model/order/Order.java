package br.com.bertan.finsync.model.order;

import br.com.bertan.finsync.exception.InvalidOrderStateException;
import br.com.bertan.finsync.model.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String externalReference;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Order place(String externalReference, List<OrderItem> items) {
        if (externalReference == null || externalReference.isBlank())
            throw new IllegalArgumentException("External reference cannot be blank");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Order must have at least one item");

        Order order = new Order();
        order.externalReference = externalReference;
        order.status = OrderStatus.PENDING;

        for (OrderItem item : items) {
            item.setOrder(order);
            order.items.add(item);
        }

        order.amount = order.calculateTotal();
        return order;
    }

    public void pay() {
        if (status == OrderStatus.CANCELLED)
            throw new InvalidOrderStateException("Cannot pay a cancelled order");
        if (status == OrderStatus.PAID)
            throw new InvalidOrderStateException("Order has already been paid");
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status == OrderStatus.PAID)
            throw new InvalidOrderStateException("Cannot cancel a paid order");
        if (status == OrderStatus.CANCELLED)
            throw new InvalidOrderStateException("Order has already been cancelled");
        this.status = OrderStatus.CANCELLED;
    }

    private Money calculateTotal() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.ZERO, Money::add);
    }

    @PrePersist
    private void onPersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}