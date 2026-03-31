package br.com.bertan.finsync.model.reconciliation;

import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.payment.Payment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_results")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Getter
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = true)
    @JsonIgnoreProperties({"items"})
    private Order order;

    @ManyToOne
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private ReconciliationStatus status;

    private String divergenceReason;

    private LocalDateTime reconciledAt;

    public static ReconciliationResult matched(Order order, Payment payment) {
        if (order == null) throw new IllegalArgumentException("Order cannot be null for a MATCHED result");
        if (payment == null) throw new IllegalArgumentException("Payment cannot be null for a MATCHED result");

        ReconciliationResult result = new ReconciliationResult();
        result.order = order;
        result.payment = payment;
        result.status = ReconciliationStatus.MATCHED;
        return result;
    }

    public static ReconciliationResult divergent(Order order, Payment payment, String reason) {
        if (order == null) throw new IllegalArgumentException("Order cannot be null for a DIVERGED result");
        if (payment == null) throw new IllegalArgumentException("Payment cannot be null for a DIVERGED result");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Divergence reason cannot be blank");

        ReconciliationResult result = new ReconciliationResult();
        result.order = order;
        result.payment = payment;
        result.status = ReconciliationStatus.DIVERGED;
        result.divergenceReason = reason;
        return result;
    }

    public static ReconciliationResult suspiciousPayment(Payment payment) {
        if (payment == null) throw new IllegalArgumentException("Payment cannot be null for an UNMATCHED result");

        ReconciliationResult result = new ReconciliationResult();
        result.payment = payment;
        result.status = ReconciliationStatus.UNMATCHED;
        return result;
    }

    @PrePersist
    private void onPersist() {
        this.reconciledAt = LocalDateTime.now();
    }
}