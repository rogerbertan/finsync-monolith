package br.com.bertan.finsync.controller;

import br.com.bertan.finsync.controller.dto.OrderResponseDTO;
import br.com.bertan.finsync.controller.dto.OrderResponseDTO.OrderItemResponseDTO;
import br.com.bertan.finsync.controller.mapper.OrderMapper;
import br.com.bertan.finsync.exception.InvalidOrderStateException;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderMapper orderMapper;

    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String PLACE_BODY = """
            {
                "externalReference": "ORD-001",
                "items": [{ "description": "Product", "quantity": 2, "unitPrice": 50.00, "currency": "BRL" }]
            }
            """;

    private OrderResponseDTO sampleResponse() {
        return new OrderResponseDTO(
                ORDER_ID,
                "ORD-001",
                new BigDecimal("100.00"),
                "BRL",
                OrderStatus.PENDING,
                List.of(new OrderItemResponseDTO(UUID.randomUUID(), "Product", 2, new BigDecimal("50.00"), "BRL")),
                LocalDateTime.of(2024, 1, 1, 10, 0),
                LocalDateTime.of(2024, 1, 1, 10, 0)
        );
    }

    @Nested
    class Place {

        @Test
        void shouldReturn201WithBody() throws Exception {
            when(orderService.place(anyString(), anyList())).thenReturn(mock(Order.class));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleResponse());

            mockMvc.perform(post("/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PLACE_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.externalReference").value("ORD-001"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.amount").value(100.00))
                    .andExpect(jsonPath("$.items", hasSize(1)));
        }

        @Test
        void shouldDelegateToService() throws Exception {
            when(orderService.place(anyString(), anyList())).thenReturn(mock(Order.class));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleResponse());

            mockMvc.perform(post("/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PLACE_BODY))
                    .andExpect(status().isCreated());

            verify(orderService).place(eq("ORD-001"), anyList());
        }

        @Test
        void whenServiceThrowsIllegalArgument_shouldReturn400() throws Exception {
            when(orderService.place(anyString(), anyList()))
                    .thenThrow(new IllegalArgumentException("Order must have at least one item"));

            mockMvc.perform(post("/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PLACE_BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Order must have at least one item"));
        }

        @Test
        void whenServiceThrowsUnexpectedException_shouldReturn500() throws Exception {
            when(orderService.place(anyString(), anyList()))
                    .thenThrow(new RuntimeException("DB unavailable"));

            mockMvc.perform(post("/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PLACE_BODY))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }
    }

    @Nested
    class FindAll {

        @Test
        void withNoParams_shouldReturn200WithPage() throws Exception {
            when(orderService.findAll(isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(mock(Order.class))));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleResponse());

            mockMvc.perform(get("/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].externalReference").value("ORD-001"));
        }

        @Test
        void withStatusParam_shouldPassStatusToService() throws Exception {
            when(orderService.findAll(eq(OrderStatus.PENDING), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/v1/orders").param("status", "PENDING"))
                    .andExpect(status().isOk());

            verify(orderService).findAll(eq(OrderStatus.PENDING), any(Pageable.class));
        }

        @Test
        void withNoResults_shouldReturnEmptyPage() throws Exception {
            when(orderService.findAll(isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/v1/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void whenServiceThrowsUnexpectedException_shouldReturn500() throws Exception {
            when(orderService.findAll(any(), any(Pageable.class)))
                    .thenThrow(new RuntimeException("unexpected"));

            mockMvc.perform(get("/v1/orders"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturn200WithBody() throws Exception {
            when(orderService.findById(ORDER_ID)).thenReturn(mock(Order.class));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(sampleResponse());

            mockMvc.perform(get("/v1/orders/{id}", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                    .andExpect(jsonPath("$.externalReference").value("ORD-001"));
        }

        @Test
        void whenNotFound_shouldReturn404() throws Exception {
            when(orderService.findById(ORDER_ID))
                    .thenThrow(new EntityNotFoundException("Order not found: " + ORDER_ID));

            mockMvc.perform(get("/v1/orders/{id}", ORDER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Order not found: " + ORDER_ID));
        }

        @Test
        void whenServiceThrowsUnexpectedException_shouldReturn500() throws Exception {
            when(orderService.findById(ORDER_ID))
                    .thenThrow(new RuntimeException("unexpected"));

            mockMvc.perform(get("/v1/orders/{id}", ORDER_ID))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }
    }

    @Nested
    class Cancel {

        @Test
        void shouldReturn200WithCancelledOrder() throws Exception {
            OrderResponseDTO cancelledResponse = new OrderResponseDTO(
                    ORDER_ID, "ORD-001", new BigDecimal("100.00"), "BRL", OrderStatus.CANCELLED,
                    List.of(), LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 11, 0)
            );
            when(orderService.cancel(ORDER_ID)).thenReturn(mock(Order.class));
            when(orderMapper.toResponse(any(Order.class))).thenReturn(cancelledResponse);

            mockMvc.perform(patch("/v1/orders/{id}/cancel", ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        void whenOrderNotFound_shouldReturn404() throws Exception {
            when(orderService.cancel(ORDER_ID))
                    .thenThrow(new EntityNotFoundException("Order not found: " + ORDER_ID));

            mockMvc.perform(patch("/v1/orders/{id}/cancel", ORDER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("Order not found: " + ORDER_ID));
        }

        @Test
        void whenOrderAlreadyPaid_shouldReturn422() throws Exception {
            when(orderService.cancel(ORDER_ID))
                    .thenThrow(new InvalidOrderStateException("Cannot cancel a paid order"));

            mockMvc.perform(patch("/v1/orders/{id}/cancel", ORDER_ID))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.detail").value("Cannot cancel a paid order"));
        }

        @Test
        void whenServiceThrowsUnexpectedException_shouldReturn500() throws Exception {
            when(orderService.cancel(ORDER_ID))
                    .thenThrow(new RuntimeException("unexpected"));

            mockMvc.perform(patch("/v1/orders/{id}/cancel", ORDER_ID))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
        }
    }
}