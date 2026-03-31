package br.com.bertan.finsync.model.order;

import br.com.bertan.finsync.model.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    private String description;

    private Integer quantity;

    @Embedded
    private Money unitPrice;

    static OrderItem of(Order order, String description, Integer quantity, Money unitPrice) {
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("Item description cannot be blank");
        if (quantity == null || quantity <= 0)
            throw new IllegalArgumentException("Item quantity must be greater than zero");
        if (unitPrice == null)
            throw new IllegalArgumentException("Item unit price cannot be null");

        OrderItem item = new OrderItem();
        item.order = order;
        item.description = description;
        item.quantity = quantity;
        item.unitPrice = unitPrice;
        return item;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}