package br.com.bertan.finsync.model.reconciliation;

import br.com.bertan.finsync.exception.InvalidReconciliationStateException;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.model.payment.Payment;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationService {

    public ReconciliationResult reconcile(Order order, Payment payment) {
        if (!payment.isEligibleForReconciliation())
            throw new InvalidReconciliationStateException("Payment is not eligible for reconciliation");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new InvalidReconciliationStateException("Order must be in PENDING status to be reconciled");

        boolean amountsMatch = order.getAmount().getValue().compareTo(payment.getAmount().getValue()) == 0
                && order.getAmount().getCurrency().equals(payment.getAmount().getCurrency());

        if (amountsMatch) {
            return ReconciliationResult.matched(order, payment);
        }

        String reason = "Expected %s %s but received %s %s".formatted(
                order.getAmount().getValue().toPlainString(),
                order.getAmount().getCurrency(),
                payment.getAmount().getValue().toPlainString(),
                payment.getAmount().getCurrency()
        );
        return ReconciliationResult.divergent(order, payment, reason);
    }
}