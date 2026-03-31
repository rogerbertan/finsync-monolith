package br.com.bertan.finsync.repository;

import br.com.bertan.finsync.model.reconciliation.ReconciliationResult;
import br.com.bertan.finsync.model.reconciliation.ReconciliationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, UUID> {

    Page<ReconciliationResult> findAllByStatus(ReconciliationStatus status, Pageable pageable);

    Page<ReconciliationResult> findAllByReconciledAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<ReconciliationResult> findAllByStatusAndReconciledAtBetween(
            ReconciliationStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable);
}