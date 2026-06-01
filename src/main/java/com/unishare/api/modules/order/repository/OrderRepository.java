package com.unishare.api.modules.order.repository;

import com.unishare.api.modules.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByBuyerId(UUID buyerId);

    Page<Order> findByBuyerId(UUID buyerId, Pageable pageable);

    List<Order> findByBuyerIdAndStatusAndCreatedAtBefore(UUID buyerId, String status, Instant createdAtBefore);

    boolean existsByServiceId(UUID serviceId);

    @Query("""
            SELECT o FROM Order o
            WHERE o.serviceId IN (
                SELECT v.id FROM ServicePackageVersion v
                WHERE v.packageId IN (
                    SELECT p.id FROM ServicePackage p WHERE p.mentorId = :mentorId
                )
            )
            """)
    Page<Order> findIncomingForMentor(@Param("mentorId") UUID mentorId, Pageable pageable);
}
