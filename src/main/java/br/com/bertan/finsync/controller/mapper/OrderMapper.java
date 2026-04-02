package br.com.bertan.finsync.controller.mapper;

import br.com.bertan.finsync.controller.dto.OrderResponseDTO;
import br.com.bertan.finsync.controller.dto.OrderResponseDTO.OrderItemResponseDTO;
import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    public OrderResponseDTO toResponse(Order order) {
        List<OrderItemResponseDTO> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getExternalReference(),
                order.getAmount().getValue(),
                order.getAmount().getCurrency(),
                order.getStatus(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponseDTO toItemResponse(OrderItem item) {
        return new OrderItemResponseDTO(
                item.getId(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPrice().getValue(),
                item.getUnitPrice().getCurrency()
        );
    }
}