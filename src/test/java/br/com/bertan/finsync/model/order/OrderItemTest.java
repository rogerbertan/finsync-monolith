package br.com.bertan.finsync.model.order;

import br.com.bertan.finsync.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class OrderItemTest {

    private static final Money UNIT_PRICE = new Money(new BigDecimal("25.00"), "BRL");

    @Test
    void of_shouldCreateItemWithGivenFields() {
        OrderItem item = OrderItem.of(null, "Widget", 2, UNIT_PRICE);

        assertThat(item.getDescription()).isEqualTo("Widget");
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.getUnitPrice()).isEqualTo(UNIT_PRICE);
    }

    @Test
    void of_withNullDescription_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.of(null, null, 1, UNIT_PRICE))
                .withMessage("Item description cannot be blank");
    }

    @Test
    void of_withBlankDescription_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.of(null, "   ", 1, UNIT_PRICE))
                .withMessage("Item description cannot be blank");
    }

    @Test
    void of_withZeroQuantity_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.of(null, "Widget", 0, UNIT_PRICE))
                .withMessage("Item quantity must be greater than zero");
    }

    @Test
    void of_withNegativeQuantity_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.of(null, "Widget", -1, UNIT_PRICE))
                .withMessage("Item quantity must be greater than zero");
    }

    @Test
    void of_withNullQuantity_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.of(null, "Widget", null, UNIT_PRICE))
                .withMessage("Item quantity must be greater than zero");
    }

    @Test
    void of_withNullUnitPrice_shouldThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> OrderItem.of(null, "Widget", 1, null))
                .withMessage("Item unit price cannot be null");
    }

    @Test
    void subtotal_shouldReturnUnitPriceTimesQuantity() {
        OrderItem item = OrderItem.of(null, "Widget", 3, UNIT_PRICE);

        Money subtotal = item.subtotal();

        assertThat(subtotal.getValue()).isEqualByComparingTo("75.00");
        assertThat(subtotal.getCurrency()).isEqualTo("BRL");
    }

    @Test
    void subtotal_withQuantityOne_shouldEqualUnitPrice() {
        OrderItem item = OrderItem.of(null, "Widget", 1, UNIT_PRICE);

        assertThat(item.subtotal()).isEqualTo(UNIT_PRICE);
    }
}