package com.unishare.api.modules.order.repository;

import com.unishare.api.modules.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByBuyerId(UUID buyerId);

    Page<Order> findByBuyerId(UUID buyerId, Pageable pageable);

    boolean existsByServiceId(UUID serviceId);
}
