package br.com.bertan.finsync.controller;

import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse;
import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse.OrderSummary;
import br.com.bertan.finsync.controller.dto.ReconciliationResultResponse.PaymentSummary;
import br.com.bertan.finsync.controller.mapper.ReconciliationResultMapper;
import br.com.bertan.finsync.exception.InvalidOrderStateException;
import br.com.bertan.finsync.exception.InvalidPaymentStateException;
import br.com.bertan.finsync.exception.InvalidReconciliationStateException;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.model.payment.PaymentMethod;
import br.com.bertan.finsync.model.payment.PaymentStatus;
import br.com.bertan.finsync.model.reconciliation.ReconciliationResult;
import br.com.bertan.finsync.model.reconciliation.ReconciliationStatus;
import br.com.bertan.finsync.service.ReconciliationApplicationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReconciliationController.class)
class ReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReconciliationApplicationService reconciliationApplicationService;

    @MockitoBean
    private ReconciliationResultMapper mapper;

    @Nested
    class Trigger {

        @Test
        void shouldReturn200WithTriggeredAt() throws Exception {
            mockMvc.perform(post("/v1/reconciliations/trigger"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.triggeredAt").exists());
        }

        @Test
        void shouldDelegateToService() throws Exception {
            mockMvc.perform(post("/v1/reconciliations/trigger"))
                    .andExpect(status().isOk());

            verify(reconciliationApplicationService).runReconciliation();
        }

        @Test
        void whenServiceThrowsIllegalArgument_shouldReturn400() throws Exception {
            doThrow(new IllegalArgumentException("invalid input"))
                    .when(reconciliationApplicationService).runReconciliation();

            mockMvc.perform(post("/v1/reconciliations/trigger"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("invalid input"));
        }

        @Test
        void whenServiceThrowsInvalidReconciliationState_shouldReturn422() throws Exception {
            doThrow(new InvalidReconciliationStateException("Order must be in PENDING status"))
                    .when(reconciliationApplicationService).runReconciliation();

            mockMvc.perform(post("/v1/reconciliations/trigger"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Order must be in PENDING status"));
        }

        @Test
        void whenServiceThrowsInvalidOrderState_shouldReturn422() throws Exception {
            doThrow(new InvalidOrderStateException("Cannot pay a cancelled order"))
                    .when(reconciliationApplicationService).runReconciliation();

            mockMvc.perform(post("/v1/reconciliations/trigger"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Cannot pay a cancelled order"));
        }

        @Test
        void whenServiceThrowsInvalidPaymentState_shouldReturn422() throws Exception {
            doThrow(new InvalidPaymentStateException("Payment has already been processed"))
                    .when(reconciliationApplicationService).runReconciliation();

            mockMvc.perform(post("/v1/reconciliations/trigger"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Payment has already been processed"));
        }

        @Test
        void whenServiceThrowsUnexpectedException_shouldReturn500() throws Exception {
            doThrow(new RuntimeException("DB unavailable"))
                    .when(reconciliationApplicationService).runReconciliation();

            mockMvc.perform(post("/v1/reconciliations/trigger"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }
    }

    @Nested
    class Report {

        private ReconciliationResultResponse sampleResponse() {
            return new ReconciliationResultResponse(
                    UUID.randomUUID(),
                    ReconciliationStatus.MATCHED,
                    null,
                    LocalDateTime.of(2024, 6, 1, 12, 0),
                    new OrderSummary(UUID.randomUUID(), "ORD-001", new BigDecimal("100.00"), "BRL", OrderStatus.PAID),
                    new PaymentSummary(UUID.randomUUID(), "gw-001", "ORD-001", new BigDecimal("100.00"), "BRL", PaymentMethod.PIX, PaymentStatus.PROCESSED)
            );
        }

        @Test
        void withNoParams_shouldReturn200WithPage() throws Exception {
            ReconciliationResultResponse response = sampleResponse();
            when(reconciliationApplicationService.getReport(isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(mock(ReconciliationResult.class))));
            when(mapper.toResponse(any())).thenReturn(response);

            mockMvc.perform(get("/v1/reconciliations/report"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status").value("MATCHED"));
        }

        @Test
        void withStatusParam_shouldPassStatusToService() throws Exception {
            when(reconciliationApplicationService.getReport(isNull(), isNull(), eq(ReconciliationStatus.DIVERGED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/v1/reconciliations/report").param("status", "DIVERGED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));

            verify(reconciliationApplicationService).getReport(isNull(), isNull(), eq(ReconciliationStatus.DIVERGED), any(Pageable.class));
        }

        @Test
        void withDateRangeParams_shouldPassDatesToService() throws Exception {
            when(reconciliationApplicationService.getReport(any(), any(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/v1/reconciliations/report")
                            .param("from", "2024-01-01T00:00:00")
                            .param("to", "2024-12-31T23:59:59"))
                    .andExpect(status().isOk());

            verify(reconciliationApplicationService).getReport(
                    eq(LocalDateTime.of(2024, 1, 1, 0, 0, 0)),
                    eq(LocalDateTime.of(2024, 12, 31, 23, 59, 59)),
                    isNull(),
                    any(Pageable.class)
            );
        }

        @Test
        void withAllParams_shouldPassAllFiltersToService() throws Exception {
            ReconciliationResultResponse response = sampleResponse();
            when(reconciliationApplicationService.getReport(any(), any(), eq(ReconciliationStatus.MATCHED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(mock(ReconciliationResult.class))));
            when(mapper.toResponse(any())).thenReturn(response);

            mockMvc.perform(get("/v1/reconciliations/report")
                            .param("from", "2024-01-01T00:00:00")
                            .param("to", "2024-12-31T23:59:59")
                            .param("status", "MATCHED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].status").value("MATCHED"));
        }

        @Test
        void withInvalidStatusParam_shouldReturn500DueToUnhandledTypeMismatch() throws Exception {
            // MethodArgumentTypeMismatchException is caught by the catch-all Exception handler
            // since ExceptionHandlerExceptionResolver has higher priority than DefaultHandlerExceptionResolver
            mockMvc.perform(get("/v1/reconciliations/report").param("status", "INVALID_STATUS"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }

        @Test
        void withPaginationParams_shouldReturnPageMetadata() throws Exception {
            when(reconciliationApplicationService.getReport(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/v1/reconciliations/report")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(1));
            // totalPages = 1 because PageImpl uses: size == 0 ? 1 : ceil(total/size)
            // Unpaged PageImpl has size == 0, so totalPages is always 1 regardless of content
        }

        @Test
        void whenServiceThrowsUnexpectedException_shouldReturn500() throws Exception {
            when(reconciliationApplicationService.getReport(any(), any(), any(), any(Pageable.class)))
                    .thenThrow(new RuntimeException("unexpected"));

            mockMvc.perform(get("/v1/reconciliations/report"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }
    }
}