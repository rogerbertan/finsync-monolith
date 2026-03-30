package br.com.bertan.finsync.model.payment;

import br.com.bertan.finsync.model.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@NoArgsConstructor
@Getter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String gatewayPaymentId; // ID vindo do Stripe/gateway

    private String idempotencyKey;   // evita duplo processamento

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
}
