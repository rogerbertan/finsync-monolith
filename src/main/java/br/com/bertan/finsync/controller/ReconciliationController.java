package br.com.bertan.finsync.controller;

import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse;
import br.com.bertan.finsync.controller.dto.ReconciliationTriggerResponse;
import br.com.bertan.finsync.controller.mapper.ReconciliationResultMapper;
import br.com.bertan.finsync.model.reconciliation.ReconciliationStatus;
import br.com.bertan.finsync.service.ReconciliationApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationApplicationService reconciliationApplicationService;
    private final ReconciliationResultMapper mapper;

    @PostMapping("/trigger")
    public ResponseEntity<ReconciliationTriggerResponse> trigger() {
        reconciliationApplicationService.runReconciliation();
        return ResponseEntity.ok(new ReconciliationTriggerResponse(LocalDateTime.now()));
    }

    @GetMapping("/report")
    public ResponseEntity<Page<ReconciliationResultResponse>> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) ReconciliationStatus status,
            Pageable pageable) {

        Page<ReconciliationResultResponse> page = reconciliationApplicationService
                .getReport(from, to, status, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(page);
    }
}