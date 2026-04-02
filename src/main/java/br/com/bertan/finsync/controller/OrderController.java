package br.com.bertan.finsync.controller;

import br.com.bertan.finsync.controller.dto.OrderRequestDTO;
import br.com.bertan.finsync.controller.dto.OrderResponseDTO;
import br.com.bertan.finsync.controller.mapper.OrderMapper;
import br.com.bertan.finsync.model.Money;
import br.com.bertan.finsync.model.order.OrderItem;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> place(@RequestBody OrderRequestDTO request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> OrderItem.of(null, i.description(), i.quantity(), new Money(i.unitPrice(), i.currency())))
                .toList();

        return ResponseEntity.status(201).body(orderMapper.toResponse(orderService.place(request.externalReference(), items)));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> findAll(
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.findAll(status, pageable).map(orderMapper::toResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderMapper.toResponse(orderService.findById(id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDTO> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(orderMapper.toResponse(orderService.cancel(id)));
    }
}