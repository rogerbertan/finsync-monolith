package br.com.bertan.finsync.repository;

import br.com.bertan.finsync.model.order.Order;
import br.com.bertan.finsync.model.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByStatus(OrderStatus status);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);
}