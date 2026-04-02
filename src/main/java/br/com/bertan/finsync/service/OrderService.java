package br.com.bertan.finsync.service;

import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderItem;
import br.com.bertan.finsync.model.order.OrderStatus;
import br.com.bertan.finsync.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order place(String externalReference, List<OrderItem> items) {
        Order order = Order.place(externalReference, items);
        return orderRepository.save(order);
    }

    public Page<Order> findAll(OrderStatus status, Pageable pageable) {
        if (status != null)
            return orderRepository.findAllByStatus(status, pageable);
        return orderRepository.findAll(pageable);
    }

    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
    }

    @Transactional
    public Order cancel(UUID id) {
        Order order = findById(id);
        order.cancel();
        return orderRepository.save(order);
    }
}