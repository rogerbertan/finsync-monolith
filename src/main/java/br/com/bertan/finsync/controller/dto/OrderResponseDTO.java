package br.com.bertan.finsync.controller.dto;

import br.com.bertan.finsync.model.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID id,
        String externalReference,
        BigDecimal amount,
        String currency,
        OrderStatus status,
        List<OrderItemResponseDTO> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record OrderItemResponseDTO(
            UUID id,
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            String currency
    ) {}
}
