package br.com.bertan.finsync.model.payment;

import br.com.bertan.finsync.exception.InvalidPaymentStateException;
import br.com.bertan.finsync.model.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String gatewayPaymentId;

    private String idempotencyKey;

    private String orderExternalReference;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;

    public static Payment receive(String gatewayPaymentId, String idempotencyKey, String orderExternalReference,
                                   Money amount, PaymentMethod method) {
        if (gatewayPaymentId == null || gatewayPaymentId.isBlank())
            throw new IllegalArgumentException("Gateway payment ID cannot be blank");
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("Idempotency key cannot be blank");
        if (orderExternalReference == null || orderExternalReference.isBlank())
            throw new IllegalArgumentException("Order external reference cannot be blank");
        if (amount == null)
            throw new IllegalArgumentException("Amount cannot be null");
        if (method == null)
            throw new IllegalArgumentException("Payment method cannot be null");

        Payment payment = new Payment();
        payment.gatewayPaymentId = gatewayPaymentId;
        payment.idempotencyKey = idempotencyKey;
        payment.orderExternalReference = orderExternalReference;
        payment.amount = amount;
        payment.method = method;
        payment.status = PaymentStatus.RECEIVED;
        return payment;
    }

    public void markProcessed() {
        if (status == PaymentStatus.PROCESSED)
            throw new InvalidPaymentStateException("Payment has already been processed");
        if (status == PaymentStatus.FAILED)
            throw new InvalidPaymentStateException("Cannot process a failed payment");
        this.status = PaymentStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed() {
        if (status == PaymentStatus.PROCESSED)
            throw new InvalidPaymentStateException("Cannot fail a payment that has already been processed");
        if (status == PaymentStatus.FAILED)
            throw new InvalidPaymentStateException("Payment has already been marked as failed");
        this.status = PaymentStatus.FAILED;
    }

    public boolean isEligibleForReconciliation() {
        return this.status == PaymentStatus.RECEIVED;
    }

    @PrePersist
    private void onPersist() {
        this.receivedAt = LocalDateTime.now();
    }
}