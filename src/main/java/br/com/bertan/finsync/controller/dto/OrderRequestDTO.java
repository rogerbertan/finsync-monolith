package br.com.bertan.finsync.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequestDTO(
        String externalReference,
        List<OrderItemRequestDTO> items
) {
    public record OrderItemRequestDTO(
            String description,
            Integer quantity,
            BigDecimal unitPrice,
            String currency
    ) {}
}
