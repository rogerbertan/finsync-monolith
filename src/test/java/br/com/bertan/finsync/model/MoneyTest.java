package br.com.bertan.finsync.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    void multiply_shouldReturnProductOfValueAndQuantity() {
        Money price = new Money(new BigDecimal("10.00"), "BRL");

        Money result = price.multiply(3);

        assertThat(result.getValue()).isEqualByComparingTo("30.00");
        assertThat(result.getCurrency()).isEqualTo("BRL");
    }

    @Test
    void multiply_byZero_shouldReturnZeroAmount() {
        Money price = new Money(new BigDecimal("50.00"), "BRL");

        Money result = price.multiply(0);

        assertThat(result.getValue()).isEqualByComparingTo("0.00");
    }

    @Test
    void multiply_shouldReturnNewInstance_preservingImmutability() {
        Money price = new Money(new BigDecimal("10.00"), "BRL");

        Money result = price.multiply(2);

        assertThat(result).isNotSameAs(price);
        assertThat(price.getValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void multiply_byNegativeQuantity_shouldThrow() {
        Money price = new Money(new BigDecimal("10.00"), "BRL");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> price.multiply(-1))
                .withMessage("Quantity cannot be negative");
    }

    @Test
    void add_shouldReturnSumOfBothAmounts() {
        Money a = new Money(new BigDecimal("10.50"), "BRL");
        Money b = new Money(new BigDecimal("4.50"), "BRL");

        Money result = a.add(b);

        assertThat(result.getValue()).isEqualByComparingTo("15.00");
        assertThat(result.getCurrency()).isEqualTo("BRL");
    }

    @Test
    void add_shouldReturnNewInstance_preservingImmutability() {
        Money a = new Money(new BigDecimal("10.00"), "BRL");
        Money b = new Money(new BigDecimal("5.00"), "BRL");

        Money result = a.add(b);

        assertThat(result).isNotSameAs(a);
        assertThat(a.getValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void add_withDifferentCurrencies_shouldThrow() {
        Money brl = new Money(new BigDecimal("10.00"), "BRL");
        Money usd = new Money(new BigDecimal("5.00"), "USD");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> brl.add(usd))
                .withMessage("Cannot add amounts with different currencies");
    }
}
