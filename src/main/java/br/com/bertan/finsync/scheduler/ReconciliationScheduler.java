package br.com.bertan.finsync.scheduler;

import br.com.bertan.finsync.service.ReconciliationApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationScheduler {

    private final ReconciliationApplicationService reconciliationApplicationService;

    @Scheduled(cron = "${finsync.reconciliation.cron}")
    public void run() {
        log.info("Starting scheduled reconciliation");
        reconciliationApplicationService.runReconciliation();
        log.info("Scheduled reconciliation finished");
    }
}