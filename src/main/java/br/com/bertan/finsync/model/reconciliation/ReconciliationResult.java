package br.com.bertan.finsync.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_results")
@NoArgsConstructor
@Getter
public class ReconciliationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Order order;

    @ManyToOne
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private ReconciliationStatus status;

    private String divergenceReason;

    private LocalDateTime reconciledAt;
}
