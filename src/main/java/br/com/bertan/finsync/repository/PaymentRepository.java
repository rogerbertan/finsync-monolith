package br.com.bertan.finsync.repository;

import br.com.bertan.finsync.model.payment.Payment;
import br.com.bertan.finsync.model.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderExternalReferenceAndStatus(String orderExternalReference, PaymentStatus status);

    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.status = 'RECEIVED'
            AND NOT EXISTS (
                SELECT 1
                FROM Order o
                WHERE o.externalReference = p.orderExternalReference
            )
            """)
    List<Payment> findReceivedWithoutMatchingOrder();
}