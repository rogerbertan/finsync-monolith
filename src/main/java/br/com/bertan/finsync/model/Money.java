package br.com.bertan.finsync.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
@Getter
public class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO, "BRL");

    @Column(name = "amount")
    private BigDecimal value;

    @Column(name = "currency")
    private String currency;

    public Money(BigDecimal value, String currency) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor monetário não pode ser negativo");
        }
        this.value = value.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public Money multiply(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        return new Money(this.value.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency))
            throw new IllegalArgumentException("Cannot add amounts with different currencies");
        return new Money(this.value.add(other.value), this.currency);
    }
}
