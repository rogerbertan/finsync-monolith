package br.com.bertan.finsync.service;

import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.model.payment.PaymentStatus;
import br.com.bertan.finsync.model.reconciliation.ReconciliationResult;
import br.com.bertan.finsync.model.reconciliation.ReconciliationService;
import br.com.bertan.finsync.model.reconciliation.ReconciliationStatus;
import br.com.bertan.finsync.repository.OrderRepository;
import br.com.bertan.finsync.repository.PaymentRepository;
import br.com.bertan.finsync.repository.ReconciliationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReconciliationApplicationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final ReconciliationService reconciliationService;

    @Transactional
    public void runReconciliation() {
        List<Order> pendingOrders = orderRepository.findAllByStatus(OrderStatus.PENDING);

        for (Order order : pendingOrders) {
            paymentRepository
                    .findByOrderExternalReferenceAndStatus(order.getExternalReference(), PaymentStatus.RECEIVED)
                    .ifPresent(payment -> {
                        ReconciliationResult result = reconciliationService.reconcile(order, payment);
                        if (result.getStatus() == ReconciliationStatus.MATCHED) {
                            order.pay();
                            payment.markProcessed();
                        }
                        reconciliationResultRepository.save(result);
                    });
        }

        paymentRepository.findReceivedWithoutMatchingOrder().forEach(payment -> {
            ReconciliationResult result = ReconciliationResult.suspiciousPayment(payment);
            reconciliationResultRepository.save(result);
        });
    }

    public Page<ReconciliationResult> getReport(
            LocalDateTime from, LocalDateTime to, ReconciliationStatus status, Pageable pageable) {

        if (status != null && from != null && to != null)
            return reconciliationResultRepository.findAllByStatusAndReconciledAtBetween(status, from, to, pageable);
        if (status != null)
            return reconciliationResultRepository.findAllByStatus(status, pageable);
        if (from != null && to != null)
            return reconciliationResultRepository.findAllByReconciledAtBetween(from, to, pageable);

        return reconciliationResultRepository.findAll(pageable);
    }
}