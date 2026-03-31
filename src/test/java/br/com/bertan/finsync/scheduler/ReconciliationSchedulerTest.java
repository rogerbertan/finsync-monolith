package br.com.bertan.finsync.scheduler;

import br.com.bertan.finsync.service.ReconciliationApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationSchedulerTest {

    @Mock
    private ReconciliationApplicationService reconciliationApplicationService;

    @InjectMocks
    private ReconciliationScheduler scheduler;

    @Test
    void run_shouldDelegateToApplicationService() {
        scheduler.run();

        verify(reconciliationApplicationService).runReconciliation();
    }

    @Test
    void run_whenServiceThrows_shouldPropagateException() {
        doThrow(new RuntimeException("DB unavailable")).when(reconciliationApplicationService).runReconciliation();

        assertThatThrownBy(() -> scheduler.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB unavailable");
    }
}